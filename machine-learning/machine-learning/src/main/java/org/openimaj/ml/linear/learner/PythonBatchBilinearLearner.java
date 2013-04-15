package org.openimaj.ml.linear.learner;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;

import gov.sandia.cognition.math.matrix.Matrix;
import gov.sandia.cognition.math.matrix.Vector;
import gov.sandia.cognition.math.matrix.mtj.DenseMatrixFactoryMTJ;
import gov.sandia.cognition.math.matrix.mtj.SparseMatrix;
import gov.sandia.cognition.math.matrix.mtj.SparseMatrixFactoryMTJ;

import org.apache.log4j.Logger;
import org.openimaj.io.ReadWriteableBinary;
import org.openimaj.math.matrix.SandiaMatrixUtils;
import org.openimaj.ml.linear.learner.init.InitStrategy;
import org.openimaj.ml.linear.learner.loss.LossFunction;
import org.openimaj.ml.linear.learner.loss.MatLossFunction;
import org.openimaj.ml.linear.learner.regul.Regulariser;


/**
 * An Batch Bilinear Learner which can be called in an online fashion. This implementation holds all 
 * data handed to {@link #process(Matrix, Matrix)} and retrains the underlying model from scratch
 * against all seen data.
 * 
 *  This specific version is backed by Python batch implementations of the Bilinear Model.
 *  The python tool is called using java {@link Process}.
 * 
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class PythonBatchBilinearLearner implements OnlineLearner<Matrix,Matrix>{
	
	static Logger logger = Logger.getLogger(PythonBatchBilinearLearner.class);
	
	protected BilinearLearnerParameters params;
	protected Matrix w;
	protected Matrix u;
	protected Boolean indu;
	protected Boolean indw;
	protected SparseMatrixFactoryMTJ smf = SparseMatrixFactoryMTJ.INSTANCE;
	protected LossFunction loss;
	protected Regulariser regul;
	protected Double lambda;
	protected Boolean biasMode;
	protected Matrix bias;
	protected Matrix diagX;
	protected Double eta0_u;
	protected Double eta0_w;

	private Boolean forceSparcity;
	
	public PythonBatchBilinearLearner() {
		this(new BilinearLearnerParameters());
	}
	public PythonBatchBilinearLearner(BilinearLearnerParameters params) {
		this.params = params;
		reinitParams();
	}
	
	public void reinitParams() {
		this.indw = this.params.getTyped(BilinearLearnerParameters.INDW);
		this.indu = this.params.getTyped(BilinearLearnerParameters.INDU);
		this.loss = this.params.getTyped(BilinearLearnerParameters.LOSS);
		this.regul = this.params.getTyped(BilinearLearnerParameters.REGUL);
		this.lambda = this.params.getTyped(BilinearLearnerParameters.LAMBDA);
		this.biasMode = this.params.getTyped(BilinearLearnerParameters.BIAS);
		this.eta0_u = this.params.getTyped(BilinearLearnerParameters.ETA0_U);
		this.eta0_w = this.params.getTyped(BilinearLearnerParameters.ETA0_W);
		this.forceSparcity = this.params.getTyped(BilinearLearnerParameters.FORCE_SPARCITY);
		
		if(indw && indu){
			this.loss = new MatLossFunction(this.loss);
		}
	}
	
	public void process(Matrix X, Matrix Y){
	}
	
	public BilinearLearnerParameters getParams() {
		return this.params;
	}
	
	public Matrix getU(){
		return this.u;
	}
	
	public Matrix getW(){
		return this.w;
	}
	public Matrix getBias() {
		if(this.biasMode)
			return this.bias;
		else
			return null;
	}
	
	public PythonBatchBilinearLearner clone(){
		PythonBatchBilinearLearner ret = new PythonBatchBilinearLearner(this.getParams());
		ret.u = this.u.clone();
		ret.w = this.w.clone();
		return ret;
	}
//	@Override
//	public void readBinary(DataInput in) throws IOException {
//		int nwords = in.readInt();
//		int nusers = in.readInt();
//		int ntasks = in.readInt();
//		
//		
//		this.w = SparseMatrixFactoryMTJ.INSTANCE.createMatrix(nwords, ntasks);
//		for (int t = 0; t < ntasks; t++) {				
//			for (int r = 0; r < nwords; r++) {
//				double readDouble = in.readDouble();
//				if(readDouble != 0){
//					this.w.setElement(r, t, readDouble);					
//				}
//			}
//		}
//		
//		this.u = SparseMatrixFactoryMTJ.INSTANCE.createMatrix(nusers, ntasks);
//		for (int t = 0; t < ntasks; t++) {				
//			for (int r = 0; r < nusers; r++) {
//				double readDouble = in.readDouble();
//				if(readDouble != 0){
//					this.u.setElement(r, t, readDouble);					
//				}
//			}
//		}
//		
//		this.bias = SparseMatrixFactoryMTJ.INSTANCE.createMatrix(ntasks, ntasks);
//		for (int t1 = 0; t1 < ntasks; t1++) {
//			for (int t2 = 0; t2 < ntasks; t2++) {				
//				double readDouble = in.readDouble();
//				if(readDouble != 0){
//					this.bias.setElement(t1, t2, readDouble);					
//				}
//			}
//		}
//	}
//	@Override
//	public byte[] binaryHeader() {
//		return "".getBytes();
//	}
//	@Override
//	public void writeBinary(DataOutput out) throws IOException {
//		out.writeInt(w.getNumRows());
//		out.writeInt(u.getNumRows());
//		out.writeInt(u.getNumColumns());
//		double[] wdata = SandiaMatrixUtils.getData(w);
//		for (int i = 0; i < wdata.length; i++) {
//			out.writeDouble(wdata[i]);
//		}
//		double[] udata = SandiaMatrixUtils.getData(u);
//		for (int i = 0; i < udata.length; i++) {
//			out.writeDouble(udata[i]);
//		}
//		double[] biasdata = SandiaMatrixUtils.getData(bias);
//		for (int i = 0; i < biasdata.length; i++) {
//			out.writeDouble(biasdata[i]);
//		}		
//	}
}
