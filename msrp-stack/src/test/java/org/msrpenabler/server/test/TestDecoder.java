package org.msrpenabler.server.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import org.msrpenabler.server.api.DefaultMsrpSession;
import org.msrpenabler.server.api.MsrpSessionsFactory;
import org.msrpenabler.server.cnx.MsrpAddrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;


public class TestDecoder extends TestCase {

	private static final Logger logger = LoggerFactory.getLogger(TestDecoder.class);
	
	protected void setUp() throws Exception {
	}

	protected void tearDown() throws Exception {
	}

	
	public void testDecoder() throws Exception {

		String msgCmd = "MSRP dkei38sd SEND\r\n" + "To-Path: ";
				//"To-Path: msrp://localhost:2855/kjhd37s2s20w2a;tcp\r\n"+
		String msgFollow = //"Hello";
				"From-Path: msrp://atlanta.example.com:7654/jshA7weztas;tcp\r\n"+
				"Message-ID: 4564dpWd\r\n" +
				"Byte-Range: 1-*/8\r\n" +
				"Content-Type: text/plain\r\n"+
				"\r\n"+
				"abcd\r\n"+
				"-------dkei38sd+\r\n"+
		
		"MSRP dkei38gd ";

		String msg2Start = "200 OK\r\n" + "To-Path: ";
				//To-Path: msrp://localhost:2855/kjhd37s2s20w2a;tcp\r\n"+
		String msg2Next = 
				"From-Path: msrp://atlanta.example.com:7654/jshA7weztas;tcp\r\n"+
				"-------dkei38gd$\r\n";
		
		try {
			
	        String host="localhost";
	        int port=2855;
	        
			logger.info("Start server");
	        //NioMsrpSockServerBootStrap myServ = new NioMsrpSockServerBootStrap(inetHost, 8080, null);
			MsrpSessionsFactory sessFactory = (MsrpSessionsFactory) MsrpSessionsFactory.getDefaultInstance();

			MsrpAddrServer addrServ = new MsrpAddrServer("msrp","localhost", 2855);
			sessFactory.cnxFactory.startServer(addrServ );
			
			logger.info("Start client");
	        EchoClient client = new EchoClient(host, port);	   
	        client.run();
	        
	        DefaultMsrpSession sessHd = new DefaultMsrpSession(sessFactory, "localhost", null, false, 30);

	        sessHd.setRemotePath("msrp://atlanta.example.com:7654/jshA7weztas;tcp");
	        
	        String toPath = sessHd.getLocalPath();
	        logger.info("Local path: {}", sessHd.getLocalPath());
	        
			sessHd.bind();
	        
//			LocalEcho localEcho = new LocalEcho("1");
//			
//			localEcho.run();
			
			Thread.sleep(1000);

			ByteBuf outBuff = Unpooled.buffer();
			
			outBuff.writeBytes(msgCmd.getBytes("UTF-8"));
			outBuff.writeBytes(toPath.getBytes("UTF-8"));
			outBuff.writeBytes("\r\n".getBytes("UTF-8"));
			outBuff.writeBytes(msgFollow.getBytes("UTF-8"));
			
			logger.info("Write to client");

			client.write(outBuff);

			outBuff = Unpooled.buffer();
			outBuff.writeBytes(msg2Start.getBytes("UTF-8"));
			outBuff.writeBytes(toPath.getBytes("UTF-8"));
			outBuff.writeBytes("\r\n".getBytes("UTF-8"));
			outBuff.writeBytes(msg2Next.getBytes("UTF-8"));
			
			client.write(outBuff);
			
			Thread.sleep(2000);
			
			client.shutDown();

			sessFactory.cnxFactory.shutdownAllServer();
	        
		} finally {
			;
		}
		
	}
	
}
