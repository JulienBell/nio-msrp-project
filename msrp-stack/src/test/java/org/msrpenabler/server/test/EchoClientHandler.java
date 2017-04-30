/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.msrpenabler.server.test;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler implementation for the echo client.  It initiates the ping-pong
 * traffic between the echo client and server by sending the first message to
 * the server.
 */
public class EchoClientHandler extends ChannelInboundHandlerAdapter {

	private static final Logger logger =
	        LoggerFactory.getLogger(EchoClientHandler.class);

//    private final ByteBuf firstMessage;

    /**
     * Creates a client-side handler.
     */
    public EchoClientHandler() {
    	
//        if (firstMessageSize <= 0) {
//            throw new IllegalArgumentException("firstMessageSize: " + firstMessageSize);
//        }
//        
//        firstMessage = Unpooled.buffer(firstMessageSize);
//        for (int i = 0; i < firstMessage.capacity(); i ++) {
//            firstMessage.writeByte((byte) i);
//        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        
        logger.info("Client Active");
    	//ctx.write(firstMessage);
    	
//        String message = "hello\r\n";
//        ByteBuf outBuf = Unpooled.buffer();
//        
//        try {
//			outBuf.writeBytes(message.getBytes("UTF-8"));
//			
//	        logger.log(Level.INFO, "Clt Nb byte " + outBuf.readableBytes() );
//	        for (int i = 0; i < outBuf.readableBytes(); i ++) {
//	            logger.log(Level.INFO, "Clt buff byte "+Integer.toString(i)+": "+ outBuf.getByte(i) );
//	        }
//			
//		} catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//        
//        ctx.write(outBuf);
//
//        
//        message = "Ca va\r\n";
//        outBuf = Unpooled.buffer();
//        
//        try {
//			outBuf.writeBytes(message.getBytes("UTF-8"));
//			
//	        logger.log(Level.INFO, "Clt 2 Nb byte " + outBuf.readableBytes() );
//	        for (int i = 0; i < outBuf.readableBytes(); i ++) {
//	            logger.log(Level.INFO, "Clt buff byte "+Integer.toString(i)+": "+ outBuf.getByte(i) );
//	        }
//			
//		} catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//        
//        ctx.write(outBuf);
        
    }

//    /**
//     * Returns the length between reader index and the the end of line found.
//     * Returns -1 if no end of line was found in the buffer.
//     */
//    private static int findEndOfLine(final ByteBuf buffer) {
//        final int n = buffer.writerIndex();
//        for (int i = buffer.readerIndex(); i < n; i ++) {
//            final byte b = buffer.getByte(i);
//            if ( (b == '\r') && i < n - 1 && buffer.getByte(i + 1) == '\n' ) {
//                return (i+2 - buffer.readerIndex());
//            }
//        }
//        return -1;  // Not found.
//    }

//    @Override
//    public void messageReceived(ChannelHandlerContext ctx, List<Object> msgs) {
//        logger.info("Client inbound updated");
//        
//        Iterator<Object> iter = msgs.iterator();
//        while (iter.hasNext()) {
//        	
//        	ByteBuf in = (ByteBuf) iter.next();
//        	
//			int length = findEndOfLine(in);
//			logger.trace("Start decode " + length + " pos " + in.readerIndex());
//	
//			if (length != -1) {
//				assert length >= 0: "Invalid length=" + length;
//				ByteBuf read = in.readBytes(length-2) ;
//				logger.trace(" After read index :" + in.readerIndex());
//				logger.info("Line:    {}", read.toString(Charset.forName("UTF-8")));
//				read = in.readBytes(2) ;
//			}
//			else {
//				logger.info(" out ...");
//				return;
//			}
//        }
//
//        //ByteBuf out = ctx.nextOutboundByteBuffer();
//        //out.writeBytes(in);
//        //ctx.flush();
//    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        logger.warn("Unexpected exception from downstream.", cause);
        ctx.close();
    }
}
