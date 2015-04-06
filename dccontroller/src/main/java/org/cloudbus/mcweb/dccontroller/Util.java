package org.cloudbus.mcweb.dccontroller;

import com.google.common.base.Preconditions;

public final class Util {

	public static float rmse(float[] actual, float[] expeccted){
		Preconditions.checkNotNull(actual);
		Preconditions.checkNotNull(expeccted);
		Preconditions.checkArgument(actual.length == expeccted.length);
		
		float sumSquareDiff = 0;
		for (int i = 0; i < expeccted.length; i++) {
			sumSquareDiff += Math.pow(actual[i] - expeccted[i], 2);
		}
		float mseRun = sumSquareDiff /  (float)expeccted.length;
		return (float)Math.sqrt(mseRun);
	}
		
	
	
}
