package org.rest.stub.sply.server;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.enabler.restlib.RestHttpException;
import org.enabler.restlib.RestHttpServlet;

public class SplyRestSrvFactory {

	private static class ChargeurService {
		private static final SplyRestSrvFactory instance = new SplyRestSrvFactory();
	}

	public static SplyRestSrvFactory getInstance() {
		return ChargeurService.instance;
	}
	
	HashMap<String, SplyHttpSrvBind> serversPools = new HashMap<String, SplyHttpSrvBind>();
	
	public SplyHttpSrvBind getSrvBindConnector(String uriServer, RestHttpServlet servlet) throws RestHttpException {

		SplyHttpSrvBind srvBind = serversPools.get(uriServer);
			
		if (null == srvBind) {
			try {
				srvBind = new SplyHttpSrvBind(uriServer, 1000, servlet);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new RestHttpException("Failed to bind on "+uriServer,e);
			}

			serversPools.put(uriServer,srvBind);
			
		}
		 
		return srvBind;
	}
	

	public void shutdown() {
		
		Set<Entry<String, SplyHttpSrvBind>> entries = serversPools.entrySet();
		
		for (Entry<String, SplyHttpSrvBind> entry: entries) {
			entry.getValue().shutdown();
		}
	}
	
}
