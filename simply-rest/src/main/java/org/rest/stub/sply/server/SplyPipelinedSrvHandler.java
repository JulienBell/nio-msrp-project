package org.rest.stub.sply.server;

import java.util.HashMap;



import org.enabler.restlib.wrapper.RestStubFullHttpRequest;
import org.enabler.restlib.wrapper.RestStubFullHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpRequest;

public class SplyPipelinedSrvHandler extends ChannelDuplexHandler {

    private static final Logger logger =
        LoggerFactory.getLogger(SplyPipelinedSrvHandler.class);

	
	int nbWaitingRequest=0;
	Integer lastIdRcv=0;
	Integer firstIdtoResp;

    private class RespAndPromise {

        RestStubFullHttpResponse req;
        ChannelPromise pr;

        public RespAndPromise(RestStubFullHttpResponse req, ChannelPromise pr) {
            this.req = req;
            this.pr = pr;
        }
    }

	HashMap<Integer, RespAndPromise> responseToSend = new HashMap<Integer, RespAndPromise>();
	
	 public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
	 {
		 lastIdRcv++;

		 if (nbWaitingRequest == 0) {
			 firstIdtoResp = lastIdRcv;
		 }
		 nbWaitingRequest++;
		 
		 RestStubFullHttpRequest restMsg = new RestStubFullHttpRequest( (FullHttpRequest) msg, lastIdRcv.toString());
		 
		 ctx.fireChannelRead(restMsg);
	 }
	 
	 
	  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
	  {
		  // If StreamId equal first one to respond:
		  // 			*	send the response
		  //			*	while next stream in queue has an available response, sends it 

		  // Else set streamId elt with the response into map
		  
		  // Push to next handler

		  if (msg instanceof RestStubFullHttpResponse) {
			  RestStubFullHttpResponse restResp = (RestStubFullHttpResponse) msg;
			  logger.info("Message to write {}", restResp);
	
			  Integer strId = new Integer(restResp.getStreamId()) ;
			 
			  if (strId.equals(firstIdtoResp)) {
			  	  ctx.write(msg, promise);
			  	  
			  	  firstIdtoResp++;
			  	  nbWaitingRequest--;
	
	              RespAndPromise reqAndPr;
	              
			  	  while (! responseToSend.isEmpty() ) {
	
	                  reqAndPr = responseToSend.remove(firstIdtoResp);
	
				  	  if (reqAndPr != null) {
	                      restResp = reqAndPr.req;
	                      promise = reqAndPr.pr;
	
					  	  ctx.writeAndFlush(restResp, promise);
					  	  firstIdtoResp++;
					  	  nbWaitingRequest--;
				  	  }
				  	  else {
				  		  return;
				  	  }
			  	  }
			  }
			  else {
				  logger.info("Add response to send in wait list");
				  //restResp.retain();
				  responseToSend.put(strId, new RespAndPromise(restResp, promise));
			  }
		  }
		  
		  else {
			  // Push to next handler
			  ctx.writeAndFlush(msg, promise);
		  }
		  
	  }	 
}

