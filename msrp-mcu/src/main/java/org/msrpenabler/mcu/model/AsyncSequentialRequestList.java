/**
 * 
 */
package org.msrpenabler.mcu.model;

import java.util.LinkedList;
import java.util.NoSuchElementException;

import org.enabler.restlib.handlers.RestFutureHandler;
import org.enabler.restlib.wrapper.RestStubFullHttpRequest;

/**
 * @author Julien
 *
 */
class AsyncSequentialRequestList {
	
	// Can be access from different thread
	private volatile LinkedList<AsyncRequestEntry> reqList = new LinkedList<AsyncRequestEntry>();
	//final private URI baseURI;
	
	public AsyncSequentialRequestList() {
		//this.baseURI = baseURI;
	}

	public int add(RestStubFullHttpRequest request, RestFutureHandler future) {
		
		AsyncRequestEntry asyncEntry = new AsyncRequestEntry(request, future);
		reqList.add(asyncEntry);
		
		return reqList.size();
	}
	/*
	public AsyncSequentialRequestList push(NotifMethodEnum method, HashMap<String, String> parameters) {
		
		QueryStringEncoder query = new QueryStringEncoder(baseURI.toString() + "/" + method.toString()); 
		
		if (parameters != null) {
			Iterator<Entry<String, String>> it = parameters.entrySet().iterator();

			while (it.hasNext()) {
				Entry<String, String> elt = it.next();
				query.addParam(elt.getKey(), elt.getValue());
			}
		}

		RestStubFullHttpRequest request = new RestStubFullHttpRequest(
				HttpVersion.HTTP_1_1, HttpMethod.GET, query.toString());
		request.headers().set(HttpHeaders.Names.HOST, baseURI.getHost());
		
		AsyncRequestEntry asyncEntry = new AsyncRequestEntry(request);
		reqList.add(asyncEntry);
		
		return this;
	}
	
	public AsyncSequentialRequestList push(	NotifMethodEnum method, HashMap<String, String> parameters, 
										String contentType, ByteBuf content) {
		
		QueryStringEncoder query = new QueryStringEncoder(baseURI.toString() + "/" + method.toString()); 
		
		if (parameters != null) {
			Iterator<Entry<String, String>> it = parameters.entrySet().iterator();

			while (it.hasNext()) {
				Entry<String, String> elt = it.next();
				query.addParam(elt.getKey(), elt.getValue());
			}
		}

		
		RestStubFullHttpRequest request = new RestStubFullHttpRequest(
				HttpVersion.HTTP_1_1, HttpMethod.GET, query.toString(), contentType, content);
		
		request.headers().set(HttpHeaders.Names.HOST, baseURI.getHost());
		
		AsyncRequestEntry asyncEntry = new AsyncRequestEntry(request);
		reqList.add(asyncEntry);
				
		return this;
	}
*/
	public AsyncRequestEntry pop() {
	
		return reqList.removeFirst();
	}

	public AsyncRequestEntry first() {
		
		try {
			return reqList.getFirst();
		}
		catch (NoSuchElementException e) {
			return null;
		}
	}
	
}
