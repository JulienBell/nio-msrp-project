package org.enabler.restlib.handlers;


import org.enabler.restlib.RequestFailureReason;
import org.enabler.restlib.wrapper.RestStubFullHttpResponse;


/**
 * 		  
 * @author Julien
 *
 *	Allow to hold a user response Handler and FutureResult Handler into a generic response Handler 
 *
 *
 */
public class RestAsyncResponseMgnt extends RestResponseHandler {
	
	final UserResponseHandler respHandler;
	final RestFutureHandler futureResult;
	
	public RestAsyncResponseMgnt(UserResponseHandler respHandler, 
										RestFutureHandler futureResult ) {
		this.respHandler = respHandler;
		this.futureResult = futureResult;
	}
	public RestAsyncResponseMgnt(UserResponseHandler respHandler ) {
		this.respHandler = respHandler;
		this.futureResult = null;
	}

	
	@Override
	public void complete(RestStubFullHttpResponse resp) {
		if (futureResult != null)  {
			futureResult.setResult((RestStubFullHttpResponse) resp);
			futureResult.setSuccess();
		}

		respHandler.onResponseReceivedCB((RestStubFullHttpResponse) resp);
	}

	@Override
	public void failure( RequestFailureReason reason ) {

		if (futureResult != null)  {
			futureResult.setFailure();
		}

		respHandler.onRequestFailureCB(reason);
	}

	@Override
	public void exception(Throwable cause) {

		if (futureResult != null)  {
			futureResult.setFailure();
		}

		respHandler.onUnexpectedExceptionCB(cause);
	}

}
