package org.openimaj.ml.linear.learner.loss;

import gov.sandia.cognition.math.matrix.Matrix;
import gov.sandia.cognition.math.matrix.Vector;

public class SquareMissingLossFunction extends LossFunction{

	@Override
	public Matrix gradient(Matrix W) {
		Matrix resid = X.times(W).minus(Y);
		for (int r = 0; r < Y.getNumRows(); r++) {
			double yc = Y.getElement(r, 0);
			if(Double.isNaN(yc)){
				resid.setElement(r,0, 0);
			}
		}
		return X.transpose().times(resid);
	}

	@Override
	public double eval(Matrix W) {
		Matrix v = X.times(W).minus(Y);
		Vector zeroVect = Y.getRow(0);
		zeroVect.zero();
		for (int c = 0; c < Y.getNumRows(); c++) {
			double yc = Y.getElement(0, c);
			if(Double.isNaN(yc)){
				v.setRow(c, zeroVect);
			}
		}
		return v.sumOfColumns().sum();
	}
	
}
