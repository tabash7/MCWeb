package org.cloudbus.mcweb.dccontroller;

public enum TrainingAlgo {
	/* The order these appear must match the order in the FANN src! */
	FANN_TRAIN_INCREMENTAL, 
	FANN_TRAIN_BATCH, 
	FANN_TRAIN_RPROP, 
	FANN_TRAIN_QUICKPROP;

}
