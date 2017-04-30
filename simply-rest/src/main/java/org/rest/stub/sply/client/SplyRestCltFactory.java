package org.rest.stub.sply.client;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.enabler.restlib.RestHttpException;

public class SplyRestCltFactory {

	private static class ChargeurService {
		private static final SplyRestCltFactory instance = new SplyRestCltFactory();
	}

	public static SplyRestCltFactory getInstance() {
		return ChargeurService.instance;
	}
	
	HashMap<String, SplyHttpCltPoolConnector> cltsPools = new HashMap<String, SplyHttpCltPoolConnector>();
	
	public SplyHttpCltPoolConnector getOrCreateCltPoolConnector(String uriServer, int nbConnector) throws RestHttpException {

		SplyHttpCltPoolConnector pool = cltsPools.get(uriServer);
			
		if (null == pool) {
			pool = new SplyHttpCltPoolConnector(uriServer, nbConnector);
			cltsPools.put(uriServer,pool);
		}
		
		return pool;
	}
	
	public SplyHttpCltConnector getCltConnector(String uriServer) {

		if (cltsPools.get(uriServer) != null)	return cltsPools.get(uriServer).getConnector();
		
		return null;
	}

	public void shutdown() {
		
		Set<Entry<String, SplyHttpCltPoolConnector>> entries = cltsPools.entrySet();
		
		for (Entry<String, SplyHttpCltPoolConnector> entry: entries) {
			entry.getValue().shutdown();
		}
	}
	
}
