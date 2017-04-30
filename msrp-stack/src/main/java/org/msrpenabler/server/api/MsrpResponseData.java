package org.msrpenabler.server.api;

public interface MsrpResponseData {

	/**
	 * Return the Message-Id value 
	 * @return
	 */
	public String getMessageId() ;

	/**
	 * Return the response Status
	 * @return
	 */
	public String getStatus() ;

	/**
	 * Return the initial Chunck sent associated to this response (null if resp associated to a complete Msg)
	 * @return
	 */
	public MsrpChunkData getAssociatedChunkMsgData();

	/**
	 * Return the initial Msg sent associated to this response (null if resp associated to a Chunk)
	 * @return
	 */
	public MsrpMessageData getAssociatedMessageData();

	
}
