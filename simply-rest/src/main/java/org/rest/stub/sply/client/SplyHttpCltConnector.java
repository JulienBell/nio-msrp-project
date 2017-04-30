package org.rest.stub.sply.client;


import org.enabler.restlib.RequestFailureReason;
import org.enabler.restlib.RestHttpException;
import org.enabler.restlib.handlers.RestFutureResult;
import org.enabler.restlib.handlers.RestHandlersContainer;
import org.enabler.restlib.handlers.UserResponseHandler;
import org.enabler.restlib.handlers.RestAsyncResponseMgnt;
import org.enabler.restlib.handlers.RestFutureHandler;
import org.enabler.restlib.wrapper.RestStubFullHttpRequest;
import org.enabler.restlib.wrapper.RestStubFullHttpResponse;
import org.rest.stub.sply.api.SplyHttpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SplyHttpCltConnector extends SplyHttpConnector {
	
    private static final Logger logger =
            LoggerFactory.getLogger(SplyHttpCltConnector.class);

    public SplyHttpClientSock getCltSock() {
		return cltSock;
	}


	private SplyHttpClientSock cltSock;
	
	public SplyHttpCltConnector(String uriServer) throws RestHttpException {

		// set notifier on connector
		super(null, false, null);

		// Create channel with SpdyHttpDecoder/Encoder and SpdyHandler
		try {
			cltSock = new SplyHttpClientSock(uriServer, this);
		} catch (Exception e) {
			throw new RestHttpException("Failed to create Client Sock",e);
		}
		
		//super.channel = cltSock.getChannel();

	}

	
	private class CltResponseHd extends RestFutureHandler implements UserResponseHandler {
		
		public void onResponseReceivedCB(RestStubFullHttpResponse resp) {
			synchronized (this) {
				notifyAll();
			}
		}

		@Override
		public void onRequestFailureCB(RequestFailureReason reason) {
			synchronized (this) {
				notifyAll();
			}
		}

		@Override
		public void onUnexpectedExceptionCB(Throwable cause) {
			synchronized (this) {
				notifyAll();
			}
		}
	}
	
	public RestStubFullHttpResponse handleCltRequest(RestStubFullHttpRequest req) throws RestHttpException {
		
		CltResponseHd respHd = new CltResponseHd();
		
		RestAsyncResponseMgnt asyncHd = new RestAsyncResponseMgnt(respHd, respHd);
		
		RestFutureResult futurResult = handleCltSendRequest(req, asyncHd);
		
		// Wait for response
		if (! futurResult.isComplete()) {
			try {
				futurResult.waitResult(10000);
			} catch (InterruptedException e) {

				cancelWaiter(futurResult);

				throw new InternalError("Interrupt while wait response to request "+ req);
			}
		}

		return futurResult.getResult();
		
	}

	
	public RestFutureResult handleCltSendRequest(RestStubFullHttpRequest req, RestAsyncResponseMgnt respHd) throws RestHttpException {
		
		//logger.error("ERREUR codage dans isConnected no thread safe");
		//boolean isCon=cltSock.isConnected();
		if (channel == null || ! cltSock.isConnected()) {
			channel = cltSock.getChannel();
		}

		if (channel == null) {
			logger.error("returned channel is  {}", channel);
			assert( channel != null );
			//logger.error("returned status connected tested {}", isCon);
		}

		RestFutureHandler future = new RestFutureHandler();
		
		logger.debug("write request to the channel {}", channel);

		channel.writeAndFlush(new RestHandlersContainer(req, respHd, future));
		
		return future;
	}
	
	@Override
	public void handleRcvRequest(RestStubFullHttpRequest req) {
		
		// Execute notifier async in workers pool thread
		throw new UnsupportedOperationException("Receive request on client not supported !");
	}
	

	public void shutdown() {
		// Shutdown client socket
		cltSock.shutdown();
	}

	
}
