package org.rest.stub.sply.api;


import org.enabler.restlib.RestHttpConnector;
import org.enabler.restlib.RestHttpServlet;
import org.enabler.restlib.wrapper.RestStubFullHttpRequest;
import org.enabler.restlib.wrapper.RestStubFullHttpResponse;

import io.netty.channel.Channel;

public abstract class SplyHttpConnector  extends RestHttpConnector {

	
	public SplyHttpConnector(RestHttpServlet requestServlet, boolean isServer, Channel ch) {
		// set notifier
		super(requestServlet, isServer, ch);
	}

	
	@Override
	protected void setStreamIdOnRequest(int streamId, RestStubFullHttpRequest request) {
		
		request.setStreamId(Integer.toString(streamId));
		
	}

	@Override
	protected void setStreamIdOnResponse(int streamId, RestStubFullHttpResponse response) {
		
		response.setStreamId(Integer.toString(streamId));
		
	}
	    
}
	
