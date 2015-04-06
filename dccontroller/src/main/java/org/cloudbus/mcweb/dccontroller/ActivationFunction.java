package org.cloudbus.mcweb.dccontroller;

public enum ActivationFunction {

    /* The order these appear must match the order in the FANN src! */
    FANN_LINEAR, 
    FANN_THRESHOLD, 
    FANN_THRESHOLD_SYMMETRIC, 
    FANN_SIGMOID, 
    FANN_SIGMOID_STEPWISE,
    FANN_SIGMOID_SYMMETRIC, 
    FANN_SIGMOID_SYMMETRIC_STEPWISE, 
    FANN_GAUSSIAN, 
    FANN_GAUSSIAN_SYMMETRIC,
    /*
     * Stepwise linear approximation to gaussian. Faster than gaussian but a bit
     * less precise. NOT implemented yet.
     */
    FANN_GAUSSIAN_STEPWISE, 
    FANN_ELLIOT, 
    FANN_ELLIOT_SYMMETRIC, 
    FANN_LINEAR_PIECE,
    FANN_LINEAR_PIECE_SYMMETRIC, 
    FANN_SIN_SYMMETRIC, 
    FANN_COS_SYMMETRIC, 
    FANN_SIN, 
    FANN_COS
}