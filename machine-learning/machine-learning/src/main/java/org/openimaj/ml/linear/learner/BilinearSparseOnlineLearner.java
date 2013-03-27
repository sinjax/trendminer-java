package org.openimaj.ml.linear.learner;

import gov.sandia.cognition.math.matrix.Matrix;
import gov.sandia.cognition.math.matrix.mtj.SparseMatrix;
import gov.sandia.cognition.math.matrix.mtj.SparseMatrixFactoryMTJ;

import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.openimaj.math.matrix.SandiaMatrixUtils;
import org.openimaj.ml.linear.learner.init.InitStrategy;
import org.openimaj.ml.linear.learner.init.SparseRandomInitStrategy;
import org.openimaj.ml.linear.learner.init.ZerosInitStrategy;
import org.openimaj.ml.linear.learner.loss.LossFunction;
import org.openimaj.ml.linear.learner.loss.MatLossFunction;
import org.openimaj.ml.linear.learner.loss.SquareMissingLossFunction;
import org.openimaj.ml.linear.learner.regul.L1L2Regulariser;
import org.openimaj.ml.linear.learner.regul.Regulariser;
import org.openimaj.util.pair.Pair;


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
 * 			- flush the batch
 * 		- return current U and W (same as last time is batch isn't filled yet)
 * 
 * 
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class BilinearSparseOnlineLearner {
	
	static Logger logger = Logger.getLogger(BilinearSparseOnlineLearner.class);
	
	public static class BilinearLearnerParameters extends LearningParameters{
		
		/**
		 * whether a bias component is added to w and u. Default is false.
		 */
		public static final String BIAS = "bias";
		/**
		 * Defaults to -1 (i.e. no seed)
		 */
		public static final String SEED = "seed";
		/**
		 * Defaults to a {@link SparseRandomInitStrategy} with sparcity set to 0.5
		 */
		public static final String WINITSTRAT = "winitstrat";
		/**
		 * Defaults to a {@link SparseRandomInitStrategy} with sparcity set to 0.5
		 */
		public static final String UINITSTRAT = "uinitstrat";
		/**
		 * Defaults to a {@link ZerosInitStrategy}
		 */
		public static final String BIASINITSTRAT = "biasinitstrat";
		/**
		 * Defaults to 1 (currently this is ignored)
		 */
		public static final String BATCHSIZE = "batchsize";
		/**
		 * Whether different tasks have independant u values, defualts to true
		 */
		public static final String INDU = "indu";
		/**
		 * Whether different tasks have independant w values, defualts to true
		 */
		public static final String INDW = "indw";
		/**
		 * The maximum number of iterations per batch, defaults to 3
		 */
		public static final String BICONVEX_MAXITER = "biconvex_maxiter";
		/**
		 * The threshold of the ratio between the (sum(new_w - old_w) + sum(new_u - old_u)) / (sum(old_u) + sum(old_w))
		 * i.e. some notion of normalised changed of the paramters. Defaults to 0.01
		 */
		public static final String BICONVEX_TOL = "biconvex_tol";
		/**
		 * The parameter of the regulariser, defaults to 0.001
		 */
		public static final String LAMBDA = "lambda";
		/**
		 * The weighting of the subgradient, weighted down each iteration of the biconvex scheme, defaults ot 0.05
		 */
		public static final String ETA0 = "eta0";
		/**
		 * The loss function, defaults to {@link SquareMissingLossFunction}
		 */
		public static final String LOSS = "loss";
		/**
		 * The regularisation function, defaults to {@link L1L2Regulariser}
		 */
		public static final String REGUL = "regul";
		
		/**
		 * 
		 */
		private static final long serialVersionUID = -2059819246888686435L;
		public BilinearLearnerParameters() {
			this.defaults.put(REGUL, new L1L2Regulariser());
			this.defaults.put(LOSS, new SquareMissingLossFunction());
			this.defaults.put(ETA0, 0.05);
			this.defaults.put(LAMBDA, 0.001);
			this.defaults.put(BICONVEX_TOL, 0.01);
			this.defaults.put(BICONVEX_MAXITER, 3);
			this.defaults.put(INDW, true);
			this.defaults.put(INDU, true);
			this.defaults.put(SEED, -1);
			this.defaults.put(WINITSTRAT, new SparseRandomInitStrategy(0,1,0.5,new Random()));
			this.defaults.put(UINITSTRAT, new SparseRandomInitStrategy(0,1,0.5,new Random()));
			this.defaults.put(BATCHSIZE, 1); // Currently ignored
			this.defaults.put(BIAS, false);
			this.defaults.put(BIASINITSTRAT, new ZerosInitStrategy());
		}
		
		
	}
	private BilinearLearnerParameters params;
	private Matrix w;
	private Matrix u;
	private boolean indu;
	private boolean indw;
	private SparseMatrixFactoryMTJ smf = SparseMatrixFactoryMTJ.INSTANCE;
	private LossFunction loss;
	private Regulariser regul;
	private double lambda;
	private boolean biasMode;
	private Matrix bias;
	
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
				iter += 1;
				// If we're in bias mode, reset the bias
				if(this.biasMode) loss.setBias(this.bias);
				
				// Vprime is nwords x tasks
				Matrix Vprime = X.transpose().times(this.w);
				loss.setX(Vprime.transpose());
				Matrix gradU = loss.gradient(this.u);
				Matrix newu = this.u.minus(SandiaMatrixUtils.timesInplace(gradU,etat(iter)));
				newu = regul.prox(newu, lambda);
				
				// Dprime is tasks x nusers
				Matrix Dprime = newu.transpose().times(X.transpose());
				loss.setX(Dprime);
				Matrix gradW = loss.gradient(this.w);
				Matrix neww = this.w.minus(SandiaMatrixUtils.timesInplace(gradW,etat(iter)));
				neww = regul.prox(neww, lambda);
				
				if(this.biasMode){
					// Calculate gradient of bias (don't regularise)
					this.u.transpose().times(X).times(this.w).plus(this.bias);
				}
				
				double sumchangew = SandiaMatrixUtils.absSum(neww.minus(this.w));
				double totalw = SandiaMatrixUtils.absSum(this.w);
				
				double sumchangeu = SandiaMatrixUtils.absSum(newu.minus(this.u));
				double totalu = SandiaMatrixUtils.absSum(this.u);
				
				this.w = neww;
				this.u = newu;
				double ratio = ((sumchangeu/totalu) + (sumchangew/totalw)) / 2;
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
	private static SparseMatrix expandY(Matrix Y) {
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
	private double etat(int iter) {
		return eta() / Math.sqrt(iter);
	}
	private double eta() {
		double eta0 = this.params.getTyped(BilinearLearnerParameters.ETA0);
		int batchsize = this.params.getTyped(BilinearLearnerParameters.BATCHSIZE);
		return eta0 / batchsize;
	}
	public double sumLoss(List<Pair<Matrix>> pairs) {
		return BilinearSparseOnlineLearner.sumLoss(pairs,this.u,this.w,this.params);
	}
	public static double sumLoss(List<Pair<Matrix>> pairs, Matrix u, Matrix w, BilinearLearnerParameters params) {
		boolean indw = params.getTyped("indw");
		boolean indu = params.getTyped("indu");
		double total = 0;
		if(indw && indu){
			int i = 0;
			for (Pair<Matrix> pair : pairs) {
				Matrix X = pair.firstObject();
				Matrix Y = pair.secondObject();
				int ntasks = Y.getNumColumns();
				SparseMatrix Yexp = expandY(Y);
				Matrix expectedAll = u.transpose().times(X.transpose()).times(w);
				for (int t = 0; t < ntasks; t++) {
					double Y_t = Yexp.getElement(t, t);
					double expected = expectedAll.getElement(t, t);
					logger.debug(
						String.format(
							"p=%d,t=%d,y_t= %2.3f,u_t.x.w=%2.3f", 
							i,t,Y_t,expected
						)
					);
					
					double delta = Y_t - expected;
					
					total += (delta * delta);
				}
				i++;
			}
		}
		
		return total;
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
}
