package org.openimaj.ml.linear.learner;

import java.util.List;
import java.util.Random;

import gov.sandia.cognition.math.matrix.Matrix;
import gov.sandia.cognition.math.matrix.mtj.SparseMatrix;
import gov.sandia.cognition.math.matrix.mtj.SparseMatrixFactoryMTJ;

import org.apache.log4j.Logger;
import org.openimaj.math.matrix.SandiaMatrixUtils;
import org.openimaj.ml.linear.learner.init.InitStrategy;
import org.openimaj.ml.linear.learner.init.SparseRandomInitStrategy;
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
	
	Logger logger = Logger.getLogger(BilinearSparseOnlineLearner.class);
	
	public static class BilinearLearnerParameters extends LearningParameters{

		/**
		 * 
		 */
		private static final long serialVersionUID = -2059819246888686435L;
		public BilinearLearnerParameters() {
			this.defaults.put("regul", new L1L2Regulariser());
			this.defaults.put("loss", new SquareMissingLossFunction());
			this.defaults.put("eta0", 0.05);
			this.defaults.put("lambda", 0.001);
			this.defaults.put("biconvex_tol", 0.01);
			this.defaults.put("biconvex_maxiter", 3);
			this.defaults.put("indw", true);
			this.defaults.put("indu", true);
			this.defaults.put("seed", -1);
			this.defaults.put("winitstrat", new SparseRandomInitStrategy(0,1,new Random()));
			this.defaults.put("uinitstrat", new SparseRandomInitStrategy(0,1,new Random()));
			this.defaults.put("batchsize", 1); // Currently ignored
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
	public BilinearSparseOnlineLearner() {
		this(new BilinearLearnerParameters());
	}
	public BilinearSparseOnlineLearner(BilinearLearnerParameters params) {
		this.params = params;
		this.indw = this.params.getTyped("indw");
		this.indu = this.params.getTyped("indu");
		this.loss = this.params.getTyped("loss");
		this.regul = this.params.getTyped("regul");
		this.lambda = this.params.getTyped("lambda");
		
		if(indw && indu){
			this.loss = new MatLossFunction(this.loss);
		}
	}
	
	private void initUW(int xrows, int xcols, int ycols) {
		InitStrategy wstrat = this.params.getTyped("winitstrat");
		InitStrategy ustrat = this.params.getTyped("uinitstrat");
		if(indw) this.w = wstrat.init(xrows, ycols);
		else this.w = wstrat.init(xrows, 1);
		if(indu) this.u = ustrat.init(xcols, ycols);
		else this.u = ustrat.init(xcols, 1);
	}
	
	public void process(Matrix X, Matrix Y){
		int nusers = X.getNumRows();
		int nfeatures = X.getNumColumns();
		int ntasks = Y.getNumColumns();
//		int ninstances = Y.getNumRows(); // Assume 1 instance!
		if (this.w == null){
			initUW(nusers, nfeatures, ntasks); // Number of words, users and tasks
		}
		
		if(indw && indu){ // Both u and w have a column per task
			// First expand Y s.t. blocks of rows contain the task values for each row of Y. 
			// This means Yexp has (n * t x t)
			SparseMatrix Yexp = smf.createMatrix(ntasks, ntasks);
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
			loss.setY(Yexp);
			int iter = 0;
			while(true) {
				iter += 1;
				Matrix Dprime = this.u.transpose().times(X.transpose());
				Matrix Vprime = X.transpose().times(this.w);
				
				loss.setX(Dprime);
				Matrix gradW = loss.gradient(this.w);
				
				loss.setX(Vprime.transpose());
				Matrix gradU = loss.gradient(this.u);
				
				Matrix neww = this.w.minus(SandiaMatrixUtils.timesInplace(gradW,etat(iter)));
				Matrix newu = this.u.minus(SandiaMatrixUtils.timesInplace(gradU,etat(iter)));
				
				newu = regul.prox(newu, lambda);
				neww = regul.prox(neww, lambda);
				
				double sumchange = 
						SandiaMatrixUtils.absSum(neww.minus(this.w)) + 
						SandiaMatrixUtils.absSum(newu.minus(this.u));
				double total = 
						SandiaMatrixUtils.absSum(this.w) + 
						SandiaMatrixUtils.absSum(this.u);
				
				this.w = neww;
				this.u = newu;
				double ratio = sumchange/total;
				double biconvextol = this.params.getTyped("biconvex_tol");
				int maxiter = this.params.getTyped("biconvex_maxiter");
				if(biconvextol  < 0 || ratio < biconvextol || iter >= maxiter) {
					logger.debug("tolerance reached after iteration: " + iter);
					break;
				}
			}
		}
	}
	private double etat(int iter) {
		return eta() / Math.sqrt(iter);
	}
	private double eta() {
		double eta0 = this.params.getTyped("eta0");
		int batchsize = this.params.getTyped("batchsize");
		return eta0 / batchsize;
	}
	public double sumLoss(List<Pair<Matrix>> pairs) {
		return BilinearSparseOnlineLearner.sumLoss(pairs,this.u,this.w,this.params);
	}
	public static double sumLoss(List<Pair<Matrix>> pairs, Matrix u, Matrix w, BilinearLearnerParameters params) {
		boolean indw = params.getTyped("indw");
		boolean indu = params.getTyped("indu");
		LossFunction loss = params.getTyped("loss");
		double total = 0;
		if(indw && indu){
			for (Pair<Matrix> pair : pairs) {
				Matrix X = pair.firstObject();
				Matrix Y = pair.secondObject();
				int ntasks = Y.getNumColumns();
				int nusers = X.getNumColumns();
				int nwords = X.getNumRows();
				for (int t = 0; t < ntasks; t++) {
					Matrix Y_t = Y.getSubMatrix(0, 0, t, t);
					Matrix u_t = u.getSubMatrix(0, nusers-1, t, t);
					Matrix w_t = w.getSubMatrix(0, nwords-1, t, t);
					Matrix delta = Y_t.minus(u_t.transpose().times(X.transpose()).times(w_t));
					total += delta.dotTimes(delta).sumOfColumns().sum();
				}
			}
		}
		
		return total;
	}
	public BilinearLearnerParameters getParams() {
		return this.params;
	}
}
