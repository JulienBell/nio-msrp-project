package org.enabler.restlib.handlers;


import org.enabler.restlib.RequestFailureReason;
import org.enabler.restlib.wrapper.RestStubFullHttpResponse;


public interface UserResponseHandler {

	
	/**
	 * User Call Back implementation
	 * 
	 * Response content buffer will be deallocated on the CB return
	 *  
	 * @param resp
	 */
	public void onResponseReceivedCB(RestStubFullHttpResponse resp) ;

	public void onRequestFailureCB(RequestFailureReason reason) ;
	
	public void onUnexpectedExceptionCB(Throwable cause) ;

	
}
