package org.spdyhttp.stub.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.spdy.SpdyFrameCodec;
import io.netty.handler.codec.spdy.SpdyHttpDecoder;
import io.netty.handler.codec.spdy.SpdyHttpEncoder;
import io.netty.handler.codec.spdy.SpdySessionHandler;
import io.netty.handler.codec.spdy.SpdyVersion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpdyHttpClientSock {


    private static final Logger logger =
        LoggerFactory.getLogger(SpdyHttpClientSock.class);

    private final String addrServ;
    private ChannelFuture futureSock=null;
    
    public static Pattern pattUriServer = Pattern.compile("(https?)://([^:]*):(\\d*)/.*");

    private final int port;
    private final String ipHost;
    
    private boolean isConnected = false;
    private Bootstrap clt=null;

    static EventLoopGroup workerGroup;
    
    final SpdyHttpCltConnector cltHandler;
    
	public SpdyHttpClientSock( String addr, int maxFrame, 
								SpdyHttpCltConnector connectorHandler) throws Exception {

    	this.addrServ = addr;
    	cltHandler = connectorHandler;
    	
    	Matcher matcher = pattUriServer.matcher(addrServ);
		
    	if (matcher.matches()) {
    		port = Integer.parseInt( matcher.group(3) );
    		ipHost = matcher.group(2);
    	}
    	else {
    		port = 8080;
    		ipHost = "127.0.0.1";
    	}
    	
    	if (workerGroup == null){
    		workerGroup = new NioEventLoopGroup();
    	}
    	init();
	}

	private class InitChannel extends ChannelInitializer<SocketChannel> {
    	
    	// Create a new socket / channel / pipeline for each new Cnx
         @Override
         public void initChannel(SocketChannel ch) throws Exception {

         	ch.pipeline().addLast( new SpdyFrameCodec(SpdyVersion.SPDY_3_1) ) ;
//        	ch.pipeline().addLast( new SpdyFrameDecoder(SpdyVersion.SPDY_3_1, new SpdyRestFrameDecDelegate()) ) ;
//        	ch.pipeline().addLast( new SpdyFrameEncoder(SpdyVersion.SPDY_3_1) ) ;

        	ch.pipeline().addLast( new SpdySessionHandler(SpdyVersion.SPDY_3_1, false) ) ;
        	
        	ch.pipeline().addLast( new SpdyHttpDecoder(SpdyVersion.SPDY_3_1, 5*1024*1024) );
        	//ch.pipeline().addLast("aggregator", new HttpObjectAggregator(65536));

        	//ch.pipeline().addLast( new LoggingHandler(LogLevel.INFO) );

        	ch.pipeline().addLast( new SpdyHttpEncoder(SpdyVersion.SPDY_3_1) );

        	ch.pipeline().addLast( "msrpHandler", cltHandler );
         }
     }
    public void init() throws Exception {
        
    	// Create the socket.
        	
        clt = new Bootstrap().group(workerGroup)
        		 .channel(NioSocketChannel.class)
       	 		 .handler(new InitChannel());
        
        	// Start connection
			setConnected(true);
			connect();
    }

	private void connect() {
		// Start the server.
		logger.info("Start connect on {}, {}", ipHost, port );
		futureSock = clt.connect(ipHost, port);
		
		futureSock.addListener( new ChannelFutureListener() {
		    public void operationComplete(ChannelFuture future) {
		    	if (future.isSuccess()) {
		    		logger.info(" Connection success on {}:{}", ipHost, port);
		    		setConnected(true);
		    	}
		    	else {
		    		logger.error(" Connection failed on {}:{}", ipHost,  port);
		    		setConnected(false);
		    	}
		   }
		});
	}

	public void waitEndChannel() throws InterruptedException {
		// Wait until the server socket is closed.
		logger.info("waitEndChannel ");
		
		futureSock.channel().closeFuture().sync();
	}

	public Channel getChannel()  {
		if (! isConnected ) {
			connect();
		}
		if (futureSock != null) {
			return futureSock.channel();
		}
		logger.warn("Failed on retrieve Future sock channel ");
		return null;
	}
    

	public void shutdown() {
		
        // Shut down all event loops to terminate all threads.
        workerGroup.shutdownGracefully();
        
        workerGroup=null;
	}

	public void setConnected(boolean isConnected) {
		this.isConnected = isConnected;
	}

	public boolean isConnected() {
		return isConnected;
	}


}
