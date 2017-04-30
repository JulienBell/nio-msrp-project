package org.enabler.restlib.handlers;


import org.enabler.restlib.RequestFailureReason;
import org.enabler.restlib.wrapper.RestStubFullHttpResponse;

/**
 * 
 * @author Julien
 *
 */
public abstract class RestResponseHandler {

	protected volatile boolean done=false;
	protected volatile RestStubFullHttpResponse response = null;
	protected Integer streamId;
	
	public final Integer getStreamId() {
		return streamId;
	}

	public final void setStreamId(int stream) {
		streamId = stream;
	} 

	
	public abstract void complete(RestStubFullHttpResponse msg) ;

	public abstract void failure(RequestFailureReason socketclosed) ;

	public abstract void exception(Throwable cause);
	
}
