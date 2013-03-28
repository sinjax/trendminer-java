package org.openimaj.ml.linear.experiments;

import gov.sandia.cognition.math.matrix.Matrix;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.openimaj.math.matrix.MatrixUtils;
import org.openimaj.math.matrix.SandiaMatrixUtils;
import org.openimaj.ml.linear.data.BillMatlabFileDataGenerator;
import org.openimaj.ml.linear.data.BillMatlabFileDataGenerator.Mode;
import org.openimaj.ml.linear.evaluation.BilinearEvaluator;
import org.openimaj.ml.linear.evaluation.RootMeanSumLossEvaluator;
import org.openimaj.ml.linear.evaluation.SumLossEvaluator;
import org.openimaj.ml.linear.learner.BilinearSparseOnlineLearner;
import org.openimaj.ml.linear.learner.BilinearSparseOnlineLearner.BilinearLearnerParameters;
import org.openimaj.ml.linear.learner.init.OnesInitStrategy;
import org.openimaj.ml.linear.learner.init.ZerosInitStrategy;
import org.openimaj.util.pair.Pair;

public class BillAustrianExperiments {
	private static final String BILL_DATA = "%s/TrendMiner/deliverables/year2-"
			+ "18month/Austrian Data/data.mat";
	
	static Logger logger = Logger.getLogger(BillAustrianExperiments.class);
	
	private static void pre() throws IOException {
		ConsoleAppender console = new ConsoleAppender(); //create appender
		//configure the appender
		String PATTERN = "[%p->%C{1}] %m%n";
		console.setLayout(new PatternLayout(PATTERN)); 
		console.setThreshold(Level.DEBUG);
		console.activateOptions();
	  	// add appender to any Logger (here is root)
		Logger.getRootLogger().addAppender(console);
		String logFile = BILL_DATA() + ".log";
		File f = new File(logFile);
		if(f.exists())f.delete();
		FileAppender file = new FileAppender(new PatternLayout(PATTERN), logFile); 
		file.setThreshold(Level.DEBUG);
		file.activateOptions();
		Logger.getRootLogger().addAppender(file );
		
	}
	
	public static void main(String[] args) throws IOException {
		pre();
		BillMatlabFileDataGenerator bmfdg = new BillMatlabFileDataGenerator(new File(BILL_DATA()), 98);
		for (int i = 0; i < bmfdg.nFolds(); i++) {
			logger.debug("Fold: " + i);
			BilinearSparseOnlineLearner learner = new BilinearSparseOnlineLearner();
			
			BilinearLearnerParameters params = learner.getParams();
			params.put(BilinearLearnerParameters.ETA0_U, 0.00002);
			params.put(BilinearLearnerParameters.ETA0_W, 0.00002);
			params.put(BilinearLearnerParameters.DIMWEIGHTED, false);
			params.put(BilinearLearnerParameters.LAMBDA, 0.001);
			params.put(BilinearLearnerParameters.BICONVEX_TOL, 0.01);
			params.put(BilinearLearnerParameters.BICONVEX_MAXITER, 10);
			params.put(BilinearLearnerParameters.BIAS, true);
			params.put(BilinearLearnerParameters.BIASETA0, 0.05);
			params.put(BilinearLearnerParameters.WINITSTRAT, new OnesInitStrategy());
			params.put(BilinearLearnerParameters.UINITSTRAT, new ZerosInitStrategy());
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
				Matrix bias = learner.getBias();
				BilinearEvaluator eval = new RootMeanSumLossEvaluator();
				eval.setLearner(learner);
				double loss = eval.evaluate(testpairs);
				logger.debug("W sparcity: " + SandiaMatrixUtils.rowSparcity(w));
				logger.debug("U sparcity: " + SandiaMatrixUtils.rowSparcity(u));
				if(learner.getParams().getTyped(BilinearLearnerParameters.BIAS)){
					logger.debug("Bias: " + SandiaMatrixUtils.diag(bias));
				}
				logger.debug(String.format("... loss: %f",loss));
			}
		}		
	}
	private static String BILL_DATA() {
		
		return String.format(BILL_DATA,DROPBOX_HOME());
	}

	private static String DROPBOX_HOME() {
		String home = System.getProperty("user.home");

		return String.format("%s/Dropbox",home);
	}
}
