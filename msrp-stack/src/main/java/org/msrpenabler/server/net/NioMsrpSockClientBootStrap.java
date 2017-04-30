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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.msrpenabler.server.cnx.MsrpAddrServer;
import org.msrpenabler.server.cnx.MsrpConnexion;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * SockClient 
 */
public class NioMsrpSockClientBootStrap {

	
	private Bootstrap bootClt;
	EventLoopGroup workerGroup;
	MsrpAddrServer addrLocalServ;
	
    public NioMsrpSockClientBootStrap(EventLoopGroup workerGroup, MsrpConnexion cnx, MsrpAddrServer addrLocalServer ) throws Exception {
    	this.workerGroup = workerGroup;
    	this.addrLocalServ = addrLocalServer;
    	init(cnx);
    }

    public void init(final MsrpConnexion cnx) throws Exception {
    	
        // Configure the client.
        bootClt = new Bootstrap();
        bootClt = bootClt.group(workerGroup)
        .channel(NioSocketChannel.class)  
        .handler(new ChannelInitializer<SocketChannel>() {

        	// Create a new socket / channel / pipeline for each new Cnx
        	@Override
        	public void initChannel(SocketChannel ch) throws Exception {

        		// Set channel in msrpConnexion
        		cnx.setChannel(ch);

        		ch.pipeline().addLast(
        				new LoggingHandler(LogLevel.DEBUG),
        				
        				// Rcv decoder
        				new NioMsrpMsgDecoder(),
        				// Send encoder
        				new NioMsrpMsgEncoder(),

        				// Inbounds Msg Handler
        				new MsgRcvHandler(cnx)
        		);

        	}
        });

    }
    
     public ChannelFuture connect( InetAddress addr, int port, int connectTOms) {

    	 // Start the client.
    	 bootClt.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTOms);

    	 SocketAddress remoteSockAddr = new InetSocketAddress(addr, port);
    	 SocketAddress localSockAddr = new InetSocketAddress(addrLocalServ.inetAddr, 0);
		 
    	 return bootClt.connect(remoteSockAddr, localSockAddr);
    	 //return bootClt.connect(addr, port);
    }

	public void shutDown() {

		if (workerGroup != null) {
			workerGroup.shutdownGracefully();
		}
		
	}


    
}
