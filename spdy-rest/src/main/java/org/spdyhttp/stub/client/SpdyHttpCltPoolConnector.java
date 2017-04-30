package org.spdyhttp.stub.client;

import io.netty.handler.codec.http.DefaultFullHttpResponse;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;


import org.enabler.restlib.RestHttpException;
import org.enabler.restlib.RestHttpServlet;
import org.enabler.restlib.wrapper.RestStubFullHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpdyHttpCltPoolConnector {

    private static final Logger logger =
        LoggerFactory.getLogger(SpdyHttpCltPoolConnector.class);
	
	final Integer nbConnection;
	final String uriServer;
	
	final AtomicInteger nextCnx=new AtomicInteger(0);

	final ArrayList<SpdyHttpCltConnector> listConnectors;
	
	protected SpdyHttpCltPoolConnector(String uriServer, int nbConnection, int maxSpdyFrames, 
										RestHttpServlet notifier) throws RestHttpException {
		this.nbConnection = nbConnection;
		this.uriServer = uriServer;
		
		listConnectors = new ArrayList<SpdyHttpCltConnector>(nbConnection);
		
		for (int i=0; i < nbConnection ;i++) {
			
			listConnectors.add(new SpdyHttpCltConnector(uriServer, maxSpdyFrames, notifier, 10));
			logger.info("Client connector created {}", i);
		}
		
	}
	
	public SpdyHttpCltConnector getConnector() {
		
		int idCnx;

		idCnx = nextCnx.getAndIncrement();

		return listConnectors.get(idCnx);
		
	}
	
	public DefaultFullHttpResponse handleCltRequest(RestStubFullHttpRequest req) throws RestHttpException {
		
		int idCnx;

        idCnx = nextCnx.getAndIncrement();
		
		return listConnectors.get(idCnx).handleCltRequest(req);
		
	}


	public void shutdown() {

		for (SpdyHttpCltConnector connector: listConnectors) {
			connector.shutdown();
		}
	}

		
	
}
