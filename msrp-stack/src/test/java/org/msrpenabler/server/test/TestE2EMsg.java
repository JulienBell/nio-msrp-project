package org.msrpenabler.server.test;

import java.net.InetAddress;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import org.msrpenabler.server.api.MsrpMessageData;
import org.msrpenabler.server.api.MsrpSessionHd;
import org.msrpenabler.server.api.MsrpSessionHdFuture;
import org.msrpenabler.server.api.MsrpSessionsFactory;
import org.msrpenabler.server.cnx.MsrpAddrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;


public class TestE2EMsg extends TestCase {

	private static final Logger logger = LoggerFactory.getLogger(TestE2EMsg.class);
	
	protected void setUp() throws Exception {
	}

	protected void tearDown() throws Exception {
	}

	
	public void testEncoder() throws Exception {

		
		try {
			
			MsrpSessionsFactory sessFactory = (MsrpSessionsFactory) MsrpSessionsFactory.getDefaultInstance();

			logger.info("Start server");

			InetAddress inetHost = InetAddress.getLocalHost();
			logger.info("Start server on {} ", inetHost.getHostAddress());
			
			MsrpAddrServer addrServ = new MsrpAddrServer("msrp",inetHost, 2856);
			sessFactory.cnxFactory.startServer(addrServ );

	        //MsrpSessionHdImpl sessServHd = new MsrpSessionHdImpl(sessFactory, null, false);
	        MsrpSessionHd sessServHd = sessFactory.createSimpleSession(inetHost,null,false, 30);

	        
	        //String fromPath = sessHd.getLocalPath();
	        logger.info("Local path: {}", sessServHd.getLocalPath());

			
			logger.info("Init client");
	        MsrpSessionHd sessCltHd = sessFactory.createSimpleSession(inetHost,null,false, 30);


			logger.info("Bind server");
	        sessServHd.setRemotePath(sessCltHd.getLocalPath());
	        logger.info("Server path: {}", sessServHd.getLocalPath());

	        MsrpSessionHdFuture futB = sessServHd.bind();

	        
			logger.info("Connect client");
	        sessCltHd.setRemotePath(sessServHd.getLocalPath());
	        logger.info("Client path: {}", sessCltHd.getLocalPath());

	        sessCltHd.connect(5).sync();
	        

			Thread.sleep(300);

	        
	        // Send Msg from client to server
	        ByteBuf buffClt = Unpooled.buffer();
	        
	        buffClt.writeBytes( "This is a test !".getBytes() );
	        
			MsrpMessageData msrpMessage = MsrpSessionsFactory.createMsg("text/plain", buffClt);

			sessCltHd.sendMsrpMessage(msrpMessage);
	        
			
			//Thread.sleep(300);

			futB.sync();
			
			// Send Msg from server to client
	        ByteBuf buffSrv = Unpooled.buffer();
	        
	        buffSrv.writeBytes( "Server is here !".getBytes() );
	        
			msrpMessage = MsrpSessionsFactory.createMsg("text/plain", buffSrv);
	        
			sessServHd.sendMsrpMessage(msrpMessage);
	        
	        
			Thread.sleep(1000);
			
			// Buffer are released after notify response 
			try {
				buffClt.release();
			}
			catch (Exception e) {
				logger.error("Failed to release buffer ");
			}
			try {
				buffSrv.release();
			}
			catch (Exception e) {
				logger.error("Failed to release buffer ");
			}

			
			sessCltHd.close();

			Thread.sleep(2000);

			try {
				sessServHd.close();
			}
			catch (Exception e) {
				logger.error("Failed to close sesssion: {}, msg: {} ", sessServHd.getLocalPath(), e.getMessage());
			}
			

			sessFactory.cnxFactory.shutdownAllServer();
	        

		}
		catch (Exception e ) {
			logger.warn("Failed : {}", e.getMessage(), e);
		}
		finally {
			;
		}
		
	}
	
}
