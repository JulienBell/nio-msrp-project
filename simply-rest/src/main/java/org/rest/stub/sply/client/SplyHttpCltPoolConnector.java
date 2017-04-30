package org.rest.stub.sply.client;

import io.netty.handler.codec.http.DefaultFullHttpResponse;

import java.util.ArrayList;


import java.util.concurrent.atomic.AtomicLong;

import org.enabler.restlib.RestHttpException;
import org.enabler.restlib.wrapper.RestStubFullHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SplyHttpCltPoolConnector {

    private static final Logger logger =
        LoggerFactory.getLogger(SplyHttpCltPoolConnector.class);
	
	final Integer nbConnection;
	final String uriServer;
	
	AtomicLong nextCnx = new AtomicLong(0);

	final ArrayList<SplyHttpCltConnector> listConnectors;
	
	protected SplyHttpCltPoolConnector(String uriServer, int nbConnection) throws RestHttpException {
		this.nbConnection = nbConnection;
		this.uriServer = uriServer;
		
		listConnectors = new ArrayList<SplyHttpCltConnector>(nbConnection);
		
		for (int i=0; i < nbConnection ;i++) {
			
			listConnectors.add(new SplyHttpCltConnector(uriServer));
			logger.info("Client connector created {}", i);
		}
		
	}
	
	public SplyHttpCltConnector getConnector() {
		
		int idCnx = (int) (nextCnx.getAndIncrement() % nbConnection);
		
		if (idCnx < 0) {
			idCnx = -idCnx;
			nextCnx.getAndSet(0);
		}
		if (idCnx >= nbConnection) {
			idCnx = 0;
			logger.error("idCnx returned {} >= nbCnx {}", idCnx, nbConnection);
		}
		
		return listConnectors.get(idCnx);
		
	}
	
	public DefaultFullHttpResponse handleCltRequest(RestStubFullHttpRequest req) throws RestHttpException {
		
		return getConnector().handleCltRequest(req);
		
	}


	public void shutdown() {

		for (SplyHttpCltConnector connector: listConnectors) {
			connector.shutdown();
		}
	}

		
	
}
