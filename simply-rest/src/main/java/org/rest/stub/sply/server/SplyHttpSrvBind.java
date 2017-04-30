package org.rest.stub.sply.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.enabler.restlib.RestHttpServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SplyHttpSrvBind {


    private static final Logger logger =
        LoggerFactory.getLogger(SplyHttpSrvBind.class);

    //private final String addrServ;
    private ChannelFuture futureBind=null;
    
    public static Pattern pattUriServer = Pattern.compile("(https?)://([^:]*):(\\d*)(/.*)*");

    private final RestHttpServlet servlet;
    private final int port;
    private final String ipHost;

    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    
	public SplyHttpSrvBind(String addrServ, int maxFrame, RestHttpServlet servlet) throws Exception {

    	//this.addrServ = addrServ;
    	this.servlet = servlet;
    	
    	Matcher matcher = pattUriServer.matcher(addrServ);
		
    	if (matcher.matches()) {
    		port = Integer.parseInt( matcher.group(3) );
    		ipHost = matcher.group(2);
    	}
    	else {
    		port = 8080;
    		ipHost = "127.0.0.1";
    	}
    	
    	init();
	}

    public void init() throws Exception {
        
    	// Configure the server.
        	
        ServerBootstrap b = new ServerBootstrap();
        b = b.group(bossGroup, workerGroup)
        		 .channel(NioServerSocketChannel.class)
            	 .option(ChannelOption.SO_BACKLOG, 50)  // maximum queue length sock waiting to be accepted
       	 		 .handler(new LoggingHandler(LogLevel.INFO))
            	 .childHandler(new ChannelInitializer<SocketChannel>() {
            	
            	// Create a new socket / channel / pipeline for each new Cnx
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {

                	//ch.pipeline().addLast( new LoggingHandler(LogLevel.INFO) );
                	
                    ch.pipeline().addLast( new HttpServerCodec() );
                	ch.pipeline().addLast("aggregator", new HttpObjectAggregator(65536));

                    ch.pipeline().addLast( new SplyPipelinedSrvHandler() );
                	
                	//ch.pipeline().addLast( new LoggingHandler(LogLevel.INFO) );

                	ch.pipeline().addLast( "msrpHandler", new SplyHttpSrvConnector(servlet, ch) );
                 }
             });

			// Start the server.
            futureBind = b.bind(ipHost, port);
            
            futureBind.addListener( new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) {
                	if (future.isSuccess()) {
                		logger.info(" Bind success on {}:{}", ipHost, port);
                	}
                	else {
                		logger.error(" Bind failed on {}:{}", ipHost,  port);
                	}
               }
            });
    }

	public void waitEndChannelBind() throws InterruptedException {
		// Wait until the server socket is closed.
		logger.info("waitEndChannelBind ");
		
		futureBind.channel().closeFuture().sync();
	}

	public Channel getChannelBind()  {
		if (futureBind != null) {
			return futureBind.channel();
		}
		return null;
	}
    

	public void shutdown() {
		
        // Shut down all event loops to terminate all threads.
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
	}


}
