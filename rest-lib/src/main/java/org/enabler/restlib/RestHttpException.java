package org.enabler.restlib;

public class RestHttpException extends Exception {


	/**
	 * 
	 */
	private static final long serialVersionUID = -8263652637747879103L;

	public RestHttpException(String message) {
		super(message);
	}

	public RestHttpException(String message, Throwable cause) {
		super(message, cause);
	}

	public RestHttpException(Throwable cause) {
		super(cause);
	}

}
