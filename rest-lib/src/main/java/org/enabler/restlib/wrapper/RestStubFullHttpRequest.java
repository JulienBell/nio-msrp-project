package org.enabler.restlib.wrapper;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

public class RestStubFullHttpRequest extends DefaultFullHttpRequest implements RestStubHttpStreamId {

	private String streamId;
	
	public RestStubFullHttpRequest(HttpVersion httpVersion, HttpMethod method, String uri) {
		super(httpVersion, method, uri);
	}

	public RestStubFullHttpRequest(HttpVersion httpVersion, HttpMethod method,	String uri, 
									String contentType, ByteBuf content) {
		super(httpVersion, method, uri, content);
		
		int lenght = content != null ? content.readableBytes() : 0;
		
        headers().set(HttpHeaders.Names.CONTENT_LENGTH , Integer.toString(lenght) );
        if (contentType != null) {
        	headers().set(HttpHeaders.Names.CONTENT_TYPE, contentType);
        }
        headers().set(HttpHeaders.Names.CONNECTION, "Keep-Alive");
        headers().set("Keep-Alive", "timeout=15, max=500");
	}
	
    public RestStubFullHttpRequest(FullHttpRequest msg, String streamId) {
		super(msg.getProtocolVersion(), msg.getMethod(), msg.getUri(), msg.content());

        headers().set(msg.headers());
        trailingHeaders().set(msg.trailingHeaders());
        this.streamId = streamId;
    }
	    

	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String id) {
		streamId = id;
	}

}
