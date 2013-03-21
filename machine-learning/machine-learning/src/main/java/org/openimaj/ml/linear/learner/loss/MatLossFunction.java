package org.openimaj.ml.linear.learner.loss;

import gov.sandia.cognition.math.matrix.Matrix;
import gov.sandia.cognition.math.matrix.mtj.SparseMatrix;
import gov.sandia.cognition.math.matrix.mtj.SparseMatrixFactoryMTJ;

public class MatLossFunction extends LossFunction{
	
	private LossFunction f;
	private SparseMatrixFactoryMTJ spf;
	public MatLossFunction(LossFunction f) {
		this.f = f;
		spf = SparseMatrixFactoryMTJ.INSTANCE;
	}
	
	@Override
	public void setX(Matrix X) {
		super.setX(X);
		f.setX(X);
	}
	
	@Override
	public void setY(Matrix Y) {
		super.setY(Y);
		f.setY(Y);
	}
	@Override
	public Matrix gradient(Matrix W) {
		SparseMatrix ret = spf.createMatrix(W.getNumRows(), W.getNumColumns());
		for (int i = 0; i < Y.getNumColumns(); i++) {
			Matrix submatrix = f.gradient(W);
			ret.setSubMatrix(0, i, submatrix);
		}
		return ret;
	}

	@Override
	public double eval(Matrix W) {
		int total = 0;
		for (int i = 0; i < Y.getNumColumns(); i++) {
			total += f.eval(W);
		}
		return total;
	}

}
