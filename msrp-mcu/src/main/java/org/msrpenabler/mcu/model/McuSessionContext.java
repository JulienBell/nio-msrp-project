/**
 * 
 */
package org.msrpenabler.mcu.model;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringEncoder;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;



import java.util.concurrent.ConcurrentHashMap;

import org.enabler.restlib.RequestFailureReason;
import org.enabler.restlib.RestHttpException;
import org.enabler.restlib.handlers.UserResponseHandler;
import org.enabler.restlib.handlers.RestAsyncResponseMgnt;
import org.enabler.restlib.handlers.RestFutureHandler;
import org.enabler.restlib.handlers.RestFutureResult;
import org.enabler.restlib.wrapper.RestStubFullHttpRequest;
import org.enabler.restlib.wrapper.RestStubFullHttpResponse;
import org.msrpenabler.mcu.restclt.NotifMethodEnum;
import org.msrpenabler.server.api.MsrpSessionHd;
import org.rest.stub.sply.client.SplyHttpCltConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Julien Bellanger
 *
 */
public class McuSessionContext implements UserResponseHandler {

	private SplyHttpCltConnector cnxClt;
	private MsrpSessionHd sess;
	
	private ConcurrentHashMap<String, Object> mapAttributes = new ConcurrentHashMap<String, Object>();

	private static final Logger logger = LoggerFactory.getLogger(McuSessionContext.class);


	// Queue of request list in Ctx to ordering all notif call
	AsyncSequentialRequestList listRequest;


	public McuSessionContext(SplyHttpCltConnector cnxClt, MsrpSessionHd sess) {
		this.cnxClt = cnxClt;
		this.sess = sess;
		listRequest = new AsyncSequentialRequestList();
	}

	public SplyHttpCltConnector getCnxClt() {
		return cnxClt;
	}

	public MsrpSessionHd getSess() {
		return sess;
	}

	@Override
	public void onRequestFailureCB(RequestFailureReason reason) {

		try {
			AsyncRequestEntry entry = listRequest.pop();

			entry.resHd.onRequestFailureCB(reason);
		}
		catch (Exception e) {
			logger.error("Exception :", e);
		}
		callNextNotif();
	}

	@Override
	public void onResponseReceivedCB(RestStubFullHttpResponse resp) {

		try {
			AsyncRequestEntry entry = listRequest.pop();

			entry.resHd.onResponseReceivedCB(resp);
			
		}
		catch (Exception e) {
			logger.error("Exception for resp {}:", resp, e);
		}
		finally {
			// Free response buffer
			resp.release();
		}

		callNextNotif();
	}

	@Override
	public void onUnexpectedExceptionCB(Throwable cause) {
		try {
			AsyncRequestEntry entry = listRequest.pop();

			entry.resHd.onUnexpectedExceptionCB(cause);

		}
		catch (Exception e) {
			logger.error("Exception :", e);
		}
		callNextNotif();
	}


	public RestFutureResult call_notif(NotifMethodEnum method, HashMap<String, String> parameters) {

		URI baseURI;

		baseURI = cnxClt.getCltSock().getUriServer();

		QueryStringEncoder query ; 
		if (method != null) {
			query = new QueryStringEncoder(baseURI.toString() + "/" + method.toString()); 
		}
		else {
			query = new QueryStringEncoder(baseURI.toString()); 
		}

		RestStubFullHttpRequest request=null;

		if (parameters != null) {
			Iterator<Entry<String, String>> it = parameters.entrySet().iterator();

			while (it.hasNext()) {
				Entry<String, String> elt = it.next();
				query.addParam(elt.getKey(), elt.getValue());
			}
		}

		request = new RestStubFullHttpRequest(
				HttpVersion.HTTP_1_1, HttpMethod.GET, query.toString());
		request.headers().set(HttpHeaders.Names.HOST, baseURI.getHost());

		return callNotif(request);
	}

	public RestFutureHandler call_notif(NotifMethodEnum method,
			HashMap<String, String> parameters, String contentType, ByteBuf content) {

		URI baseURI;
		baseURI = cnxClt.getCltSock().getUriServer();
		
		logger.debug("baseuri: {}, method: {}", baseURI, method);

		QueryStringEncoder query ; 
		if (method != null) {
			query = new QueryStringEncoder(baseURI.toString() + "/" + method.toString()); 
		}
		else {
			query = new QueryStringEncoder(baseURI.toString()); 
		}

		RestStubFullHttpRequest request=null;

		if (parameters != null) {
			Iterator<Entry<String, String>> it = parameters.entrySet().iterator();

			while (it.hasNext()) {
				Entry<String, String> elt = it.next();
				query.addParam(elt.getKey(), elt.getValue());
			}
		}

		request = new RestStubFullHttpRequest(
				HttpVersion.HTTP_1_1, HttpMethod.GET, query.toString(), contentType, content);
		request.headers().set(HttpHeaders.Names.HOST, baseURI.getHost());
		
		return callNotif(request);
		
	}

	
	public void callNextNotif() {

		// Call Next request
		AsyncRequestEntry entry = listRequest.first();
		if (entry == null) {
			logger.debug("No more request to Call");
		}
		else {
			try {
				logger.debug("Call next notif {}:", entry.req);

				RestAsyncResponseMgnt asyncResp = new RestAsyncResponseMgnt(this, entry.futResult);

				cnxClt.handleCltSendRequest(entry.req, asyncResp);
				logger.debug("After call next notif {}:", entry.req);
			} catch (RestHttpException e) {
				logger.error("Failure on send notif {}\n exception :", entry.req, e);
			}
		}

	}
	
	
	public RestFutureHandler  callNotif(RestStubFullHttpRequest request) {

		RestFutureHandler future = new RestFutureHandler();

		int size = listRequest.add(request, future);

		logger.debug("request list size {}:", size);

		if (size == 1) {
			callNextNotif();
		}

		return future;
	}

	public Object getAttribute(String attributeName) {
		return mapAttributes.get(attributeName);
	}

	public void setAttribute(String attributeName, Object object) {
		mapAttributes.put(attributeName, object);
	}


}
