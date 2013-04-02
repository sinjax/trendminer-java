package org.openimaj.ml.linear.learner.regul;

import gov.sandia.cognition.math.matrix.Matrix;

public class L1L2Regulariser implements Regulariser{

	@Override
	public Matrix prox(Matrix W, double lambda) {
		int nrows = W.getNumRows();
		int ncols = W.getNumColumns();
		Matrix ret = W.clone();
		ret.zero();
		
		for (int r = 0; r < nrows; r++) {
			double rownorm = W.getRow(r).norm2();
			if(rownorm > lambda){
				double scal = (rownorm - lambda)/rownorm;
				for (int c = 0; c < ncols; c++) {
					ret.setElement(r, c, W.getElement(r, c) * scal);
				}
			}
		}
		return ret;
	}

}
