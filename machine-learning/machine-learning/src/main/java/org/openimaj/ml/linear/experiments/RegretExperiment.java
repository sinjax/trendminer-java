package org.openimaj.ml.linear.experiments;

import gov.sandia.cognition.math.matrix.Matrix;
import gov.sandia.cognition.math.matrix.MatrixFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openimaj.io.IOUtils;
import org.openimaj.math.matrix.SandiaMatrixUtils;
import org.openimaj.ml.linear.data.BillMatlabFileDataGenerator;
import org.openimaj.ml.linear.data.BillMatlabFileDataGenerator.Mode;
import org.openimaj.ml.linear.evaluation.BilinearEvaluator;
import org.openimaj.ml.linear.evaluation.RootMeanSumLossEvaluator;
import org.openimaj.ml.linear.learner.BilinearLearnerParameters;
import org.openimaj.ml.linear.learner.BilinearSparseOnlineLearner;
import org.openimaj.ml.linear.learner.init.SingleValueInitStrat;
import org.openimaj.ml.linear.learner.init.SparseZerosInitStrategy;
import org.openimaj.util.pair.Pair;

public class RegretExperiment extends BilinearExperiment{

	@Override
	public void performExperiment() throws Exception {
		BilinearLearnerParameters params = new BilinearLearnerParameters();
		params.put(BilinearLearnerParameters.ETA0_U, 0.02);
		params.put(BilinearLearnerParameters.ETA0_W, 0.02);
		params.put(BilinearLearnerParameters.LAMBDA, 0.001);
		params.put(BilinearLearnerParameters.BICONVEX_TOL, 0.01);
		params.put(BilinearLearnerParameters.BICONVEX_MAXITER, 10);
		params.put(BilinearLearnerParameters.BIAS, true);
		params.put(BilinearLearnerParameters.BIASETA0, 0.5);
		params.put(BilinearLearnerParameters.WINITSTRAT, new SingleValueInitStrat(0.1));
		params.put(BilinearLearnerParameters.UINITSTRAT, new SparseZerosInitStrategy());
		BillMatlabFileDataGenerator bmfdg = new BillMatlabFileDataGenerator(
				new File(BILL_DATA()), 
				98,
				true
		);
		prepareExperimentLog(params);
		BilinearSparseOnlineLearner learner = new BilinearSparseOnlineLearner(params);
		bmfdg.setFold(-1, Mode.ALL); // go through all items in day order
		int j = 0;
		while(true){
			Pair<Matrix> next = bmfdg.generate();
			if(next == null) break;
			
			logger.debug("...Calculating regret for item"+j);
			// Calculate regret using current model
			BilinearEvaluator eval = new RootMeanSumLossEvaluator();
			eval.setLearner(learner);
			List<Pair<Matrix>> asList = Arrays.asList(next);
			double loss = eval.evaluate(asList);
			logger.debug(String.format("... loss: %f",loss));
			
			learner.process(next.firstObject(), next.secondObject());
			logger.debug(String.format("Saving learner, Fold %d, Item %d",-1, j));
			File learnerOut = new File(FOLD_ROOT(-1),String.format("learner_%d",j));
			IOUtils.writeBinary(learnerOut, learner);
			
			j++;
		}
	}

}
