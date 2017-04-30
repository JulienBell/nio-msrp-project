package org.rest.stub.sply.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import java.net.URI;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SplyHttpClientSock {


    private static final Logger logger =
        LoggerFactory.getLogger(SplyHttpClientSock.class);

    private final URI uriServer;
    private ChannelFuture futureSock=null;
    
    public static Pattern pattUriServer = Pattern.compile("(https?)://([^:]*):(\\d*)/.*");

    private final int port;
    private final String ipHost;
    
    //private boolean isConnected = false;
    private volatile boolean isConnected = false;
    private Bootstrap clt=null;

    static EventLoopGroup workerGroup;
    
    final SplyHttpCltConnector cltHandler;
    private Channel channel=null;
    
	public SplyHttpClientSock( String addr,	SplyHttpCltConnector connectorHandler) throws Exception {

    	this.uriServer = new URI(addr);
    	cltHandler = connectorHandler;
    	
    	port = uriServer.getPort();
    	ipHost = uriServer.getHost();
    	
    	if (workerGroup == null){
    		workerGroup = new NioEventLoopGroup();
    	}
    	init();
	}

	public URI getUriServer() {
		return uriServer;
	}

	private class InitChannel extends ChannelInitializer<SocketChannel> {
    	
    	// Create a new socket / channel / pipeline for each new Cnx
         @Override
         public void initChannel(SocketChannel ch) throws Exception {

        	//ch.pipeline().addLast( new LoggingHandler(LogLevel.DEBUG) );
        	
        	ch.pipeline().addLast( new HttpClientCodec() );

        	//ch.pipeline().addLast( new LoggingHandler(LogLevel.INFO) );
        	
        	ch.pipeline().addLast("aggregator", new HttpObjectAggregator(65536));

            ch.pipeline().addLast( "pipelinedCltHandler", new SplyPipelinedCltHandler() );
        	
        	ch.pipeline().addLast( "msrpHandler", cltHandler );
         }
     }
	
	private InitChannel initChannel = new InitChannel();
	
    public void init() throws Exception {
        
    	// Create the socket.
        	
        clt = new Bootstrap().group(workerGroup)
        		 .channel(NioSocketChannel.class)
       	 		 .handler(initChannel);
        
        	// Start connection
			//setConnected(true);
			//connect();
    }

	public synchronized Channel connect() {
		
		// Start the connexion to the server.
        logger.info("Test connect on {}, {}", ipHost, port );

		if (isConnected) {
			if ( channel == null ) {
				logger.error("Return null channel {} ??", channel);
			}
			return channel;
		}

		logger.info("Start connect on {}, {}", ipHost, port );
		
		clt.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
		
		futureSock = clt.connect(ipHost, port);
		
		channel = futureSock.channel();

		logger.info("channel {}:", ipHost, port );

		// Async connection is too complex to manage, has it's not possible to write on cnx before it's connected
		// it's easier to synchronize all requester during the connection delay
		
//        futureSock.addListener( new ChannelFutureListener() {
//		    public void operationComplete(ChannelFuture future) {
//
//				if (future.isSuccess()) {
//		    		logger.info(" Connection success on {}:{}", ipHost, port);
//		    		
//		    		setConnected(true);
//		    		
//		    		// Add CB to reset cn status on channel closure
//		    		ChannelFutureListener closeCB = new ChannelFutureListener() {
//
//						public void operationComplete(ChannelFuture arg0)
//								throws Exception {
//				    		logger.info(" Connection closure on {}:{}", ipHost, port);
//							setConnected(false);
//							futureSock=null;
//						}
//		    		};
//		    		
//					future.channel().closeFuture().addListener(closeCB);
//		    	}
//		    	else {
//		    		logger.error(" Connection failed on {}:{}", ipHost,  port);
//		    		setConnected(false);
//		    	}
//				
//				// Memory barrier on volatile tryConnection 
//				tryConnection=false;
//		   }
//		});
		
       futureSock.syncUninterruptibly();
		logger.info(" Connection waiting ended on {}:{}", ipHost,  port);
		
		if (futureSock.isSuccess()) {
    		setConnected(true);
    		
    		// Add CB to reset cn status on channel closure
    		ChannelFutureListener closeCB = new ChannelFutureListener() {

				public void operationComplete(ChannelFuture arg0)
						throws Exception {
		    		logger.info(" Connection closure on {}:{}", ipHost, port);
					setConnected(false);
					futureSock=null;
				}
    		};
    		
    		futureSock.channel().closeFuture().addListener(closeCB);
    		
		}
		else {
			logger.warn(" Connection failed {}:{}, cause ", ipHost,  port, futureSock.cause());
			futureSock = null;
			channel = null;
		}
		
		return channel;
		
	}

	public void waitEndChannel() throws InterruptedException {
		// Wait until the server socket is closed.
		logger.info("waitEndChannel ");
		
		if (futureSock != null) {
			futureSock.channel().closeFuture().sync();
		}
	}

	public Channel getChannel()  {
		boolean isCon = isConnected;
		if (! isCon ) {
			return connect();
		}

		if (channel == null) {
			logger.error("returned channel is {}", channel);
			logger.error(" status connected before {}", isCon);
			logger.error(" status connected now {}", isConnected);
		}
		
		return channel;
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
