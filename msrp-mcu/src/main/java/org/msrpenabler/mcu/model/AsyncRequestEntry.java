/**
 * 
 */
package org.msrpenabler.mcu.model;

import org.enabler.restlib.handlers.UserResponseHandler;
import org.enabler.restlib.handlers.RestFutureHandler;
import org.enabler.restlib.wrapper.RestStubFullHttpRequest;

/**
 * @author Julien Bellanger
 *
 */
public class AsyncRequestEntry {

	final public RestStubFullHttpRequest req;
	final public UserResponseHandler resHd;
	final public RestFutureHandler futResult;
		
	public AsyncRequestEntry(RestStubFullHttpRequest request, RestFutureHandler future) {
		this.req = request;
		this.resHd = new EmptyRespCBHandler(this.req);
		this.futResult = future;
	}
	
}
