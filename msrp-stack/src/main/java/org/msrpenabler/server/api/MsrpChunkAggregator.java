package org.msrpenabler.server.api;

import java.util.Iterator;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;

public interface MsrpChunkAggregator extends ByteBufHolder {


	public boolean isComplete();
	
	public String getMessageId() ;

	public String getContentType() ;

	public int getContentLength() ;
	
	public int getWaitingLength();
	
	public ByteBuf content();
	
	public String toString();
	
	public Iterator<MsrpChunkData> iterator();
	
}
