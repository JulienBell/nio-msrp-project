package org.enabler.restlib.wrapper;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;


public class RestStubFullHttpResponse extends DefaultFullHttpResponse implements RestStubHttpStreamId {

	String streamId;
	
	public RestStubFullHttpResponse(HttpVersion version, HttpResponseStatus status, String contentType, ByteBuf content) {
		super(version, status, content);

		initCommonHeaders();
		
		int lenght = content != null ? content.readableBytes() : 0;
		
        headers().set(HttpHeaders.Names.CONTENT_LENGTH , Integer.toString(lenght) );
        if (contentType != null) {
        	headers().set(HttpHeaders.Names.CONTENT_TYPE, contentType);
        }
	}

	public RestStubFullHttpResponse(HttpVersion version, HttpResponseStatus status) {
		super(version, status);
		
		initCommonHeaders();
        headers().set(HttpHeaders.Names.CONTENT_LENGTH , 0 );
	}

	private void initCommonHeaders() {
        headers().set(HttpHeaders.Names.CONNECTION, "Keep-Alive");
        headers().set("Keep-Alive", "timeout=15, max=500");
	}
	
    public RestStubFullHttpResponse(FullHttpResponse msg, String streamId) {
		super(msg.getProtocolVersion(), msg.getStatus(), msg.content());
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
