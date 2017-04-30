package org.spdyhttp.stub.server;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.enabler.restlib.RestHttpException;
import org.enabler.restlib.RestHttpServlet;

public class SpdyRestSrvFactory {

	private static class ChargeurService {
		private static final SpdyRestSrvFactory instance = new SpdyRestSrvFactory();
	}

	public static SpdyRestSrvFactory getInstance() {
		return ChargeurService.instance;
	}
	
	HashMap<String, SpdyHttpSrvBind> serversPools = new HashMap<String, SpdyHttpSrvBind>();
	
	public SpdyHttpSrvBind getSrvBindConnector(String uriServer, RestHttpServlet servlet) throws RestHttpException {

		SpdyHttpSrvBind srvBind = serversPools.get(uriServer);
			
		if (null == srvBind) {
			try {
				srvBind = new SpdyHttpSrvBind(uriServer, 1000, servlet);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new RestHttpException("Failed to bind on "+uriServer,e);
			}
		}
		 
		serversPools.put(uriServer,srvBind);
		
		return srvBind;
	}
	

	public void shutdown() {
		
		Set<Entry<String, SpdyHttpSrvBind>> entries = serversPools.entrySet();
		
		for (Entry<String, SpdyHttpSrvBind> entry: entries) {
			entry.getValue().shutdown();
		}
	}
	
}
