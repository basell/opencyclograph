package com.touchgraph.graphlayout;

public class DontUseException extends Exception {
	/**
	 * Constructs a new exception with <code>null</code> as its detail message.
	 * The cause is not initialized, and may subsequently be initialized by a
	 * call to {@link #initCause}.
	 */
	public DontUseException() {
		super();
	}

	/**
	 * Constructs a new exception with the specified detail message.  The
	 * cause is not initialized, and may subsequently be initialized by
	 * a call to {@link #initCause}.
	 *
	 * @param   message   the detail message. The detail message is saved for 
	 *          later retrieval by the {@link #getMessage()} method.
	 */
	public DontUseException(String message) {
		super(message);
	}

}
