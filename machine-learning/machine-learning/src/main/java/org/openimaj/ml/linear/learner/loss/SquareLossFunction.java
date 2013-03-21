package org.openimaj.ml.linear.learner.loss;

import gov.sandia.cognition.math.matrix.Matrix;

public class SquareLossFunction extends LossFunction{

	@Override
	public Matrix gradient(Matrix W) {
		return X.transpose().times(X.times(W).minus(Y));
	}

	@Override
	public double eval(Matrix W) {
		Matrix v = (X.times(W).minus(Y));
		v.dotTimesEquals(v);
		return v.sumOfRows().sum();
	}
	
}
