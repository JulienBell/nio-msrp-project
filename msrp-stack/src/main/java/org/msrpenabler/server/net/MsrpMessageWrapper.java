package org.msrpenabler.server.net;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.msrpenabler.server.api.MsrpChunkData;
import org.msrpenabler.server.api.MsrpMessageData;
import org.msrpenabler.server.api.MsrpResponseData;
import org.msrpenabler.server.api.MsrpReportData;
import org.msrpenabler.server.api.internal.MsrpMsgTypeEnum;
import org.msrpenabler.server.exception.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;


public class MsrpMessageWrapper implements MsrpMessageData, MsrpChunkData, MsrpResponseData {


	private static final Logger logger = LoggerFactory.getLogger( MsrpMessageWrapper.class);
	
	protected MsrpMsgTypeEnum msgType = MsrpMsgTypeEnum.MSRP_UNDEF;
	
	protected String	firstLine=null;

	protected String 	transactionId=null;
	protected String 	cmdMSRP=null;
	protected String 	statusCode=null;
	protected String 	commentRsp=null;
	
	protected String	toPath=null;
	protected String	toPathSessionId=null;
	protected String	fromPath=null;
	protected String	fromPathSessionId=null;
	
	protected boolean   hasByteRange=false;
	protected String	byteRangeStart=null;
	protected String	byteRangeEnd=null;
	protected String	byteRangeLength=null;

	protected Map<String,String> headersMap = new HashMap<String, String>();
	
	
	protected String	contentType=null;
	
	// To should be read has a Slice and retain() // But when release it surely ???
	protected ByteBuf rawContent=null;

	private int contentLength=0;
	
	
	public String endLine=null;
	public String endLineTid=null;

	public char continuationFlag='0';

	// may be on response treatment 
	protected MsrpMessageData associatedMsg=null;
	
	
	public String getFirstLine() {
		return firstLine;
	}

	public void setFirstLine(String firstLine) {
		this.firstLine = firstLine;
	}

	protected String messageId=null;

	
	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public MsrpMessageWrapper( MsrpMessageData msgSend ) {

		msgType = MsrpMsgTypeEnum.MSRP_SEND;

		contentType = msgSend.getContentType();
		rawContent = msgSend.content().duplicate();

		logger.info("Create msg copy, isChunk {}", msgSend.isChunkPartMsg());
		
		if (msgSend.isChunkPartMsg()) {
			
			hasByteRange = true;

			byteRangeEnd = Integer.toString( ((MsrpChunkData) msgSend).getByteEndRange());
			byteRangeStart = Integer.toString( ((MsrpChunkData) msgSend).getByteStartRange());
			byteRangeLength = Integer.toString(msgSend.getContentLength());
			continuationFlag = ((MsrpChunkData) msgSend).getContinuationFlag();

			contentLength = rawContent.readableBytes();
		}

		else {

			// Content non vide
			if (contentType != null && rawContent != null) {
				hasByteRange = true;
				byteRangeStart = "1";

				contentLength = rawContent.readableBytes();

				byteRangeLength = Integer.toString(contentLength);
				byteRangeEnd = this.byteRangeLength;
				continuationFlag= '$';
			}
			else {
				hasByteRange = false;
				continuationFlag= '$';
			}
		}			
		messageId = msgSend.getMessageId();
	}


		
	public MsrpMessageWrapper() {

	}


	public String getContentType() {
		
		return contentType;
	}
	
	public char getContinuationFlag() {
		return continuationFlag;
	}




	public void setContentType(String type) {
		
		contentType = type;
	}

	protected void setContent(ByteBuf contentBuff) {

		this.rawContent = contentBuff;
	}

	protected void appendContent(ByteBuf byteLine) {
		
		if (rawContent == null) {
			rawContent = Unpooled.buffer();
		}
		rawContent.writeBytes(byteLine);
	}

	public void cleanContentCRLF() {
		// rawContent may be null
		if (rawContent != null) {
			int writeId = rawContent.readableBytes();
			rawContent.writerIndex(writeId-2);
		}
	}
	public void appendContentCRLF() {
		rawContent.writeByte('\r');
		rawContent.writeByte('\n');
	}

	public MsrpMessageData getMessageData() {
		
		if ( msgType != MsrpMsgTypeEnum.MSRP_SEND || messageId == null || contentType == null || rawContent == null) {
			return null;
		}

		return this ;
	}

	public boolean isChunkPartMsg() {

		return ( msgType == MsrpMsgTypeEnum.MSRP_SEND && messageId != null && contentType != null
				&& ( continuationFlag == '+' ||
					 (continuationFlag == '$' && hasByteRange && Integer.parseInt(byteRangeStart) > 1 ) ) ) ;
	}
	
	public MsrpChunkData getChunkData() {
		
		if ( ! isChunkPartMsg() ) {
			return null;
		}

		return this;
	}
	
	
	public MsrpReportData getReportData() throws TransactionException {

		if (msgType != MsrpMsgTypeEnum.MSRP_REPORT) {
			return null;
		}
		
		// TODO -- 
		//return this;
        throw new TransactionException("Not implemented");

	}

	public MsrpResponseData getResponseData() {

		if (msgType != MsrpMsgTypeEnum.MSRP_RESPONSE) {
			return null;
		}
		return this;
	}

	
	
	public MsrpMessageWrapper generateResponseMsg(String statusCode, String comment) {
		
		MsrpMessageWrapper resp = new MsrpMessageWrapper();

		resp.msgType = MsrpMsgTypeEnum.MSRP_RESPONSE;
		resp.statusCode = statusCode;
		resp.commentRsp = comment;

		resp.toPath =  fromPath;
		resp.fromPath =  toPath;
		resp.transactionId = transactionId;
		
		return resp;
	
	}


	public int getContentLength() {
		return contentLength;
	}

	public void setContentLength() {
		if (rawContent == null) {
			contentLength = 0;
		}
		else {
			contentLength = rawContent.readableBytes();
		}
	}

	
	@Override
	public byte[] getContentByte() {
		// TODO Do we return a new byte[] or a test if hasarray and return the underline array ?
		// test with hasarray or unwrap...
		byte[] c = new byte[getContentLength()];
		content().getBytes(0, c); 
		return c;
	}



	@Override
	public int getByteEndRange() {

		if (hasByteRange) return Integer.valueOf(byteRangeEnd);
		
		return contentLength;
	}


	@Override
	public int getByteStartRange() {
		if (hasByteRange) return Integer.valueOf(byteRangeStart);
		
		return 0;
	}


	@Override
	public String toString() {
		
		if (getContentLength() != 0) {

			if (content().unwrap() != null) {
				return content().unwrap().toString(Charset.forName("UTF-8"));
			}
			else {
				return content().toString(Charset.forName("UTF-8"));
			}
		}
		return "";
	}



	@Override
	public String getStatus() {

		return statusCode;
	}

	@Override
	public MsrpChunkData getAssociatedChunkMsgData() {

		if ( associatedMsg != null && associatedMsg.isChunkPartMsg() ) {
			return (MsrpChunkData) associatedMsg;
		}
		return null;
	}

	@Override
	public MsrpMessageData getAssociatedMessageData() {

		if ( associatedMsg != null && ! associatedMsg.isChunkPartMsg() ) {
			return associatedMsg;
		}
		return null;
	}

	
	public void setAssociatedMessageData(MsrpMessageData msg) {

		associatedMsg = msg;
	}

	
	@Override
	public MsrpMessageData duplicate() {
		
		logger.info("call duplicate on messageId {}", messageId);
		
		return new MsrpMessageWrapper(this);
		
	}

	@Override
	public ByteBuf content() {
		return rawContent;
	}

	@Override
	public MsrpMessageWrapper copy() {
		MsrpMessageWrapper copy = new MsrpMessageWrapper(this);
		
		copy.rawContent = this.rawContent.copy();
		
		return copy;
	}

	@Override
	public MsrpMessageWrapper retain(int increment) {
		if (rawContent != null) {
			rawContent.retain(increment);
		}
		if (logger.isDebugEnabled()) {
			StackTraceElement[] stack = Thread.currentThread().getStackTrace();
			String callingMet="";
			int id=1;
			while (id < stack.length && stack[id].getMethodName().equals("retain")) id++;
			int prof=1;
			while (id < stack.length && prof < 7) {
				callingMet += stack[id].getClassName()+"."+ stack[id].getMethodName()+"("+ stack[id].getLineNumber()+")\n"; 
				prof++;
				id++;
			}
			logger.debug("After call retain msgId {} / star range {}, count {}, type {}, from \n{}", 
					getMessageId(), getByteStartRange(), refCnt(), msgType, callingMet);
		}
		return this;
	}

	@Override
	public boolean release(int decrement) {
		boolean ret=false;
		if (rawContent != null) {
			ret=rawContent.release(decrement);
			if (ret) {
				rawContent = null;
			}
		}

		if (logger.isDebugEnabled()) {
			StackTraceElement[] stack = Thread.currentThread().getStackTrace();
			String callingMet="";
			int id=1;
			while (id < stack.length && stack[id].getMethodName().equals("release")) id++;
			int prof=1;
			while (id < stack.length && prof < 7) {
				callingMet += stack[id].getClassName()+"."+ stack[id].getMethodName()+"("+ stack[id].getLineNumber()+")\n";
				prof++;
				id++;
			}
			logger.debug("After call release msgId {} / star range {}, count {}, type {}, from \n{}", 
										getMessageId(), getByteStartRange(), refCnt(), msgType, callingMet);
		}
		return ret;
	}

	@Override
	public boolean release() {
		return release(1);
	}

	@Override
	public MsrpMessageWrapper retain() {
		MsrpMessageWrapper ret = retain(1);
		return ret;
	}

	@Override
	public int refCnt() {
		if (rawContent != null) {
			return rawContent.refCnt();
		}
		return 0;
	}


}
