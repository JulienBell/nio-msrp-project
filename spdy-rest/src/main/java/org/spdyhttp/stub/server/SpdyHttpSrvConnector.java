package org.spdyhttp.stub.server;

import io.netty.channel.Channel;

import org.enabler.restlib.RestHttpServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdyhttp.stub.api.SpdyHttpConnector;

public class SpdyHttpSrvConnector extends SpdyHttpConnector {

    private static final Logger logger =
        LoggerFactory.getLogger(SpdyHttpSrvConnector.class);

    
    public SpdyHttpSrvConnector(RestHttpServlet notifier, Channel ch) {
		
		super(notifier, true, ch);
		
		logger.info("New connection accepted on server from {}", ch.remoteAddress());
		
	}

    
	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		logger.error("NOT IMPLEMENTED");
		
	}
    
}
