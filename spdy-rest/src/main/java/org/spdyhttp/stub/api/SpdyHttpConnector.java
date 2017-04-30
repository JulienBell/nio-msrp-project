package org.spdyhttp.stub.api;


import org.enabler.restlib.RestHttpConnector;
import org.enabler.restlib.RestHttpServlet;
import org.enabler.restlib.wrapper.RestStubFullHttpRequest;
import org.enabler.restlib.wrapper.RestStubFullHttpResponse;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.spdy.SpdyHttpHeaders;

public abstract class SpdyHttpConnector  extends RestHttpConnector {

	public SpdyHttpConnector(RestHttpServlet requestServlet, boolean isServer, Channel ch) {
		
		// set notifier
		super(requestServlet, isServer, ch);
	}


	/**
	 *	Stream Id is define by the super class, the method to set it on the request depend on client type 
	 */
	@Override
	protected void setStreamIdOnRequest(int streamId, RestStubFullHttpRequest request) {
		
		HttpHeaders.addIntHeader(request, SpdyHttpHeaders.Names.STREAM_ID, streamId);
		HttpHeaders.addIntHeader(request, SpdyHttpHeaders.Names.PRIORITY, 3);
		
		request.setStreamId( Integer.toString(streamId) );
	}
	

	@Override
	protected void setStreamIdOnResponse(int streamId, RestStubFullHttpResponse response) {
		
		HttpHeaders.addIntHeader(response, SpdyHttpHeaders.Names.STREAM_ID, streamId);
		HttpHeaders.addIntHeader(response, SpdyHttpHeaders.Names.PRIORITY, 3);

		response.setStreamId(Integer.toString(streamId));
		
	}
    
	
}
	
