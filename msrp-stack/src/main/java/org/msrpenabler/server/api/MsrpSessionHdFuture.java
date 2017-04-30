package org.msrpenabler.server.api;

public interface MsrpSessionHdFuture {

	/**
	 * Allowing to wait on end of operation synchronously
	 * 
	 * @return
	 * @throws InterruptedException
	 */
	public MsrpSessionHdFuture sync() throws InterruptedException;
	
	public boolean isDone();

	public boolean isSuccess();
	
	public Throwable cause();
	
}
