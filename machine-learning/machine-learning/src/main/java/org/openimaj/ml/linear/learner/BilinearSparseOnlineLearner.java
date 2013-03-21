package org.openimaj.ml.linear.learner;

import gov.sandia.cognition.math.matrix.Matrix;
import gov.sandia.cognition.math.matrix.mtj.SparseMatrix;
import gov.sandia.cognition.math.matrix.mtj.SparseMatrixFactoryMTJ;

import org.openimaj.math.matrix.MatrixUtils;
import org.openimaj.ml.linear.learner.init.InitStrategy;
import org.openimaj.ml.linear.learner.init.SparseRandomInitStrategy;
import org.openimaj.ml.linear.learner.loss.LossFunction;
import org.openimaj.ml.linear.learner.loss.SquareMissingLossFunction;
import org.openimaj.ml.linear.learner.regul.L1L2Regulariser;
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
 * 			- flush the batch
 * 		- return current U and W (same as last time is batch isn't filled yet)
 * 
 * 
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class BilinearSparseOnlineLearner {
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
			this.defaults.put("indw", true);
			this.defaults.put("indu", true);
			this.defaults.put("seed", -1);
			this.defaults.put("initstrat", new SparseRandomInitStrategy());
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
	public BilinearSparseOnlineLearner() {
		this(new BilinearLearnerParameters());
	}
	public BilinearSparseOnlineLearner(BilinearLearnerParameters params) {
		this.params = params;
		this.indw = this.params.getTyped("indw");
		this.indu = this.params.getTyped("indu");
		this.loss = this.params.getTyped("loss");
		this.regul = this.params.getTyped("regul");
	}
	
	private void initUW(int xrows, int xcols, int ycols) {
		
		InitStrategy strat = this.params.getTyped("initstrat");
		if(indw) this.w = strat.init(xrows, ycols);
		else this.w = strat.init(xrows, 1);
		if(indu) this.u = strat.init(xcols, ycols);
		else this.u = strat.init(xcols, 1);
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
			Matrix Dprime = this.u.transpose().times(X.transpose());
			Matrix Vprime = X.transpose().times(this.w);
			
			loss.setX(Dprime);
			Matrix gradW = loss.gradient(this.w);
			
			loss.setX(Vprime);
			Matrix gradU = loss.gradient(this.u);
			
			Matrix neww = this.w.minus(gradW);
			Matrix newu = this.u.minus(gradU);
			
//			sumchange = abs(neww.minus(this.w))
		}
	}
}
