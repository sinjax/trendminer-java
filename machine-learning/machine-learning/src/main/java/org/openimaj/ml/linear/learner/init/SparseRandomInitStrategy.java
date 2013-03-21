package org.openimaj.ml.linear.learner.init;

import gov.sandia.cognition.math.matrix.Matrix;
import gov.sandia.cognition.math.matrix.mtj.SparseMatrixFactoryMTJ;

public class SparseRandomInitStrategy implements InitStrategy{
	SparseMatrixFactoryMTJ smf = SparseMatrixFactoryMTJ.INSTANCE;
	@Override
	public Matrix init(int rows, int cols) {
		return smf.createMatrix(rows, cols);
	}

}
