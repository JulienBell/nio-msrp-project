package org.msrpenabler.server.api.internal;

import io.netty.buffer.ByteBuf;

import org.msrpenabler.server.net.MsrpMessageWrapper;

public class MsrpChunkDataImpl extends MsrpMessageWrapper  {

        		
	/**
	 * 
	 * @param messageId
	 * @param contentType
	 * @param rawContent
	 * @param startByteRange
	 * @param lengthByteRange (-1 if unknown)
	 * @param endByteRange (-1 if unknown)
	 */
	public MsrpChunkDataImpl(String messageId, String contentType,
			ByteBuf rawContent, int startByteRange, int lengthByteRange, int endByteRange) {
		
		init(messageId, contentType, rawContent, startByteRange, lengthByteRange, endByteRange, false);
	}
		
	/**
	 * 
	 * @param messageId
	 * @param contentType
	 * @param rawContent
	 * @param startByteRange
	 * @param lengthByteRange (-1 if unknown)
	 * @param endByteRange (-1 if unknown)
	 * @param isLastChunck true if last chunk to send if endByteRange = -1 
	 */
	public MsrpChunkDataImpl(String messageId, String contentType,
				ByteBuf rawContent, int startByteRange, int lengthByteRange, int endByteRange,
				boolean isLastChunck) {
		
		init(messageId, contentType, rawContent, startByteRange, lengthByteRange, endByteRange, isLastChunck);

	}

	public void init(String messageId, String contentType,
			ByteBuf rawContent, int startByteRange, int lengthByteRange, int endByteRange,
			boolean isLastChunck) {
		
		super.messageId = messageId;
		
		super.byteRangeStart = Integer.toString(startByteRange);
		if (lengthByteRange >=0 ) {
			super.byteRangeLength = Integer.toString(lengthByteRange);
		}
		else {
			super.byteRangeLength = "*";
		}
		if (endByteRange >=0 ) {
			super.byteRangeEnd = Integer.toString(endByteRange);
		}
		else {
			super.byteRangeLength = "*";
		}
		super.hasByteRange = true;
		
		super.setContentType(contentType);
		super.setContent(rawContent);
		super.setContentLength();

		if ( (isLastChunck && byteRangeLength.equals("*") )|| 
				(byteRangeLength.equals(byteRangeEnd) && ! byteRangeLength.equals("*")) ) {
			super.continuationFlag='$';
		}
		else {
			super.continuationFlag='+';
		}

	}	
	
		
}
