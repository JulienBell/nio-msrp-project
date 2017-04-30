/**
 * 
 */
package org.enabler.restlib.handlers;

import org.enabler.restlib.RestHttpException;
import org.enabler.restlib.wrapper.RestStubFullHttpResponse;


/**
 * @author Julien Bellanger
 *
 *
 *  Writable RestFuturResult
 *
 */
public class RestFutureHandler implements RestFutureResult {
	
	protected volatile boolean done=false;
	protected volatile boolean success=false;
	protected volatile RestStubFullHttpResponse result = null;
	protected volatile int streamId=0;
	
	public int getStreamId() {
		return streamId;
	}
	
	public void setStreamId(int streamId) {
		this.streamId= streamId; 
	}
	
	public void setResult(RestStubFullHttpResponse result) {
		this.result = result;
	}
	
	private synchronized void setDone() {
		done=true;
		notifyAll();
	}
	
	public void setSuccess() {
		success=true;
		setDone();
	}

	public void setFailure() {
		setDone();
	}
	
	@Override
	public boolean isComplete() {
		return done;
	}
	@Override
	public boolean isSuccess() {
		return success;
	}
	
	@Override
	public  RestStubFullHttpResponse getResult() {

		if (done) {
			return result;
		}
		
		return null;
	}
	
	@Override
	public synchronized RestStubFullHttpResponse waitResult(long millisec) throws InterruptedException, RestHttpException {
		
		wait(millisec);

		if ( !done ) {
			throw new RestHttpException("Wait timeout for expected response");
		}

		return getResult();
	}
	
}
