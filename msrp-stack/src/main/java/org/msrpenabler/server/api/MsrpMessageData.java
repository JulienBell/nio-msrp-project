package org.msrpenabler.server.api;

import io.netty.buffer.ByteBufHolder;

public interface MsrpMessageData extends ByteBufHolder {
	

	/**
	 * Return the Message-Id value 
	 * @return
	 */
	public String getMessageId() ;
	

	/**
	 * Return the Content-Type value
	 * @return
	 */
	public String getContentType();

	
	/**
	 * This method release the buffer ??	
	 * It is useful for end point service to retrieve copy of content Data
	 * A relay should prefer to call {@link MsrpMessageData#content()} to avoid buffer copy
	 * 
	 * @return
	 */
	public byte[] getContentByte() ;
	
	/**
	 * 
	 * @return
	 */
	public int getContentLength();

	
	/**
	 * 
	 * @return
	 */
	public String toString();


	/**
	 * 
	 * @return
	 */
	public boolean isChunkPartMsg();
	

	
//	/**
//	 * Release the current Message. Data are no more available after this call
//	 */
//	public void release();
//
//	/**
//	 * Increment retain cpt on the current Message. Data will not really release if cpt > 1
//	 */
//	public void retain();
//
//
//	public MsrpMessageData duplicate() throws SessionHdException;	
	
}
