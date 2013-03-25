package org.openimaj.ml.linear.data;

import static org.junit.Assert.assertTrue;
import gov.sandia.cognition.math.matrix.Matrix;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openimaj.io.FileUtils;
import org.openimaj.ml.linear.learner.BilinearSparseOnlineLearner;
import org.openimaj.util.pair.Pair;

public class BilinearSparseOnlineLearnerTest {
	Logger logger = Logger.getLogger(BilinearSparseOnlineLearnerTest.class);
	/**
	 * the output folder
	 */
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private File matfile;
	private File winitfile;
	private File uinitfile;
	@Before
	public void before() throws IOException {
		ConsoleAppender console = new ConsoleAppender(); //create appender
		//configure the appender
		String PATTERN = "%d [%p|%c|%C{1}] %m%n";
		console.setLayout(new PatternLayout(PATTERN)); 
		console.setThreshold(Level.DEBUG);
		console.activateOptions();
	  	// add appender to any Logger (here is root)
		Logger.getRootLogger().addAppender(console);
		matfile = folder.newFile("tmp.mat");
		InputStream stream = MatlabFileDataGeneratorTest.class.getResourceAsStream("/org/openimaj/ml/linear/data/XYs.mat");
		FileUtils.copyStreamToFileBinary(stream, matfile);
		winitfile = folder.newFile("inittmpw.mat");
		stream = MatlabFileDataGeneratorTest.class.getResourceAsStream("/org/openimaj/ml/linear/data/Winit.mat");
		FileUtils.copyStreamToFileBinary(stream, winitfile);
		
		uinitfile = folder.newFile("inittmpu.mat");
		stream = MatlabFileDataGeneratorTest.class.getResourceAsStream("/org/openimaj/ml/linear/data/Uinit.mat");
		FileUtils.copyStreamToFileBinary(stream, uinitfile);
	}

	@Test
	public void testBilinear() throws IOException {
		BiconvexDataGenerator gen = new BiconvexDataGenerator(
				5, 10, 3, 0.3, // users, words, tasks, sparcity
				true, true, -1, 0); // indw, indu, seed, noise
//		MatlabFileDataGenerator gen = new MatlabFileDataGenerator(matfile);
		BilinearSparseOnlineLearner learner = new BilinearSparseOnlineLearner();
//		learner.getParams().put("winitstrat", new MatlabFileInitStrat(winitfile));
//		learner.getParams().put("uinitstrat", new MatlabFileInitStrat(uinitfile));
		int dataitems = 400;
		List<Pair<Matrix>> pairs = new ArrayList<Pair<Matrix>>();
		double first100 = 0;
		double second100 = 0;
		for (int i = 0; i < dataitems; i++) {
			Pair<Matrix> xy = gen.generate();
			if(xy == null) continue;
			pairs.add(xy);
			learner.process(xy.firstObject(), xy.secondObject());
			double loss = learner.sumLoss(pairs);
			if(i / 200 == 0) first100 += loss/(i+1);
			else if(i / 200 == 1) second100 += loss/(i+1);
			logger.debug(String.format("Pair %d, Loss = %f", i, loss));
		}
		logger.debug("First 200:" + first100/200);
		logger.debug("Second 200:" + second100/200);
		assertTrue(first100 > second100);
	}
}
