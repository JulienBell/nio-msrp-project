package org.msrpenabler.server.net;

import java.nio.charset.Charset;

import java.util.List;
import java.util.regex.Matcher;

import org.msrpenabler.server.api.internal.MsrpMsgTypeEnum;
import org.msrpenabler.server.util.MsrpSyntaxRegexp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class NioMsrpMsgDecoder extends ByteToMessageDecoder {

	
    private static final Logger logger =
        LoggerFactory.getLogger(NioMsrpMsgDecoder.class);

	private static final Logger loggerMsg =
	        LoggerFactory.getLogger("org.msrp.dump.decode");
    
    
	public enum CodingState {
	    WAIT_FIRST_LINE,
	    WAIT_TO_PATH,
	    WAIT_FROM_PATH,
	    MSRP_HEADERS,
	    
	    WAIT_CONTENT_STUFF,
	    IN_CONTENT_STUFF,
	    IN_CONTENT_STUFF_LENGTH,
	    
	    
	    WAIT_END_LINE,
	    WAIT_END_MSG_ERROR, 
	}	

	private CodingState decodingState = CodingState.WAIT_FIRST_LINE;
	
	private MsrpMessageWrapper decodeMsg;

	private int inStuffLength;
	

    /**
     * Returns the length between reader index and the the end of line found.
     * Returns -1 if no end of line was found in the buffer.
     */
    private static int findEndOfLine(final ByteBuf buffer) {
        final int n = buffer.writerIndex();
        for (int i = buffer.readerIndex(); i < n; i ++) {
            final byte b = buffer.getByte(i);
            if ( (b == '\r') && i < n - 1 && buffer.getByte(i + 1) == '\n' ) {
                return (i - buffer.readerIndex());
            }
        }
        return -1;  // Not found.
    }


	@Override
	protected void decode(ChannelHandlerContext ctx,
			ByteBuf in, List<Object> msgLstResult) throws Exception {
		
		MsrpMessageWrapper msgDecoded;
		ByteBuf slice;
		int length;
		
        logger.trace( "Srv  Nb bytes "+in.readableBytes() );
		logger.trace(" Buffer in ref count "+ in.refCnt() );
		
		// End of loop on return statement: end of line not found, not enough readable Byte or exception
		for(;;) {
			
			if (decodingState != CodingState.IN_CONTENT_STUFF_LENGTH) {

				length = findEndOfLine(in);

				if (length != -1) {
					assert length >= 0: "Invalid length=" + length;
					slice = in.readSlice(length) ;
					in.skipBytes(2);
					logger.trace(" After read slice index :" + in.readerIndex());
				}
				else {
					return;
				}

				msgDecoded = decodeOutStuff(ctx, slice );

				if (msgDecoded != null) {
					msgLstResult.add(msgDecoded);
				}
			}
			else {

				assert inStuffLength >= 0: "Invalid length=" + inStuffLength;
				
				if ( in.readableBytes() >= inStuffLength) {
					slice = in.readSlice(inStuffLength) ;
	
					loggerMsg.info("Read content length: {}, content: {}", inStuffLength, slice.toString());			
					decodeMsg.appendContent(slice);
					decodeMsg.setContentLength();
					decodingState = CodingState.WAIT_END_LINE;
				}
				else {
					return;
				}
				
			}

		}
	}
    
		
	// Decoder out of Stuff
	private MsrpMessageWrapper decodeOutStuff(ChannelHandlerContext ctx, ByteBuf byteLine) throws Exception {
		
        logger.trace( "Srv  decodingState "+ decodingState.toString() + ", refcnt: "+ Integer.toString(byteLine.refCnt()));
		
        // Keep buffer Slice for next call
        //byteLine.retain();

		int lineLength = byteLine.readableBytes();
        
//        if (lineLength < 2) {
//        	logger.info("taille ligne <2 "+ Integer.toString(lineLength) );
//        	return null; 
//        }
        
        Matcher matchLine;
        String lineStr;
        byte endLine='x';
        
		switch (decodingState) {
		 
		case WAIT_FIRST_LINE:
			
			// re-init content length
			inStuffLength = -1;

			// skip first empty line
			if ( lineLength == 0 || (byteLine.getByte(0) == '\r' && byteLine.getByte(1) == '\n')) {
				return null;
			}
			
			lineStr = byteLine.toString(Charset.forName("UTF-8"));
			loggerMsg.info(lineStr);			

			matchLine = MsrpSyntaxRegexp.pattReqLine.matcher(lineStr);

			if (matchLine.matches()) {
				decodeMsg = new MsrpMessageWrapper();
				
				decodeMsg.firstLine = lineStr ;
				
				decodeMsg.transactionId = matchLine.group(1); 
				decodeMsg.cmdMSRP = matchLine.group(2);

                switch (decodeMsg.cmdMSRP) {
                    case "SEND":
                        decodeMsg.msgType = MsrpMsgTypeEnum.MSRP_SEND;
                        break;
                    case "REPORT":
                        decodeMsg.msgType = MsrpMsgTypeEnum.MSRP_REPORT;
                        break;
                    default:
                        decodeMsg.msgType = MsrpMsgTypeEnum.MSRP_UNDEF;
                        break;
                }
				
				decodingState = CodingState.WAIT_TO_PATH; 
			}
			else {
				
				matchLine = MsrpSyntaxRegexp.pattRespLine.matcher(lineStr);
				
				if (matchLine.matches()) {
					decodeMsg = new MsrpMessageWrapper();
					
					decodeMsg.firstLine = lineStr ;
					decodeMsg.msgType=MsrpMsgTypeEnum.MSRP_RESPONSE;
					
					decodeMsg.transactionId = matchLine.group(1); 
					decodeMsg.statusCode = matchLine.group(2); 
					decodeMsg.commentRsp = matchLine.group(3); 

					decodingState = CodingState.WAIT_TO_PATH; 
				}
				else {
					//TODO Should we disconnect this Cnx ?
					//     or silently discard until next First Line
					decodingState = CodingState.WAIT_FIRST_LINE; 
//					decodingState = CodingState.WAIT_END_MSG_ERROR; 
					
			        logger.error( "Failed to decode first line : {}", lineStr);
				}
			}
			break;

			
		case WAIT_TO_PATH:
	        logger.trace( "Srv  WAIT_TO_PATH ");
			
			lineStr = byteLine.toString(Charset.forName("UTF-8")) ;
			loggerMsg.info(lineStr);			
			
			matchLine = MsrpSyntaxRegexp.patt_ToPath.matcher(lineStr);
			
			if (matchLine.matches()) {

		        logger.trace( "Srv  msrp_uri: "+ matchLine.group(1));
				decodingState = CodingState.WAIT_FROM_PATH; 
				decodeMsg.toPath = matchLine.group(1);
		        decodeMsg.toPathSessionId = matchLine.group(8);
			}			
			else {
				decodingState = CodingState.WAIT_END_MSG_ERROR; 
		        logger.error( "Failed to decode To-Path : {}", lineStr);
			}
			break;

		case WAIT_FROM_PATH:
	        logger.trace( "Srv  WAIT_FROM_PATH ");
			
			lineStr = byteLine.toString(Charset.forName("UTF-8")) ;
			loggerMsg.info(lineStr);			
			
			matchLine = MsrpSyntaxRegexp.patt_FromPath.matcher(lineStr);

			if (matchLine.matches()) {
				decodingState = CodingState.MSRP_HEADERS; 
				decodeMsg.fromPath = matchLine.group(1);
				decodeMsg.fromPathSessionId = matchLine.group(8);
			}			
			else {
				decodingState = CodingState.WAIT_END_MSG_ERROR; 
		        logger.error( "Failed to decode From-Path : {}", lineStr);
			}
			break;

		case MSRP_HEADERS:
	        logger.trace( "Srv  MSRP_HEADERS ");
			
			lineStr = byteLine.toString(Charset.forName("UTF-8")) ;
			loggerMsg.info(lineStr);			

			
			// Test end-line
			if (  byteLine.getByte(0) == '-' ) {
				matchLine = MsrpSyntaxRegexp.patt_EndLine.matcher(lineStr);
				
				if (matchLine.matches()) {

					decodeMsg.endLineTid = matchLine.group(1);

					String strEndFlag = matchLine.group(2);
					if ( strEndFlag == null) {
							decodingState = CodingState.WAIT_FIRST_LINE; 
							logger.error( "Failed to decode end line flag : {}", lineStr);
							return null;
					}
					decodeMsg.continuationFlag = strEndFlag.charAt(0);
					
					if ( decodeMsg.continuationFlag != '$' 
							&& decodeMsg.continuationFlag != '#'
							&& decodeMsg.continuationFlag != '+') {
						decodingState = CodingState.WAIT_FIRST_LINE; 
						logger.error( "Failed to decode end line flag : {}", lineStr);
						return null;
					}
						
					
					// End of Message !!
					decodingState = CodingState.WAIT_FIRST_LINE;

					// Return message to next Handler
					return decodeMsg;
				} 
				
			}
			
			matchLine = null;
			if ( byteLine.getByte(0) == 'C' ) {
				matchLine = MsrpSyntaxRegexp.patt_ContentType.matcher(lineStr);
			}

			if (matchLine != null && matchLine.matches()) {
				decodeMsg.contentType = matchLine.group(1);
				decodingState = CodingState.WAIT_CONTENT_STUFF; 
			} 
			else {
				matchLine = null;
				if ( byteLine.getByte(0) == 'B' ) {
					matchLine = MsrpSyntaxRegexp.patt_ByteRange.matcher(lineStr);
				}

				if (matchLine != null && matchLine.matches()) {
					decodeMsg.hasByteRange = true;
					decodeMsg.byteRangeStart = matchLine.group(1);
					decodeMsg.byteRangeLength = matchLine.group(2);
					decodeMsg.byteRangeEnd = matchLine.group(3);
				} 
				else {
					matchLine = null;
					if ( byteLine.getByte(0) == 'M' ) {
						matchLine = MsrpSyntaxRegexp.patt_MsgId.matcher(lineStr);
					}

					if (matchLine != null && matchLine.matches()) {
						decodeMsg.messageId = matchLine.group(1);
					} 
					else {

						matchLine = MsrpSyntaxRegexp.patt_HeaderGen.matcher(lineStr);

						if (matchLine != null && matchLine.matches()) {

							if (matchLine.matches()) {
								decodeMsg.headersMap.put(matchLine.group(1), matchLine.group(2));
							} 
							else {
								decodingState = CodingState.WAIT_END_MSG_ERROR; 
								logger.error( "Failed to decode header : {}", lineStr);
							}
						}
					}
				}
			}
			
			break;
			
		case WAIT_CONTENT_STUFF:
	        logger.trace( "Srv  WAIT_CONTENT_STUFF ");
	        
			if ( lineLength < 2 || (byteLine.getByte(0) == '\r' && byteLine.getByte(1) == '\n')) {
				
				if (decodeMsg.hasByteRange && decodeMsg.byteRangeLength != null
						&& decodeMsg.byteRangeLength.equals("*") && decodeMsg.byteRangeLength.equals("0") ) {
					
					inStuffLength = Integer.valueOf(decodeMsg.byteRangeLength) 
											- Integer.valueOf(decodeMsg.byteRangeStart)+1;
					if (inStuffLength <= 0) {
						decodingState = CodingState.IN_CONTENT_STUFF;
						inStuffLength = -1;
					}
					else {
						decodingState = CodingState.IN_CONTENT_STUFF_LENGTH;
					}
				}
				else {
					decodingState = CodingState.IN_CONTENT_STUFF;
				}
				
				loggerMsg.info("");			
			}
			else {
				decodingState = CodingState.WAIT_END_MSG_ERROR; 
		        logger.error( "Failed to decode CRLF after content-type header : {}", byteLine);
			}
			
			break;

		case IN_CONTENT_STUFF:
	        logger.trace( "Srv  IN_CONTENT_STUFF ");

			// 7 '-' + 4 min tid + flag      (withdraw + CR LF)
			if ( lineLength > 11 ) {
				
		        try {
		        	if (byteLine.writerIndex()>0) {
		        		endLine = byteLine.getByte(byteLine.writerIndex()-1);
		        	}
		        	else {
			        	logger.warn("In stuff and byteline index {}, linelenght {}", byteLine.writerIndex(), lineLength);
			        	logger.warn("Raw content obj {}", decodeMsg.rawContent);
		        	}
		        }
		        catch(Exception e) {
		        	logger.error("Failed on index {}, linelenght {}, exception: ", byteLine.writerIndex(), lineLength, e);
		        	throw e;
		        }
		        
				if (byteLine.getByte(0) == '-'	&& byteLine.getByte(1) == '-' && byteLine.getByte(2) == '-' 
					&& byteLine.getByte(3) == '-' && byteLine.getByte(4) == '-' && byteLine.getByte(5) == '-' 
					&& byteLine.getByte(6) == '-' 
					&& ( endLine == '$' || endLine == '+' || endLine == '#') ) {
				
					lineStr = byteLine.toString(Charset.forName("UTF-8")) ;
					
					matchLine = MsrpSyntaxRegexp.patt_EndLine.matcher(lineStr);
					
					if (matchLine.matches()) {
	
				        logger.trace( "Srv  end transactionId "+ matchLine.group(1));
	
						if (matchLine.group(1).contentEquals(decodeMsg.transactionId)) {
							decodeMsg.endLineTid = matchLine.group(1);		
							decodeMsg.continuationFlag = (char) endLine;
							
							// End of Message correct !!
							decodingState = CodingState.WAIT_FIRST_LINE;
							//decodeMsg.cleanContentCRLF();
							
							// Return message to next Handler
							loggerMsg.info(lineStr);
							
							// suppress last \r\n just read before endline
							decodeMsg.cleanContentCRLF();
							decodeMsg.setContentLength();
							return decodeMsg;
						} 
						else {
					        logger.info(" Read a end-line like with bad TransactionID, it is considered as part of the content : {}", lineStr);
						}
					}
				}
			}

			// Append to contentStuff
			loggerMsg.info(byteLine.toString(Charset.forName("UTF-8")));			
			decodeMsg.appendContent(byteLine);
			decodeMsg.appendContentCRLF();
			break;
			
			
		case WAIT_END_MSG_ERROR:
		case WAIT_END_LINE:
	        logger.trace( "Status {}, line: {} ", decodingState, byteLine.toString(Charset.forName("UTF-8")));
			loggerMsg.info(byteLine.toString(Charset.forName("UTF-8")));			
			
			if ( lineLength < 2 || (byteLine.getByte(0) == '\r' && byteLine.getByte(1) == '\n')) {
				return null;
			}			
			
			// 7 '-' + 4 min tid + flag      (withdraw + CR LF)
	        endLine = byteLine.getByte(byteLine.writerIndex()-1);
	        
			if ( lineLength > 11 && byteLine.getByte(0) == '-'	&& byteLine.getByte(1) == '-' && byteLine.getByte(2) == '-' 
					&& byteLine.getByte(3) == '-' && byteLine.getByte(4) == '-' && byteLine.getByte(5) == '-' 
					&& byteLine.getByte(6) == '-' 
					&& ( endLine == '$' || endLine == '+' || endLine == '#') ) {

				logger.trace( "Found end line " + byteLine.toString(Charset.forName("UTF-8")));
				decodeMsg.continuationFlag = (char) endLine;
				
				// If normal ending after IN_STUFF_LENGTH check transaction Id
				if (decodingState == CodingState.WAIT_END_LINE) {

					lineStr = byteLine.toString(Charset.forName("UTF-8")) ;

					matchLine = MsrpSyntaxRegexp.patt_EndLine.matcher(lineStr);

					if (matchLine.matches()) {

						logger.trace( "Srv  end transactionId "+ matchLine.group(1));
						decodeMsg.endLineTid = matchLine.group(1);		

						if (! matchLine.group(1).contentEquals(decodeMsg.transactionId)) {

							logger.error("Unexpected end transaction Id: {}", decodeMsg.endLineTid);
							logger.error("However, the message is retuned for tid: {}", decodeMsg.transactionId);
						}
						decodingState = CodingState.WAIT_FIRST_LINE;

						// Return message to next Handler
						inStuffLength = -1;
						decodeMsg.setContentLength();
						return decodeMsg;
					}
				}
				
				decodingState = CodingState.WAIT_FIRST_LINE;
			}
			break;
			
		default:
			break;
		}
			
		return null;
	}




}
