/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.msrpenabler.server.net;

import java.io.IOException;
import org.msrpenabler.server.cnx.MsrpAddrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * Echoes back any received data from a client.
 */
public class NioMsrpSockServerBootStrap {

    private static final Logger logger =
        LoggerFactory.getLogger(NioMsrpSockServerBootStrap.class);

    private final MsrpAddrServer addrServ;
    private ChannelFuture futureBind=null;

    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    
    public NioMsrpSockServerBootStrap(MsrpAddrServer addrServ) {

    	this.addrServ = addrServ;
    }

    public void run() throws Exception {
        
    	// Configure the server.
        	
        ServerBootstrap b = new ServerBootstrap();
        b = b.group(bossGroup, workerGroup)
        		 .channel(NioServerSocketChannel.class)
            	 .option(ChannelOption.SO_BACKLOG, 500)  // maximum queue length sock waiting to be accepted
       	 		 .handler(new LoggingHandler(LogLevel.INFO))
            	 .childHandler(new ChannelInitializer<SocketChannel>() {
            	
            	// Create a new socket / channel / pipeline for each new Cnx
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {

                	ch.pipeline().addLast( new LoggingHandler(LogLevel.DEBUG) );

                	ch.pipeline().addLast( new NioMsrpMsgDecoder() );
                	ch.pipeline().addLast( new NioMsrpMsgEncoder() );
                	ch.pipeline().addLast( "msrpHandler", new MsgRcvHandler(addrServ) );
                	
                 }
             });

			// Start the server.
            futureBind = b.bind(addrServ.inetAddr, addrServ.port);
            
            futureBind.addListener( new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) {
                	if (future.isSuccess()) {
                		logger.info(" Bind success on {}:{}", addrServ.inetAddr, addrServ.port);
                	}
                	else {
                		logger.error(" Bind failed on {}:{}", addrServ.inetAddr,  addrServ.port, future.cause());
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
    
    public void shutDown() throws IOException {
    	
        // Shut down all event loops to terminate all threads.
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
        
    }
    
}
