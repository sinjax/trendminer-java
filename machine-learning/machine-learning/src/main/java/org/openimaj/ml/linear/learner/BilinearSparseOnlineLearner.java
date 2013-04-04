package org.openimaj.ml.linear.learner;

import gov.sandia.cognition.math.matrix.Matrix;
import gov.sandia.cognition.math.matrix.mtj.SparseMatrix;
import gov.sandia.cognition.math.matrix.mtj.SparseMatrixFactoryMTJ;

import org.apache.log4j.Logger;
import org.openimaj.math.matrix.SandiaMatrixUtils;
import org.openimaj.ml.linear.learner.init.InitStrategy;
import org.openimaj.ml.linear.learner.loss.LossFunction;
import org.openimaj.ml.linear.learner.loss.MatLossFunction;
import org.openimaj.ml.linear.learner.regul.Regulariser;


/**
 * An implementation of a stochastic gradient decent with proximal perameter adjustment
 * (for regularised parameters).
 * 
 * Data is dealt with sequentially using a one pass implementation of the 
 * online proximal algorithm described in chapter 9 and 10 of:
 * The Geometry of Constrained Structured Prediction: Applications to Inference and
 * Learning of Natural Language Syntax, PhD, Andre T. Martins
 * 
 * The implementation does the following:
 * 	- When an X,Y is recieved:
 * 		- Update currently held batch
 * 		- If the batch is full:
 * 			- While There is a great deal of change in U and W:
 * 				- Calculate the gradient of W holding U fixed
 * 				- Proximal update of W
 * 				- Calculate the gradient of U holding W fixed
 * 				- Proximal update of U
 * 				- Calculate the gradient of Bias holding U and W fixed
 * 			- flush the batch
 * 		- return current U and W (same as last time is batch isn't filled yet)
 * 
 * 
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class BilinearSparseOnlineLearner implements OnlineLearner<Matrix,Matrix>{
	
	static Logger logger = Logger.getLogger(BilinearSparseOnlineLearner.class);
	
	private BilinearLearnerParameters params;
	private Matrix w;
	private Matrix u;
	private Boolean indu;
	private Boolean indw;
	private SparseMatrixFactoryMTJ smf = SparseMatrixFactoryMTJ.INSTANCE;
	private LossFunction loss;
	private Regulariser regul;
	private Double lambda;
	private Boolean biasMode;
	private Matrix bias;
	private Matrix diagX;
	private Boolean weightByDim;
	private Double eta0_u;
	private Double eta0_w;
	
	public BilinearSparseOnlineLearner() {
		this(new BilinearLearnerParameters());
	}
	public BilinearSparseOnlineLearner(BilinearLearnerParameters params) {
		this.params = params;
		reinitParams();
	}
	
	public void reinitParams() {
		this.indw = this.params.getTyped(BilinearLearnerParameters.INDW);
		this.indu = this.params.getTyped(BilinearLearnerParameters.INDU);
		this.loss = this.params.getTyped(BilinearLearnerParameters.LOSS);
		this.regul = this.params.getTyped(BilinearLearnerParameters.REGUL);
		this.lambda = this.params.getTyped(BilinearLearnerParameters.LAMBDA);
		this.biasMode = this.params.getTyped(BilinearLearnerParameters.BIAS);
		this.weightByDim = this.params.getTyped(BilinearLearnerParameters.DIMWEIGHTED);
		this.eta0_u = this.params.getTyped(BilinearLearnerParameters.ETA0_U);
		this.eta0_w = this.params.getTyped(BilinearLearnerParameters.ETA0_W);
		
		if(indw && indu){
			this.loss = new MatLossFunction(this.loss);
		}
	}
	private void initParams(int xrows, int xcols, int ycols) {
		InitStrategy wstrat = this.params.getTyped(BilinearLearnerParameters.WINITSTRAT);
		InitStrategy ustrat = this.params.getTyped(BilinearLearnerParameters.UINITSTRAT);
		
		if(indw) this.w = wstrat.init(xrows, ycols);
		else this.w = wstrat.init(xrows, 1);
		if(indu) this.u = ustrat.init(xcols, ycols);
		else this.u = ustrat.init(xcols, 1);
		
		this.bias = smf.createMatrix(ycols,ycols);
		if(this.biasMode){			
			InitStrategy bstrat = this.params.getTyped(BilinearLearnerParameters.BIASINITSTRAT);
			this.bias = bstrat.init(ycols, ycols);
			this.diagX = smf.createIdentity(ycols, ycols);
		}
	}
	
	public void process(Matrix X, Matrix Y){
		int nfeatures = X.getNumRows();
		int nusers = X.getNumColumns();
		int ntasks = Y.getNumColumns();
//		int ninstances = Y.getNumRows(); // Assume 1 instance!

		// only inits when the current params is null
		if (this.w == null){
			initParams(nfeatures, nusers, ntasks); // Number of words, users and tasks	
		}
		
		if(indw && indu){ // Both u and w have a column per task
			// First expand Y s.t. blocks of rows contain the task values for each row of Y. 
			// This means Yexp has (n * t x t)
			SparseMatrix Yexp = expandY(Y);
			loss.setY(Yexp);
			int iter = 0;
			while(true) {
				// We need to set the bias here because it is used in the loss calculation of U and W
				if(this.biasMode) loss.setBias(this.bias);
				iter += 1;
				
				// Vprime is nusers x tasks
				Matrix Vprime = X.transpose().times(this.w);
				// ... so the loss function's X is (tasks x nusers)
				loss.setX(Vprime.transpose());
				Matrix gradU = loss.gradient(this.u);
				if(weightByDim){
					SandiaMatrixUtils.timesInplace(gradU,dimWeightedetat(iter,nfeatures,eta0_u));
				}
				else{					
					SandiaMatrixUtils.timesInplace(gradU,etat(iter,eta0_u));
				}
				Matrix newu = this.u.minus(gradU);
				newu = regul.prox(newu, lambdat(iter,lambda));
				
				// Dprime is tasks x nwords
				Matrix Dprime = newu.transpose().times(X.transpose());
				// ... as is the cost function's X
				loss.setX(Dprime);
				Matrix gradW = loss.gradient(this.w);
				if(weightByDim){
					SandiaMatrixUtils.timesInplace(gradW,dimWeightedetat(iter,nfeatures,eta0_w));
				}
				else{					
					SandiaMatrixUtils.timesInplace(gradW,etat(iter,eta0_w));
				}
				
				Matrix neww = this.w.minus(gradW);
				neww = regul.prox(neww, lambdat(iter,lambda));
				
				
				
				double sumchangew = SandiaMatrixUtils.absSum(neww.minus(this.w));
				double totalw = SandiaMatrixUtils.absSum(this.w);
				
				double sumchangeu = SandiaMatrixUtils.absSum(newu.minus(this.u));
				double totalu = SandiaMatrixUtils.absSum(this.u);
				double ratio = ((sumchangeu/totalu) + (sumchangew/totalw)) / 2;
				
				if(this.biasMode){
					Matrix mult = newu.transpose().times(X.transpose()).times(neww).plus(this.bias);
					// We must set bias to null! 
					loss.setBias(null);
					loss.setX(diagX);
					// Calculate gradient of bias (don't regularise)
					Matrix biasGrad = loss.gradient(mult);
					Matrix newbias = this.bias.minus(
							SandiaMatrixUtils.timesInplace(
									biasGrad,
									biasEtat(iter)
							)
					);
					
					double sumchangebias = SandiaMatrixUtils.absSum(newbias.minus(this.bias));
					double totalbias = SandiaMatrixUtils.absSum(this.bias);
					
					ratio = ((ratio * 2) + (sumchangebias/totalbias) )/ 3;
					this.bias = newbias;
					
				}
				
				
				this.w = neww;
				this.u = newu;
				
				double biconvextol = this.params.getTyped("biconvex_tol");
				int maxiter = this.params.getTyped("biconvex_maxiter");
				if(iter%3 == 0){
					logger.debug(String.format("Iter: %d. Last Ratio: %2.3f",iter,ratio));
				}
				if(biconvextol  < 0 || ratio < biconvextol || iter >= maxiter) {
					logger.debug("tolerance reached after iteration: " + iter);
					break;
				}
			}
		}
	}
	private double lambdat(int iter, double lambda) {
		return lambda/iter;
	}
	public static SparseMatrix expandY(Matrix Y) {
		int ntasks = Y.getNumColumns();
		SparseMatrix Yexp = SparseMatrixFactoryMTJ.INSTANCE.createMatrix(ntasks, ntasks);
		for (int touter = 0; touter < ntasks; touter++) {
			for (int tinner = 0; tinner < ntasks; tinner++) {
				if(tinner == touter){
					Yexp.setElement(touter, tinner, Y.getElement(0, tinner));
				}
				else{
					Yexp.setElement(touter, tinner, Double.NaN);
				}
			}
		}
		return Yexp;
	}
	private double biasEtat(int iter){
		double biasEta0 = this.params.getTyped(BilinearLearnerParameters.BIASETA0);
		return biasEta0 / Math.sqrt(iter);
	}
	
	private double dimWeightedetat(int iter, int ndims,double eta0) {
		int etaSteps = this.params.getTyped(BilinearLearnerParameters.ETASTEPS);
		double sqrtCeil = Math.sqrt(Math.ceil(iter/(double)etaSteps));
		return (eta(eta0) / sqrtCeil) / ndims;
	}
	
	private double etat(int iter,double eta0) {
		int etaSteps = this.params.getTyped(BilinearLearnerParameters.ETASTEPS);
		double sqrtCeil = Math.sqrt(Math.ceil(iter/(double)etaSteps));
		return eta(eta0) / sqrtCeil;
	}
	private double eta(double eta0) {
		int batchsize = this.params.getTyped(BilinearLearnerParameters.BATCHSIZE);
		return eta0 / batchsize;
	}
	
	
	
	public BilinearLearnerParameters getParams() {
		return this.params;
	}
	
	public Matrix getU(){
		return this.u;
	}
	
	public Matrix getW(){
		return this.w;
	}
	public Matrix getBias() {
		if(this.biasMode)
			return this.bias;
		else
			return null;
	}
	public void addU(int newUsers) {
		if(this.u == null) return; // If u has not be inited, then it will be on first process
		InitStrategy ustrat = this.params.getTyped(BilinearLearnerParameters.UINITSTRAT);
		Matrix newU = ustrat.init(newUsers, this.u.getNumColumns());
		this.u = SandiaMatrixUtils.vstack(this.u,newU);
	}
	
	public void addW(int newWords) {
		if(this.w == null) return; // If w has not be inited, then it will be on first process
		InitStrategy wstrat = this.params.getTyped(BilinearLearnerParameters.WINITSTRAT);
		Matrix newW = wstrat.init(newWords, this.w.getNumColumns());
		this.w = SandiaMatrixUtils.vstack(this.w,newW);
	}
}
