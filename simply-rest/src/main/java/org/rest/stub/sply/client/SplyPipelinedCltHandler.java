package org.rest.stub.sply.client;

import java.util.LinkedList;



import org.enabler.restlib.wrapper.RestStubFullHttpRequest;
import org.enabler.restlib.wrapper.RestStubFullHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpResponse;


public class SplyPipelinedCltHandler extends ChannelDuplexHandler {

    private static final Logger logger =
        LoggerFactory.getLogger(SplyPipelinedCltHandler.class);

	LinkedList<String> streamIdFifo = new LinkedList<String>();
	
	 public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
	 {
		 // On response get first StreamId response waited and push it to next Handler

		 String strId = streamIdFifo.remove();
		 
		 logger.debug("stream to return {}", strId);
		 
		 RestStubFullHttpResponse restMsg = new RestStubFullHttpResponse((FullHttpResponse)msg, strId);

		 ctx.fireChannelRead(restMsg);
	 }
	 
	 
	  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
	  {
		  // request create a StreamId, push it on the queue and send request towards server
		  
		  RestStubFullHttpRequest restReq = (RestStubFullHttpRequest) msg;

		  String strId = restReq.getStreamId();

		  logger.debug("stream id to send {}", strId);
			 
		  streamIdFifo.add(strId);
		  
		  // Push to next handler
	  	  ctx.writeAndFlush(msg, promise);
	  }	 
}
