package org.enabler.restlib.wrapper;

import io.netty.handler.codec.http.HttpMessage;

public interface RestStubHttpStreamId extends HttpMessage {
	
	public String getStreamId();
	
	public void setStreamId(String id);
	
}
