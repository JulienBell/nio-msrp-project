package org.msrpenabler.mcu.model;

import io.netty.handler.codec.http.HttpResponseStatus;


import org.enabler.restlib.RequestFailureReason;
import org.enabler.restlib.handlers.UserResponseHandler;
import org.enabler.restlib.wrapper.RestStubFullHttpRequest;
import org.enabler.restlib.wrapper.RestStubFullHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmptyRespCBHandler implements UserResponseHandler {
	
	final RestStubFullHttpRequest request;
	
	private static final Logger logger = LoggerFactory.getLogger(EmptyRespCBHandler.class);
	
	
	public EmptyRespCBHandler(RestStubFullHttpRequest request) {
		this.request = request;
	}

	public void operationComplete(RestStubFullHttpResponse resp) {

	}

	@Override
	public void onRequestFailureCB(RequestFailureReason reason) {
		logger.error("failure {} on send notif {}:", reason, request );
	}

	@Override
	public void onResponseReceivedCB(RestStubFullHttpResponse resp) {

		if (resp.getStatus() != HttpResponseStatus.ACCEPTED
				 || resp.getStatus() != HttpResponseStatus.OK
				 || resp.getStatus() != HttpResponseStatus.NO_CONTENT) {
				logger.debug("receive http response {} on notif {}:", resp.toString(), request);
			}
			else {
				logger.error("receive http response failure {} on notif {}:", request );
			}
	}

	@Override
	public void onUnexpectedExceptionCB(Throwable cause) {

		logger.error("Unexpected exception :", cause );
		
	}
}
