package org.openimaj.math.matrix;

import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;

import gov.sandia.cognition.math.matrix.Matrix;
import gov.sandia.cognition.math.matrix.MatrixFactory;
import gov.sandia.cognition.math.matrix.mtj.DenseMatrix;
import gov.sandia.cognition.math.matrix.mtj.SparseMatrixFactoryMTJ;

public class SandiaMatrixUtils {

	public static Matrix abs(Matrix mat) {
		Matrix ret = mat.clone();
		int nrows = ret.getNumRows();
		int ncols = ret.getNumColumns();
		for (int r = 0; r < nrows; r++) {
			for (int c = 0; c < ncols; c++) {
				ret.setElement(r, c, Math.abs(mat.getElement(r, c)));
			}
		}
		return ret;
	}

	public static double absSum(Matrix mat) {
		double tot = 0;
		int nrows = mat.getNumRows();
		int ncols = mat.getNumColumns();
		for (int r = 0; r < nrows; r++) {
			for (int c = 0; c < ncols; c++) {
				tot += Math.abs(mat.getElement(r, c));
			}
		}
		return tot;
	}

	public static Matrix timesInplace(Matrix mat, double etat) {
		int nrows = mat.getNumRows();
		int ncols = mat.getNumColumns();
		for (int r = 0; r < nrows; r++) {
			for (int c = 0; c < ncols; c++) {
				mat.setElement(r, c, mat.getElement(r, c) * etat);
			}
		}
		return mat;
	}

	public static Matrix asMat(MLArray mlArray) {
		MLDouble mlArrayDbl = (MLDouble) mlArray;
		int rows = mlArray.getM();
		int cols = mlArray.getN();
		
		Matrix mat = SparseMatrixFactoryMTJ.INSTANCE.createMatrix(rows, cols);
		
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				mat.setElement(r, c, mlArrayDbl.get(r, c));
			}
		}
		return mat;
	}

	public static double rowSparcity(Matrix mat) {
		double nrows = mat.getNumRows();
		double nsparse = 0;
		for (int r = 0; r < nrows; r++) {
			if(mat.getRow(r).sum() == 0){
				nsparse ++;
			}
		}
		return nsparse/nrows;
	}
	
	public static double colSparcity(Matrix mat) {
		double ncols = mat.getNumColumns();
		double nsparse = 0;
		for (int c = 0; c < ncols; c++) {
			if(mat.getColumn(c).sum() == 0){
				nsparse ++;
			}
		}
		return nsparse/ncols;
	}

	public static Matrix plusInplace(Matrix mat, double etat) {
		int nrows = mat.getNumRows();
		int ncols = mat.getNumColumns();
		for (int r = 0; r < nrows; r++) {
			for (int c = 0; c < ncols; c++) {
				mat.setElement(r, c, mat.getElement(r, c) + etat);
			}
		}
		return mat;
	}

	/**
	 * Add a row to the bottom of this matrix with the value v
	 * 
	 * @param transpose
	 * @param v
	 * @return
	 */
	public static <T extends Matrix> T vstackValue(Matrix mat, MatrixFactory<T> fact, double v) {
		int ncols = mat.getNumColumns();
		int nrows = mat.getNumRows();
		T ret = fact.createMatrix(mat.getNumRows()+1, mat.getNumColumns());
		ret.setSubMatrix(0, 0, mat);
		for (int i = 0; i < ncols ; i++) {
			mat.setElement(nrows-1, i, v);
		}
		return ret;
	}

	/**
	 * Add a col to the end of this matrix with the value v
	 * @param mat
	 * @param fact
	 * @param v
	 * @return
	 */
	public static <T extends Matrix> Matrix hstackValue(Matrix mat, MatrixFactory<T> fact, double v) {
		int ncols = mat.getNumColumns();
		int nrows = mat.getNumRows();
		T ret = fact.createMatrix(mat.getNumRows(), mat.getNumColumns()+1);
		ret.setSubMatrix(0, 0, mat);
		for (int i = 0; i < nrows ; i++) {
			mat.setElement(i, ncols-1, v);
		}
		return ret;
	}
	
}
