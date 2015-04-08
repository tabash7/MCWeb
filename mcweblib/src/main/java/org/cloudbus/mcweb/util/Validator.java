package org.cloudbus.mcweb.util;

import com.google.common.base.Preconditions;

/**
 * Used to assert invariants in the code, similarly to {@link Preconditions}.
 * 
 * @author nikolay.grozev
 *
 */
public class Validator {

	/**
	 * Checks the condition, and if false throws an {@link IllegalStateException} with the provided message.
	 * @param condition - the condition to check;
	 */
	public static void check(boolean condition) {
		check(condition, "Invalid state");
	}

	/**
	 * Throw a {@link NullPointerException} if the object is null.
	 * @param o - the object to check;
	 */
	public static void notNull(Object o) {
		if(o == null) {
			throw new NullPointerException("Object is null");
		}
	}
	
	/**
	 * Checks the condition, and if false throws an {@link IllegalStateException} with the provided message.
	 * @param condition - the condition to check;
	 * @param message - the message for the potential exception
	 */
	public static void check(boolean condition, String message) {
		if(!condition) {
			throw new IllegalStateException(message);
		}
	}
	
}
