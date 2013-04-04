package org.openimaj.ml.linear.data;

import static org.junit.Assert.assertTrue;
import gov.sandia.cognition.math.matrix.Matrix;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openimaj.io.FileUtils;
import org.openimaj.ml.linear.evaluation.BilinearEvaluator;
import org.openimaj.ml.linear.evaluation.SumLossEvaluator;
import org.openimaj.ml.linear.learner.BilinearSparseOnlineLearner;
import org.openimaj.ml.linear.learner.IncrementalBilinearSparseOnlineLearner;
import org.openimaj.util.pair.IndependentPair;
import org.openimaj.util.pair.Pair;

public class IncrementalBilinearSparseOnlineLearnerTest {
	Logger logger = Logger.getLogger(IncrementalBilinearSparseOnlineLearnerTest.class);
	/**
	 * the output folder
	 */
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	@Before
	public void before() throws IOException {
		ConsoleAppender console = new ConsoleAppender(); //create appender
		//configure the appender
		String PATTERN = "%d [%p|%c|%C{1}] %m%n";
		console.setLayout(new PatternLayout(PATTERN)); 
		console.setThreshold(Level.FATAL);
		console.activateOptions();
	  	// add appender to any Logger (here is root)
		Logger.getRootLogger().addAppender(console);
	}

	@Test
	public void testBilinear() throws IOException {
		int nfeatures = 10;
		int nusers = 5;
		int ntasks = 3;
		BiconvexIncrementalDataGenerator gen = new BiconvexIncrementalDataGenerator (
				nusers,nfeatures,ntasks, 0.3,0.3, // users, words, tasks, sparcity, xsparcity
				true, true, -1, 0); // indw, indu, seed, noise
		IncrementalBilinearSparseOnlineLearner learner = new IncrementalBilinearSparseOnlineLearner();
		int dataitems = 400;
		List<Pair<Matrix>> pairs = new ArrayList<Pair<Matrix>>();
		double first100 = 0;
		double second100 = 0;
		for (int i = 0; i < dataitems; i++) {
			IndependentPair<Map<String, Map<String, Double>>, Map<String, Double>> xy = gen.generate();
			if(xy == null) continue;
			learner.process(xy.firstObject(), xy.secondObject());
			pairs.add(learner.asMatrixPair(xy, nfeatures, nusers, ntasks));
			BilinearEvaluator eval = new SumLossEvaluator();
			eval.setLearner(learner.getBilinearLearner());
			double loss = eval.evaluate(pairs);
			if(i / 200 == 0) first100 += loss/(i+1);
			else if(i / 200 == 1) second100 += loss/(i+1);
			logger.debug(String.format("Pair %d, Loss = %f", i, loss));
		}
		logger.debug("First 200:" + first100/200);
		logger.debug("Second 200:" + second100/200);
		assertTrue(first100 > second100);
	}
}
