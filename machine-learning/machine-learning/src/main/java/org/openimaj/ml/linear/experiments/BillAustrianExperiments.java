package org.openimaj.ml.linear.experiments;

import gov.sandia.cognition.math.matrix.Matrix;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.openimaj.math.matrix.MatrixUtils;
import org.openimaj.math.matrix.SandiaMatrixUtils;
import org.openimaj.ml.linear.data.BillMatlabFileDataGenerator;
import org.openimaj.ml.linear.data.BillMatlabFileDataGenerator.Mode;
import org.openimaj.ml.linear.learner.BilinearSparseOnlineLearner;
import org.openimaj.ml.linear.learner.BilinearSparseOnlineLearner.BilinearLearnerParameters;
import org.openimaj.ml.linear.learner.init.OnesInitStrategy;
import org.openimaj.ml.linear.learner.init.ZerosInitStrategy;
import org.openimaj.util.pair.Pair;

public class BillAustrianExperiments {
	private static final String BILL_DATA = "/home/ss/Dropbox/TrendMiner/deliverables/year2-"
			+ "18month/Austrian Data/data.mat";
	
	
	
	private static void pre() {
		ConsoleAppender console = new ConsoleAppender(); //create appender
		//configure the appender
		String PATTERN = "[%p->%C{1}] %m%n";
		console.setLayout(new PatternLayout(PATTERN)); 
		console.setThreshold(Level.DEBUG);
		console.activateOptions();
	  	// add appender to any Logger (here is root)
		Logger.getRootLogger().addAppender(console);
	}
	
	public static void main(String[] args) throws IOException {
		pre();
		BillMatlabFileDataGenerator bmfdg = new BillMatlabFileDataGenerator(new File(BILL_DATA), 98);
		for (int i = 0; i < bmfdg.nFolds(); i++) {
			System.out.println("Fold: " + i);
			BilinearSparseOnlineLearner learner = new BilinearSparseOnlineLearner();
			
			BilinearLearnerParameters params = learner.getParams();
			params.put(BilinearLearnerParameters.ETA0, 0.0002);
			params.put(BilinearLearnerParameters.LAMBDA, 0.0001);
			params.put(BilinearLearnerParameters.BICONVEX_TOL, 0.01);
			params.put(BilinearLearnerParameters.BICONVEX_MAXITER, 100);
			params.put(BilinearLearnerParameters.BIAS, false);
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
			System.out.println("...training");
			bmfdg.setFold(i, Mode.TRAINING);
			int j = 0;
			while(true){
				Pair<Matrix> next = bmfdg.generate();
				if(next == null) break;
				System.out.println("...trying item "+j++);
				learner.process(next.firstObject(), next.secondObject());
				Matrix u = learner.getU();
				Matrix w = learner.getW();
//				System.out.println("W: " + w);
//				System.out.println("U: " + u);
				
				double loss = BilinearSparseOnlineLearner.sumLoss(testpairs, u, w, learner.getParams());
				System.out.println("W sparcity: " + SandiaMatrixUtils.rowSparcity(w));
				System.out.println("U sparcity: " + SandiaMatrixUtils.rowSparcity(u));
				System.out.println(String.format("... loss: %f",loss));
			}
			
			
		}

	}
}
