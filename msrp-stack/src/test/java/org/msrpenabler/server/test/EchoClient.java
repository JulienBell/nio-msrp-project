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
package org.msrpenabler.server.test;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * Sends one message when a connection is open and echoes back any received
 * data to the server.  Simply put, the echo client initiates the ping-pong
 * traffic between the echo client and server by sending the first message to
 * the server.
 */
public class EchoClient {

	private static final Logger logger = LoggerFactory.getLogger(EchoClient.class );
	
    private final String host;
    private final int port;
    
    Channel ch;
    ChannelFuture lastWriteFuture;
    EventLoopGroup group;

    public EchoClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void run() throws Exception {
        // Configure the client.
        group = new NioEventLoopGroup();
//        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSocketChannel.class)
             .option(ChannelOption.TCP_NODELAY, true)
             .handler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ch.pipeline().addLast(
                             new LoggingHandler(LogLevel.INFO),
                             new EchoClientHandler());
                 }
             });

            // Start the client.
            ChannelFuture f = b.connect(host, port).sync();

            ch = f.channel();
            
//            // Wait until the connection is closed.
//            f.channel().closeFuture().sync();
//        } finally {
//            // Shut down the event loop to terminate all threads.
//            group.shutdown();
//        }
    }

    
	public void write(ByteBuf data) throws IOException {
		
		logger.info("Start write buffer");
		
		lastWriteFuture = ch.writeAndFlush(data);
		
        if (lastWriteFuture != null) {
            lastWriteFuture.awaitUninterruptibly();
            lastWriteFuture=null;
        }
	}    
    
    public void shutDown() throws IOException {
    	
        // Read commands from the stdin.
//        System.out.println("Enter text (quit to end)");
//
//        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
//        
//    	for (;;) {
//            String line = in.readLine();
//            if (line == null || "quit".equalsIgnoreCase(line)) {
//                break;
//            }
//
//            // Sends the received line to the server.
//            //lastWriteFuture = ch.write(line);
//        }

//        // Wait until all messages are flushed before closing the channel.
//        if (lastWriteFuture != null) {
//            lastWriteFuture.awaitUninterruptibly();
//        }

        group.shutdownGracefully();
        
}
	
    public static void main(String[] args) throws Exception {
        // Print usage if no argument is specified.
        if (args.length < 2 || args.length > 3) {
            System.err.println(
                    "Usage: " + EchoClient.class.getSimpleName() +
                    " <host> <port> [<first message size>]");
            return;
        }

        // Parse options.
        final String host = args[0];
        final int port = Integer.parseInt(args[1]);


        new EchoClient(host, port).run();
    }
}
