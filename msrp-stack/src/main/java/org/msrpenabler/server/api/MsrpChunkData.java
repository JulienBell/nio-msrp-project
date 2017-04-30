package org.msrpenabler.server.api;

public interface MsrpChunkData extends MsrpMessageData {

	public String getMessageId() ;

	public int getByteStartRange();

	public int getByteEndRange();

	public char getContinuationFlag();
	
	public String toString();

//	public int refCnt();
	
}
