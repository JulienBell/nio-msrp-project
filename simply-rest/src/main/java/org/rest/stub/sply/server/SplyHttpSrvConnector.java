package org.rest.stub.sply.server;

import io.netty.channel.Channel;

import org.enabler.restlib.RestHttpServlet;
import org.rest.stub.sply.api.SplyHttpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SplyHttpSrvConnector extends SplyHttpConnector {

    private static final Logger logger =
        LoggerFactory.getLogger(SplyHttpSrvConnector.class);

    
    public SplyHttpSrvConnector(RestHttpServlet notifier, Channel ch) {
		
		super(notifier, true, ch);
		
		logger.info("New connection accepted on server from {}", ch.remoteAddress());
		
	}


	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		logger.error("NOT IMPLEMENTED");
		
	}

}
