package org.enabler.restlib;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;



import org.enabler.restlib.handlers.RestFutureHandler;
import org.enabler.restlib.handlers.RestFutureResult;
import org.enabler.restlib.handlers.RestHandlersContainer;
import org.enabler.restlib.wrapper.RestStubFullHttpRequest;
import org.enabler.restlib.wrapper.RestStubFullHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.spdy.SpdyHttpHeaders;

@Sharable
public abstract class RestHttpConnector  extends ChannelDuplexHandler {

	
    private static final Logger logger =
        LoggerFactory.getLogger(RestHttpConnector.class);


	protected final ConcurrentHashMap<Integer,RestHandlersContainer> mapRespHandlers = new ConcurrentHashMap<>();
    
	protected final RestHttpServlet requestServlet;

    protected AtomicInteger lastStreamId;
	
	protected volatile Channel channel;
	
	public RestHttpConnector(RestHttpServlet requestServlet, boolean isServer, Channel ch) {
		
		// set notifier
		this.requestServlet = requestServlet;
        channel = ch;
        
        lastStreamId = new AtomicInteger( (isServer ? 2 : 1));
	}

	/**
	 * 
	 * Call the servlet method.
	 * Be aware that if a content buffer is received, it is released after the handleRcvRequest call. 
	 * If content has to be used after this call, the callee has to call retain on the req.content() buffer
	 * 
	 * @param req
	 */
	public void handleRcvRequest(RestStubFullHttpRequest req) {
		
		requestServlet.handleRcvRequest(req, this);
		req.content().release();
		logger.info("content buffer ref count after release {}", req.content().refCnt());
	}
	
	
	final public void handleSendResponse(RestStubFullHttpRequest initReq, RestStubFullHttpResponse resp) throws RestHttpException {
		
		final String streamId = initReq.getStreamId();

		setStreamIdOnResponse(Integer.parseInt(streamId), resp);
		
		resp.setStreamId(streamId);
		
		if ( !HttpHeaders.isContentLengthSet(resp) && ! HttpHeaders.isTransferEncodingChunked(resp) ) {
			HttpHeaders.setTransferEncodingChunked(resp);
		}
		
		// Send Response on the Frame
		ChannelFuture future = channel.write(resp);
		
		// Listener for analyze result in async mode
		future.addListener(	new ChannelFutureListener() {

			public void operationComplete(ChannelFuture res) {
				
				if (! res.isSuccess()) {
					logger.error("Failed to send response on streamId {}, cause", streamId, res.cause());
				}
				else {
					logger.debug("Succeed in sending response on streamId {}", streamId);
				}
			}
		}
		);

		channel.flush();
		
	}

	
	public void cancelWaiter(RestFutureResult futur) {

		mapRespHandlers.remove( ((RestFutureHandler) futur).getStreamId());
	}
	
	
	final public ChannelPromise prepareRequestToSend(RestHandlersContainer reqData) throws RestHttpException {

		final int streamId;
		final ByteBuf content = reqData.request.content();
				

		streamId = lastStreamId.getAndAdd(2);

		setStreamIdOnRequest(streamId, reqData.request);
		
		reqData.respHandler.setStreamId(streamId);
		reqData.futur.setStreamId(streamId);
		
		mapRespHandlers.put(streamId, reqData);

		if ( !HttpHeaders.isContentLengthSet(reqData.request) && ! HttpHeaders.isTransferEncodingChunked(reqData.request) ) {
			HttpHeaders.setTransferEncodingChunked(reqData.request);
		}
		
		logger.debug("Send request {} on streamId {}", reqData.request, streamId);
		
		
		ChannelPromise promise = channel.newPromise();
		
		promise.addListener(new ChannelFutureListener() {

			public void operationComplete(ChannelFuture res) {
				
				if (! res.isSuccess()) {
					mapRespHandlers.remove(streamId);
					logger.error("Failed to send request on streamId {}, cause", streamId, res.cause());
				}
				else {
					logger.debug("Succeed in sending request on streamId {}, refCount content req {}", streamId, (content != null ? content.refCnt(): 0));
				}
			}
		}
		);
		
		return promise;
		
	}

	
	/**
	 * Has to be implemented by the connector type
	 * 
	 * @param streamId: calculated by the super class
	 * @param request: concerned request if the connector implementation had to push streamId into headers or parameter 
	 */
	protected abstract void setStreamIdOnRequest(int streamId, RestStubFullHttpRequest request) ;

	/**
	 * Has to be implemented by the connector type
	 * 
	 * @param streamId: calculated by the super class
	 * @param response: concerned request if the connector implementation had to push streamId into headers or parameter 
	 */
	protected abstract void setStreamIdOnResponse(int streamId, RestStubFullHttpResponse response) ;

	
	private void handleRcvResponse(RestStubFullHttpResponse msg) {

		String val = msg.getStreamId();
		
		Integer streamId = new Integer(val);
		
		RestHandlersContainer reqData = mapRespHandlers.remove(streamId);
		
		if (reqData.respHandler == null) {
			logger.warn("Unexpected received unwaited response: {}", msg);
			return;
		}
		
		// Set future
		if (reqData.futur != null) {
			reqData.futur.setResult(msg);
			reqData.futur.setSuccess();
		}
		
		// Call waiter
		reqData.respHandler.complete(msg);
	}

	
	public abstract void shutdown() ;
	
	
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
		logger.debug("Channel active");
    }

    
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		logger.debug("Read complete");
    	ctx.flush();
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    	
		logger.debug("Receive msg class: {}", msg.getClass().getName());

		if (! (msg instanceof HttpObject) ) {
    		throw new IllegalArgumentException("Input message is not an HttpObject");
    	}

        if (msg instanceof RestStubFullHttpRequest) {

    		logger.debug("Receive req RestStubFullHttpRequest");
        	handleRcvRequest((RestStubFullHttpRequest) msg);
            return;
        }

        if (msg instanceof DefaultFullHttpRequest) {

    		logger.debug("Receive req DefaultFullHttpRequest");
    		
    		String val = ((DefaultFullHttpRequest) msg).headers().get(SpdyHttpHeaders.Names.STREAM_ID);
    		
    		RestStubFullHttpRequest wrapper = new RestStubFullHttpRequest( (DefaultFullHttpRequest) msg, val);
    		
        	handleRcvRequest(wrapper);
            return;
        }

        if (msg instanceof RestStubFullHttpResponse) {
        	
    		logger.debug("Receive reponse RestStubFullHttpResponse ");
        	handleRcvResponse( (RestStubFullHttpResponse) msg);
            return;
        }

        if (msg instanceof DefaultFullHttpResponse) {
        	
    		logger.debug("Receive reponse DefaultFullHttpResponse");

    		String val = ((DefaultFullHttpResponse) msg).headers().get(SpdyHttpHeaders.Names.STREAM_ID);
    		
    		RestStubFullHttpResponse wrapper = new RestStubFullHttpResponse( (DefaultFullHttpResponse) msg, val);
    		
    		handleRcvResponse( wrapper );
            return;
        }

        throw new IllegalArgumentException("Unexpected HttpObject message "+ msg);
    }

    
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
    {
    	// request create a StreamId, push it on the queue and send request towards server

    	logger.debug("Msg to send class: {}", msg.getClass().getName());

    	if (! (msg instanceof RestHandlersContainer) ) {
    		// Send a response here
    		ctx.writeAndFlush(msg, promise);
    	}
    	else {
    		
    		// its a request
    		RestHandlersContainer reqData = (RestHandlersContainer) msg;

    		promise = prepareRequestToSend(reqData);

    		ctx.writeAndFlush(reqData.request, promise);
    	}

    	ctx.flush();
    }	 


    
	final private void evtSockClose() {

		Enumeration<Integer> keys = mapRespHandlers.keys();
		
		while (keys.hasMoreElements()) {
			Integer streamId = keys.nextElement();
			
			RestHandlersContainer respMgnt = mapRespHandlers.remove(streamId);
			if (respMgnt == null) {
				logger.warn("found a waiter null on sock closure for streamId: {}", streamId);
			}
			else {
				// Set futur result
				if (respMgnt.futur != null) {
					respMgnt.futur.setFailure();
				}
				
				// Call handlers
				respMgnt.respHandler.failure(RequestFailureReason.socketClosed);
			}
		}
	}
	
	
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
		logger.info("Get exception caught on connector : ", cause);
    }

    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    	evtSockClose();
        ctx.fireChannelInactive();
    }


    
}
	
