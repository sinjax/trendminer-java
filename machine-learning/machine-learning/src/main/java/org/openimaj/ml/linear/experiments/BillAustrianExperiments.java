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
import org.openimaj.ml.linear.data.BillMatlabFileDataGenerator;
import org.openimaj.ml.linear.data.BillMatlabFileDataGenerator.Mode;
import org.openimaj.ml.linear.learner.BilinearSparseOnlineLearner;
import org.openimaj.util.pair.Pair;

public class BillAustrianExperiments {
	private static final String BILL_DATA = "/Users/ss/Dropbox/TrendMiner/deliverables/year2-"
			+ "18month/Austrian Data/data.mat";
	
	
	
	private static void pre() {
		ConsoleAppender console = new ConsoleAppender(); //create appender
		//configure the appender
		String PATTERN = "%d [%p|%c|%C{1}] %m%n";
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
			}
			
			double loss = BilinearSparseOnlineLearner.sumLoss(testpairs, learner.getU(), learner.getW(), learner.getParams());
			
			System.out.println(String.format("Fold %d, Loss: %f",i,loss));
		}

	}
}
