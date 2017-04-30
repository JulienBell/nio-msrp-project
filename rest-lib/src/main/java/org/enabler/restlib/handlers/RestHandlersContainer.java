package org.enabler.restlib.handlers;

import org.enabler.restlib.wrapper.RestStubFullHttpRequest;

/**
 * 
 * @author Julien
 *
 *
 *  Hold request and associated response Handler and future result
 *  Futur result is not mandatory 
 *
 */
public class RestHandlersContainer {

	final public RestStubFullHttpRequest request;
	final public RestResponseHandler	respHandler;
	final public RestFutureHandler futur;
	
	public RestHandlersContainer(RestStubFullHttpRequest request,
			RestResponseHandler respHandler, RestFutureHandler futur) {
		this.request = request;
		this.respHandler = respHandler;
		this.futur = futur;
	}

	public RestHandlersContainer(RestStubFullHttpRequest request,
			RestResponseHandler respHandler) {
		
		this(request, respHandler, null);
	}
	
}
