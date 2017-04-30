package org.spdyhttp.stub.client;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.enabler.restlib.RestHttpException;
import org.enabler.restlib.RestHttpServlet;

public class SpdyRestCltFactory {

	private static class ChargeurService {
		private static final SpdyRestCltFactory instance = new SpdyRestCltFactory();
	}

	public static SpdyRestCltFactory getInstance() {
		return ChargeurService.instance;
	}
	
	HashMap<String, SpdyHttpCltPoolConnector> cltsPools = new HashMap<String, SpdyHttpCltPoolConnector>();
	
	public SpdyHttpCltPoolConnector createCltConnector(String uriServer, int nbConnector, 
												RestHttpServlet notifier) throws RestHttpException {

		SpdyHttpCltPoolConnector pool = cltsPools.get(uriServer);
			
		if (null == pool) {
			pool = new SpdyHttpCltPoolConnector(uriServer, nbConnector, 1000, notifier);
		}
		 
		cltsPools.put(uriServer,pool);
		
		return pool;
	}
	
	public SpdyHttpCltConnector getCltConnector(String uriServer, RestHttpServlet notifier) {

		return cltsPools.get(uriServer).getConnector();
	}

	public void shutdown() {
		
		Set<Entry<String, SpdyHttpCltPoolConnector>> entries = cltsPools.entrySet();
		
		for (Entry<String, SpdyHttpCltPoolConnector> entry: entries) {
			entry.getValue().shutdown();
		}
	}
	
}
