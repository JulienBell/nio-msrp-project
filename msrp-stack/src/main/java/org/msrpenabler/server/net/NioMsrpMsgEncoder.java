package org.msrpenabler.server.net;

import java.nio.charset.Charset;
import java.util.Map.Entry;

import org.msrpenabler.server.exception.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class NioMsrpMsgEncoder extends MessageToByteEncoder<MsrpMessageWrapper> {

	private static final Logger loggerMsg =
	        LoggerFactory.getLogger("org.msrp.dump.encode");
	
	private static final byte CR = '\r';
	private static final byte LF = '\n';
	
	private static final Charset utf8 = Charset.forName("UTF-8");

	/**
	 * As MsrpMessageWrapper implement ByteBufHolder, its content will be release after this call by Netty 
	 */
	@Override
	protected void encode(ChannelHandlerContext ctx, MsrpMessageWrapper msg, ByteBuf out) throws Exception {

		String lineToAdd;
		
		switch (msg.msgType) {

		case MSRP_RESPONSE:
			msg.firstLine = "MSRP " + msg.transactionId + " " + msg.statusCode + " " + msg.commentRsp;
			msg.endLine = "-------" + msg.transactionId + "$";
			break;
			
		case MSRP_SEND:
			msg.firstLine = "MSRP " + msg.transactionId + " SEND";
			msg.endLine = "-------" + msg.transactionId + msg.continuationFlag;
			break;

		case MSRP_REPORT:
			msg.firstLine = "MSRP " + msg.transactionId + " REPORT";
			msg.endLine = "-------" + msg.transactionId + "$";
			break;
			
		case MSRP_UNDEF:
		default:
			throw new TransactionException("Unknown message type to encode "+ msg.msgType);
		}
		
		writeAndAddCRLF(out, msg.firstLine);

		writeAndAddCRLF(out, "To-Path: " + msg.toPath);
		writeAndAddCRLF(out, "From-Path: " + msg.fromPath);

        for (Entry<String, String> headerEntry : msg.headersMap.entrySet()) {

            lineToAdd = headerEntry.getKey() + ": " + headerEntry.getValue();

            writeAndAddCRLF(out, lineToAdd);
        }
		
		if (msg.hasByteRange && msg.byteRangeStart != null && msg.byteRangeLength != null && msg.byteRangeEnd != null) {
			lineToAdd = "Byte-Range: " + msg.byteRangeStart + "-" + msg.byteRangeLength + "/" + msg.byteRangeEnd;
		
			writeAndAddCRLF(out, lineToAdd); 
		}
		
		
		if (msg.contentType != null) {

			lineToAdd = "Message-ID: "+ msg.messageId;
			writeAndAddCRLF(out, lineToAdd);
			
			lineToAdd = "Content-Type: "+ msg.contentType;
			writeAndAddCRLF(out, lineToAdd);
			writeCRLF(out);
			
			// TODO Here we should check the content doesn't match with the endline TID
			//  if it's the case each end-line like should be returned here
			//  and we have to choose a TID that doesn't match any of these end-line like 
			loggerMsg.info("");
			
			if (msg.content() != null) {
				writeAndAddCRLF(out, msg.content());
			}

		}
		
		writeAndAddCRLF(out, msg.endLine);
		
		ctx.flush();
	}
		
		
	private static void writeAndAddCRLF(ByteBuf out, ByteBuf byteLine ) {
		
		if (loggerMsg.isInfoEnabled()) {
		
			if (byteLine.hasArray()) {
				loggerMsg.info(byteLine.toString(Charset.forName("UTF-8")));
			}
			else if (byteLine.unwrap() != null) {
				loggerMsg.info(byteLine.unwrap().toString(Charset.forName("UTF-8")));
			}
			else {
				loggerMsg.info(byteLine.toString(Charset.forName("UTF-8")));
			}
		}
		
		out.writeBytes(byteLine);
		writeCRLF(out);
	}

	private static void writeAndAddCRLF(ByteBuf out, String str ) {
		loggerMsg.info(str);
		out.writeBytes(str.getBytes(utf8));
		writeCRLF(out);
	}
	
	private static void writeCRLF(ByteBuf out) {
		out.writeByte(CR);
		out.writeByte(LF);
	}
	
}
