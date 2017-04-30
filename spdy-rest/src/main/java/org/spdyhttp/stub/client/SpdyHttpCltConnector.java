package org.spdyhttp.stub.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import org.enabler.restlib.RequestFailureReason;
import org.enabler.restlib.RestHttpException;
import org.enabler.restlib.RestHttpServlet;
import org.enabler.restlib.handlers.RestFutureHandler;
import org.enabler.restlib.handlers.RestFutureResult;
import org.enabler.restlib.handlers.RestHandlersContainer;
import org.enabler.restlib.handlers.RestResponseHandler;
import org.enabler.restlib.wrapper.RestStubFullHttpRequest;
import org.enabler.restlib.wrapper.RestStubFullHttpResponse;
import org.spdyhttp.stub.api.SpdyHttpConnector;

public class SpdyHttpCltConnector extends SpdyHttpConnector {

	private SpdyHttpClientSock cltSock;
	
	public SpdyHttpCltConnector(String uriServer, int maxSpdyFrames, 
			RestHttpServlet notifier, int maxNotifWorkers) throws RestHttpException {

		// set notifier on connector
		super(notifier, false, null);

		this.notifierWorkers = Executors.newFixedThreadPool(maxNotifWorkers);
		
		// Create channel with SpdyHttpDecoder/Encoder and SpdyHandler
		try {
			cltSock = new SpdyHttpClientSock(uriServer, maxSpdyFrames, this);
		} catch (Exception e) {
			throw new RestHttpException("Failed to create Client Sock",e);
		}
		
		super.channel = cltSock.getChannel();

	}

	
	private final ExecutorService notifierWorkers;
	
	private class ExecuteNotifRequest implements Runnable {

		final RestStubFullHttpRequest request;
		final RestHttpServlet notifServlet;
		final SpdyHttpConnector connector;

		protected ExecuteNotifRequest(RestStubFullHttpRequest request,
				RestHttpServlet notifier, SpdyHttpConnector connector) {
			this.request = request;
			this.notifServlet = notifier;
			this.connector = connector;
		}

		public void run() {
			notifServlet.handleRcvRequest(request, connector);
		}
	}
	
	
	private class SpdyRespHdClt extends RestResponseHandler {
		
		@Override
		public void complete(RestStubFullHttpResponse msg) {
		}

		@Override
		public void failure(RequestFailureReason socketclosed) {
		}

		@Override
		public void exception(Throwable cause) {
		}
	}
	
	public RestFutureResult handleCltSendRequest(RestStubFullHttpRequest req, RestResponseHandler respHd) {
		
		if (channel == null || ! cltSock.isConnected()) {
			this.channel = cltSock.getChannel();
		}

		RestFutureHandler future = new RestFutureHandler();
		
		channel.writeAndFlush(new RestHandlersContainer(req, respHd, future));
		
		return future;
	}

	public RestStubFullHttpResponse handleCltRequest(RestStubFullHttpRequest req) throws RestHttpException {
		

		SpdyRespHdClt respHd = new SpdyRespHdClt();
		
		RestFutureResult future = handleCltSendRequest(req, respHd);
		
		// Wait for response
		if (! future.isComplete()) {
			try {
				future.waitResult(10000);
			} catch (Exception e) {

				cancelWaiter(future);
					
				throw new InternalError("Interrupt while wait response to request "+ req);
			}
		}
		
		return future.getResult();
	}
	
	
	@Override
	public void handleRcvRequest(RestStubFullHttpRequest req) {
		
		// Execute notifier async in workers pool thread
		notifierWorkers.execute(new ExecuteNotifRequest(req, requestServlet, this) );
		
	}
	

	public void shutdown() {
		// Shutdown client socket
		cltSock.shutdown();
	}


}
