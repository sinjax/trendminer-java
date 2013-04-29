package org.openimaj.ml.linear.experiments;

import gov.sandia.cognition.math.matrix.Matrix;
import gov.sandia.cognition.math.matrix.MatrixFactory;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.openimaj.io.IOUtils;
import org.openimaj.math.matrix.SandiaMatrixUtils;
import org.openimaj.ml.linear.data.BillMatlabFileDataGenerator;
import org.openimaj.ml.linear.data.BillMatlabFileDataGenerator.Mode;
import org.openimaj.ml.linear.evaluation.BilinearEvaluator;
import org.openimaj.ml.linear.evaluation.RootMeanSumLossEvaluator;
import org.openimaj.ml.linear.learner.BilinearLearnerParameters;
import org.openimaj.ml.linear.learner.BilinearSparseOnlineLearner;
import org.openimaj.ml.linear.learner.init.OnesInitStrategy;
import org.openimaj.ml.linear.learner.init.SingleValueInitStrat;
import org.openimaj.ml.linear.learner.init.SparseOnesInitStrategy;
import org.openimaj.ml.linear.learner.init.SparseRowOnesInitStrategy;
import org.openimaj.ml.linear.learner.init.SparseZerosInitStrategy;
import org.openimaj.util.pair.Pair;

public class BillAustrianExperiments extends BilinearExperiment {
	
	public static void main(String[] args) throws IOException {
		BillAustrianExperiments exp = new BillAustrianExperiments();
		exp.performExperiment();
	}

	@Override
	public void performExperiment() throws IOException {
		BilinearLearnerParameters params = new BilinearLearnerParameters();
		params.put(BilinearLearnerParameters.ETA0_U, 0.02);
		params.put(BilinearLearnerParameters.ETA0_W, 0.02);
		params.put(BilinearLearnerParameters.LAMBDA, 0.001);
		params.put(BilinearLearnerParameters.BICONVEX_TOL, 0.01);
		params.put(BilinearLearnerParameters.BICONVEX_MAXITER, 10);
		params.put(BilinearLearnerParameters.BIAS, true);
		params.put(BilinearLearnerParameters.ETA0_BIAS, 0.5);
		params.put(BilinearLearnerParameters.WINITSTRAT, new SingleValueInitStrat(0.1));
		params.put(BilinearLearnerParameters.UINITSTRAT, new SparseZerosInitStrategy());
		BillMatlabFileDataGenerator bmfdg = new BillMatlabFileDataGenerator(
				new File(BILL_DATA()), 
				98,
				true
		);
		prepareExperimentLog(params);
		for (int i = 0; i < bmfdg.nFolds(); i++) {
			logger.debug("Fold: " + i);
			BilinearSparseOnlineLearner learner = new BilinearSparseOnlineLearner(params);
			learner.reinitParams();
			
			bmfdg.setFold(i, Mode.TEST);
			List<Pair<Matrix>> testpairs = new ArrayList<Pair<Matrix>>(); 
			while(true){
				Pair<Matrix> next = bmfdg.generate();
				if(next == null) break;
				testpairs.add(next);
			}
			logger.debug("...training");
			bmfdg.setFold(i, Mode.TRAINING);
			int j = 0;
			while(true){
				Pair<Matrix> next = bmfdg.generate();
				if(next == null) break;
				logger.debug("...trying item "+j++);
				learner.process(next.firstObject(), next.secondObject());
				Matrix u = learner.getU();
				Matrix w = learner.getW();
				Matrix bias = MatrixFactory.getDenseDefault().copyMatrix(learner.getBias());
				BilinearEvaluator eval = new RootMeanSumLossEvaluator();
				eval.setLearner(learner);
				double loss = eval.evaluate(testpairs);
				logger.debug(String.format("Saving learner, Fold %d, Item %d",i, j));
				File learnerOut = new File(FOLD_ROOT(i),String.format("learner_%d",j));
				IOUtils.writeBinary(learnerOut, learner);
				logger.debug("W row sparcity: " + SandiaMatrixUtils.rowSparcity(w));
				logger.debug("U row sparcity: " + SandiaMatrixUtils.rowSparcity(u));
				Boolean biasMode = learner.getParams().getTyped(BilinearLearnerParameters.BIAS);
				if(biasMode){
					logger.debug("Bias: " + SandiaMatrixUtils.diag(bias));
				}
				logger.debug(String.format("... loss: %f",loss));
			}
		}	
	}
	
}
