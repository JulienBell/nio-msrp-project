package org.msrpenabler.server.api.internal;

import org.msrpenabler.server.net.MsrpMessageWrapper;
import org.msrpenabler.server.util.GenerateIds;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class MsrpMessageDataImpl extends MsrpMessageWrapper {
	
	/**
	 * 
	 * @param messageId
	 * @param length
	 * @param contentType
	 * @param rawContent
	 */
	public MsrpMessageDataImpl(String messageId, String contentType,
			ByteBuf rawContent) {
		super.messageId = messageId;
		super.continuationFlag='$';
		super.setContentType(contentType);
		super.setContent(rawContent);
		super.setContentLength();
	}

	/**
	 * 
	 * @param contentType
	 * @param rawContent
	 */
	public MsrpMessageDataImpl(String contentType,
			ByteBuf rawContent) {
		super.messageId = GenerateIds.createMessageId();
		super.continuationFlag='$';
		super.setContentType(contentType);
		super.setContent(rawContent);
		super.setContentLength();
	}
	
	/**
	 * Create empty MsrpContentType
	 */
	public MsrpMessageDataImpl() {
		this.messageId = GenerateIds.createMessageId();
		super.continuationFlag='$';
		super.setContent(null);
		super.setContentLength();
	}
	
	public MsrpMessageDataImpl(String contentType, byte[] rawContent) {
		super.messageId = GenerateIds.createMessageId();
		super.continuationFlag='$';
		super.setContentType(contentType);
		this.setContent( Unpooled.wrappedBuffer(rawContent) );
		super.setContentLength();
	}


}
