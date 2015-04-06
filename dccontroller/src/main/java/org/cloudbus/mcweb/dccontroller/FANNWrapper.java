package org.cloudbus.mcweb.dccontroller;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.base.Preconditions;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;

public class FANNWrapper {

    /** Logger. */
    private static final Logger LOG = Logger.getLogger(FANNWrapper.class.getCanonicalName());
	
	private static NativeLibrary fann;
	static {
		NativeLibrary fann = NativeLibrary.getInstance("fann");
		Native.register(fann);
	}

	private static String bufferFile =  Paths.get(System.getProperty("user.home"), "buffer-ann.txt").toString();

	private int[] topology;
	private float momentum;
	private float learning_rate;
	private float connection_rate;
	private Pointer ann;
	
	/**
	 * Constr.
	 * @param topology: A vector of integers, specifying the number of neurons in each layer. Must not be None. Must have more than 1 element.
	 * @param inputMomentum: The training momentum. Must be in the interval [0,1).
	 * @param learning_rate: The learning rate. Must be in the interval [0,1).
	 * @param connection_rate: The FANN connection rate. Must be an integer greater or equal to 1.
	 */
	public FANNWrapper(int[] topology, float inputMomentum, float learning_rate, float connection_rate) {
		Preconditions.checkNotNull(topology);
		List<Integer> listTopology = Arrays.asList( ArrayUtils.toObject(topology)  );
		Preconditions.checkArgument(listTopology.stream().allMatch( x -> x > 0 ));
		Preconditions.checkArgument(0 <= inputMomentum && inputMomentum < 1);
		Preconditions.checkArgument(0 <= learning_rate && learning_rate < 1);
		Preconditions.checkArgument(connection_rate >= 1);
	
        this.topology = topology;
        this.momentum = inputMomentum;
        this.learning_rate = learning_rate;
        this.connection_rate = connection_rate;
        
		ann = fann_create_sparse_array(connection_rate, topology.length, topology);
		
		fann_set_learning_rate(ann, learning_rate);
		fann_set_learning_momentum(ann, inputMomentum);
		fann_set_activation_function_hidden(ann, ActivationFunction.FANN_SIGMOID.ordinal());
		fann_set_activation_function_output(ann, ActivationFunction.FANN_LINEAR.ordinal());
		fann_set_training_algorithm(ann, TrainingAlgo.FANN_TRAIN_INCREMENTAL.ordinal());
		fann_randomize_weights(ann, -0.1f, 0.1f);
	}
	
	public FANNWrapper(int[] topology, float inputMomentum, float learning_rate) {
		this(topology, inputMomentum, learning_rate, 1);
	}

	public FANNWrapper(int[] topology) {
		this(topology, 0.05f, 0.01f);
	}
	
	public FANNWrapper() {
		this(new int[]{3, 250, 2});
	}

	/**
	 *  Trains the underlying neural network, until trainTimes iterations have been performed, or the RMSE has been achieved.
	 *  @param n: Number of users. Must not be None. Must be non-negative integer.
	 *  @param expOutput: The expected output from the NN. Must be of appropriate size. Must not be None.
	 *  @param trainTimes: How many training iterations to perform. An integer, greater or equal to 1. Must not be None. 
	 *  @param revert: Boolean flag, whether to revert the changes to the NN after the training.
	 *  @param maxRMSE: If not None, trains until the RMSE is achieved. May be None. May not be 0 or negative.
	 *  @return: The RMSE after the training.
	 */
    public float train(int n, float[] expOutput, int trainTimes, boolean revert, Float maxRMSE) {
    	Preconditions.checkArgument(n >= 0, String.format("Invalid n: %s", n));
    	Preconditions.checkNotNull(expOutput);
    	Preconditions.checkArgument(expOutput.length == topology[topology.length-1], String.format("Invalid Expected Output: %s", Arrays.toString(expOutput)));
    	Preconditions.checkArgument(trainTimes >= 1, String.format("Invalid trainTimes: %s", trainTimes));
    	Preconditions.checkArgument(maxRMSE == null || maxRMSE > 0, String.format("Invalid maxRMSE param: %s", maxRMSE));
        
        float[] input = this._convertInput(n);
        Preconditions.checkArgument(input.length == topology[0], String.format("Input size: {0}, expected size:{1}", input.length, topology[0]));
        Preconditions.checkArgument(expOutput.length == topology[topology.length-1], String.format("Output size: {0}, expected size:{1}", expOutput.length, expOutput.length));
        
        LOG.log(Level.INFO, "Training for {0} users {1} times with input:{2} and output:{3}", new Object[]{n, trainTimes, Arrays.toString(input), Arrays.toString(expOutput)});
        
        if(revert) {
        	LOG.log(Level.FINE, "Saving FANN's state to {0}", bufferFile);
        	fann_save(ann, bufferFile);
        }
        
        for (int i = 0; i < trainTimes; i++) {
			float rmse = rmse(n, expOutput);
			if ((maxRMSE != null) && (rmse < maxRMSE)) {
                break;
			}
			fann_train(ann, input, expOutput);
		}

        float result = rmse(n, expOutput);
        
        if(revert) {
        	LOG.log(Level.FINE, "Restoring FANN's state from {0}", bufferFile);
            ann = fann_create_sparse_array(connection_rate, topology.length, topology);
            ann = fann_create_from_file(bufferFile);
        }
        
        return result;
    }
	
    /**
     * Computes the RMSE for the given number of users and expected output.
     * @param n: Number of users. Must not be None. Must be non-negative integer. 
     * @param expOutput: The expected output from the NN. Must be of appropriate size. Must not be None.
     * @return: the RMSE for the given number of users and expected output.
     */
	private float rmse(int n, float[] expOutput) {
		Preconditions.checkArgument(n > 0, String.format("Invalid n: %s", n));
		Preconditions.checkNotNull(expOutput);
		Preconditions.checkArgument(expOutput.length == topology[topology.length - 1], String.format("Invalid Expected Output: %s", Arrays.toString(expOutput)));
		
        return Util.rmse(run(n), expOutput);
	}

	/**
	 * Runs the underlying neural network.
	 * @param n: Number of users. Must not be None. Must be non-negative integer.
	 * @return: The output layer as a colleciton.
	 */
	private float[] run(int n) {
		Preconditions.checkArgument(n >= 0, String.format("Invalid n: %s", n));
        return fann_run(ann, _convertInput(n));
	}

	private float[] _convertInput(int n) {
		// TODO Auto-generated method stub
		return null;
	}

	public static void main(String[] args) {
		FANNWrapper fw = new FANNWrapper();
		LOG.log(Level.WARNING, "{0}", 5);
	}

	//// ========================================== Native API ==========================================
	////=================================================================================================
	protected static native Pointer fann_create_standard_array(int numLayers, int[] layers);

	protected static native Pointer fann_create_sparse_array(float connection_rate, int numLayers, int[] layers);

	protected static native Pointer fann_create_shortcut_array(int numLayers, int[] layers);

	protected static native float fann_get_MSE(Pointer ann);

	protected static native float[] fann_run(Pointer ann, float[] input);

	protected static native void fann_destroy(Pointer ann);

	protected static native int fann_get_num_input(Pointer ann);

	protected static native int fann_get_num_output(Pointer ann);

	protected static native int fann_get_total_neurons(Pointer ann);

	protected static native void fann_set_activation_function(Pointer ann, int activation_function, int layer, int neuron);

	protected static native void fann_set_activation_function_hidden(Pointer ann, int activation_function);
	
	protected static native void fann_set_activation_function_output(Pointer ann, int activation_function);
	
	
	protected static native void fann_set_activation_steepness(Pointer ann, float steepness, int layer, int neuron);

	protected static native Pointer fann_get_neuron(Pointer ann, int layer, int neuron);

	protected static native Pointer fann_create_from_file(String configuration_file);

	protected static native int fann_save(Pointer ann, String file);
	
	protected static native void fann_set_learning_rate(Pointer ann, float learning_rate);
	
	protected static native float fann_get_learning_rate(float learning_rate);
	
	protected static native void fann_set_learning_momentum(Pointer ann, float learning_momentum);
	
	protected static native float fann_get_learning_momentum(float learning_momentum);
	
	protected static native void fann_set_training_algorithm(Pointer ann, int training_algorithm);
	
	protected static native int fann_get_training_algorithm(Pointer ann);
	
	protected static native void fann_randomize_weights(Pointer ann, float min_weight, float max_weight);
	
	protected static native void fann_train(Pointer ann, float[] input, float[]	desired_output);
	
}
