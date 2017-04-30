package org.msrpenabler.server.exception;

public class SessionHdException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -979567913697660266L;
	

	public SessionHdException(String message) {
		super(message);
	}

	public SessionHdException(String message, Throwable cause) {
		super(message, cause);
	}

	public SessionHdException(Throwable cause) {
		super(cause);
	}

}
