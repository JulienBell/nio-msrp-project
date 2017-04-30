package org.enabler.restlib.handlers;

import org.enabler.restlib.RestHttpException;
import org.enabler.restlib.wrapper.RestStubFullHttpResponse;



/**
 * 
 * @author Julien
 *
 *	Result interface return to user 
 *
 */
public interface RestFutureResult {

	// Future status
	public boolean isComplete();

	public boolean isSuccess();
	
	public RestStubFullHttpResponse getResult();
	
	public RestStubFullHttpResponse waitResult(long millisec) throws RestHttpException, InterruptedException;
	
}
