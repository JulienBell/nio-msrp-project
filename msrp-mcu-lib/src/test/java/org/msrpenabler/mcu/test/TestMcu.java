package org.msrpenabler.mcu.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.net.InetAddress;
import org.junit.Ignore;
import org.junit.Test;
import org.msrpenabler.mculib.cnf.CnfGlobalListener;
import org.msrpenabler.mculib.cnf.ConferenceUnit;
import org.msrpenabler.mculib.cnf.ConferencesFactory;
import org.msrpenabler.server.api.EnumSessionOptions;
import org.msrpenabler.server.api.InfoLogMsrpSessionListener;
import org.msrpenabler.server.api.MsrpChunkData;
import org.msrpenabler.server.api.MsrpMessageData;
import org.msrpenabler.server.api.MsrpSessionHd;
import org.msrpenabler.server.api.MsrpSessionHdFuture;
import org.msrpenabler.server.api.MsrpSessionListener;
import org.msrpenabler.server.api.MsrpSessionsFactory;
import org.msrpenabler.server.cnx.MsrpAddrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

public class TestMcu extends TestCase {
	
	private static final Logger logger = LoggerFactory.getLogger(TestMcu.class);
	
	public int cptThd;

	ConferencesFactory cnfFact;

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	
	public class TestThread extends Thread {
		

		private String id;
		private TestMcu parent;


		protected TestThread(String id, TestMcu parent) {

			this.id = id;
			this.parent = parent;
		}

		
		public void run() {

			System.out.println("Reponse on id "+ id );
			parent.cptThd--;
		}
	}
	
	@Test
	//@Ignore
	public void testMcu() {
		

		MsrpSessionsFactory sessFactory = (MsrpSessionsFactory) MsrpSessionsFactory.getDefaultInstance();

		logger.info("Start server");

		class MyMsrpSessionListener extends InfoLogMsrpSessionListener {

			public MyMsrpSessionListener(String name) {
				super(name);
			}

			@Override
			public void evtRcvMessage(MsrpMessageData msrpContent,
					boolean wasChunked) {

				super.evtRcvMessage(msrpContent, wasChunked);
				
//				if (getName().equals("[CLT] ")) {
//					try {
//						getSessionHd().close();
//					} catch (Exception e) {
//						logger.error(getName()+"Failed on close session : {}", getSessionHd().getSessionId());
//					}
//				}
			}
		}
		
		try {

			MsrpSessionListener listenerSrv = new MyMsrpSessionListener("SRV");
			MsrpSessionListener listenerClt1 = new MyMsrpSessionListener("CLT_1");
			MsrpSessionListener listenerClt2 = new MyMsrpSessionListener("CLT_2");
			MsrpSessionListener listenerClt3 = new MyMsrpSessionListener("CLT_3");

			System.out.println("Start Server");

			InetAddress inetHost = InetAddress.getLocalHost();

			logger.info("Start server on {} ", inetHost.getHostAddress());

			MsrpAddrServer addrServ = new MsrpAddrServer("msrp",inetHost, 2855);
			sessFactory.cnxFactory.startServer(addrServ );


			System.out.println("create msrp sess1 on server");

			MsrpSessionHd sessServHd1 = sessFactory.createSimpleSession(inetHost,listenerSrv,false, 30);
			sessServHd1.setOption(EnumSessionOptions.SESS_NOTIFY_FULL_RESPONSE);

			logger.info("Local path sessServ 1: {}", sessServHd1.getLocalPath());



			System.out.println("create msrp sess2 on server");

			MsrpSessionHd sessServHd2 = sessFactory.createSimpleSession(inetHost,listenerSrv,false, 30);
			sessServHd2.setOption(EnumSessionOptions.SESS_NOTIFY_FULL_RESPONSE);

			logger.info("Local path sessServ 2: {}", sessServHd2.getLocalPath());

			System.out.println("create msrp sess3 on server");
			MsrpSessionHd sessServHd3 = sessFactory.createSimpleSession(inetHost,listenerSrv,false, 30);
			sessServHd3.setOption(EnumSessionOptions.SESS_NOTIFY_FULL_RESPONSE);

			logger.info("Local path sessServ 3: {}", sessServHd3.getLocalPath());


			System.out.println("create conference on server");
			ConferencesFactory cnfFactory = ConferencesFactory.getDefaultInstance();

			ConferenceUnit cnf = cnfFactory.createConference();

			CnfGlobalListener cnfListener = new CnfGlobalListener() {

				@Override
				public void evtRcvMsrpChunk(ConferenceUnit confHub,
						MsrpSessionHd fromSessMsrp, MsrpChunkData msrpChunk) {
					logger.info("Message Chunck received on Conference: {}", msrpChunk.content().toString());
				}

				@Override
				public void evtRcvMessage(ConferenceUnit confHub,
						MsrpSessionHd fromSessMsrp, MsrpMessageData msrpContent,
						boolean wasChunked) {
					logger.info("Message received on Conference: {}", msrpContent.content().toString());
				}
			};

			cnf.setGlobalListener(cnfListener);


			System.out.println("Attach msrp sessions on conference");

			cnf.attachSession(sessServHd1);
			cnf.attachSession(sessServHd2);
			cnf.attachSession(sessServHd3);


			System.out.println("Start client msrp sess1 ");
			MsrpSessionHd sessCltHd1 = sessFactory.createSimpleSession(inetHost,listenerClt1,false, 30);

			sessCltHd1.setOption(EnumSessionOptions.SESS_NOTIFY_FULL_RESPONSE);

			logger.info("Bind server 1");
			sessServHd1.setRemotePath(sessCltHd1.getLocalPath());
			logger.info("Server path: {}", sessServHd1.getLocalPath());

			MsrpSessionHdFuture futSrv1 = sessServHd1.bind();

			logger.info("Connect client 1");
			sessCltHd1.setRemotePath(sessServHd1.getLocalPath());
			logger.info("Client 1 path: {}", sessCltHd1.getLocalPath());

			MsrpSessionHdFuture futClt1 = sessCltHd1.connect(5).sync();
			logger.info("Connect client 1 is connected ");

			futSrv1.sync();
			logger.info("Sess Srv 1 is connected ");


			System.out.println("Start client msrp sess2 ");
			MsrpSessionHd sessCltHd2 = sessFactory.createSimpleSession(inetHost,listenerClt2,false, 30);

			sessCltHd2.setOption(EnumSessionOptions.SESS_NOTIFY_FULL_RESPONSE);

			logger.info("Bind server 2");
			sessServHd2.setRemotePath(sessCltHd2.getLocalPath());
			logger.info("Server path: {}", sessServHd2.getLocalPath());

			MsrpSessionHdFuture futSrv2 = sessServHd2.bind();

			logger.info("Connect client 2");
			sessCltHd2.setRemotePath(sessServHd2.getLocalPath());
			logger.info("Client 2 path: {}", sessCltHd2.getLocalPath());

			sessCltHd2.connect(5).sync();
			logger.info("Connect client 2 is connected ");

			futSrv2.sync();
			logger.info("Sess Srv 2 is connected ");


			System.out.println("Start client msrp sess 3 ");
			MsrpSessionHd sessCltHd3 = sessFactory.createSimpleSession(inetHost,listenerClt3,false, 30);

			sessCltHd3.setOption(EnumSessionOptions.SESS_NOTIFY_FULL_RESPONSE);

			logger.info("Bind server 3");
			sessServHd3.setRemotePath(sessCltHd3.getLocalPath());
			logger.info("Server path: {}", sessServHd3.getLocalPath());

			MsrpSessionHdFuture futSrv3 = sessServHd3.bind();

			logger.info("Connect client 3");
			sessCltHd3.setRemotePath(sessServHd3.getLocalPath());
			logger.info("Client 3 path: {}", sessCltHd3.getLocalPath());

			sessCltHd3.connect(5).sync();
			logger.info("Connect client 3 is connected ");

			futSrv3.sync();
			logger.info("Sess Srv 3 is connected ");

	        ByteBuf buffClt = Unpooled.buffer();
	        ByteBuf buffClt2 = Unpooled.buffer();
	        
	        if (!futClt1.isSuccess()) {
	        	logger.error("Connexion failure on client side");
	        }
	        else {
	        
	        	// Send Msg from client to server
	        	buffClt.writeBytes( "This is".getBytes() );

	        	String msgId = MsrpSessionsFactory.createMessageId();

	        	MsrpChunkData msrpChunk = MsrpSessionsFactory.createChunkMsg(msgId, "text/plain", buffClt,
	        			1, buffClt.readableBytes(), 16);

	        	sessCltHd1.sendMsrpChunk(msrpChunk, true);

	        	buffClt2.writeBytes( " a test !".getBytes() );

	        	msrpChunk = MsrpSessionsFactory.createChunkMsg(msgId, "text/plain", buffClt2,
	        			8, buffClt2.readableBytes()+8-1, 16);

	        	sessCltHd1.sendMsrpChunk(msrpChunk, true);
	        }

	        
			Thread.sleep(3000);

	        
	        try {
				sessCltHd1.close();
				sessCltHd2.close();
				sessServHd3.close();

				Thread.sleep(100);
			}
			catch (Exception e) {
				logger.error("Failed to close sesssion: ", e);
			}
				
			try {
				sessServHd1.close();
			}
			catch (Exception e) {
				logger.error("Failed to close sesssion: {}, msg: {} ", sessServHd1.getLocalPath(), e.getMessage());
			}
			try {
				sessServHd2.close();
			}
			catch (Exception e) {
				logger.error("Failed to close sesssion: {}, msg: {} ", sessServHd2.getLocalPath(), e.getMessage());
			}

			try {
				sessCltHd3.close();
			}
			catch (Exception e) {
				logger.error("Failed to close sesssion: {}, msg: {} ", sessCltHd3.getLocalPath(), e.getMessage());
			}
				

			sessFactory.cnxFactory.shutdownAllServer();
	        
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		

//		TestThread thd1 = new TestThread(cnx, "A", this);
//		TestThread thd2 = new TestThread(cnx, "B", this);
//		TestThread thd3 = new TestThread(cnx, "C", this);
//		
//		cptThd=3;
//		
//		thd1.start();
//		thd2.start();
//		thd3.start();
//		
//		while (cptThd > 0){
//
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//
//		}

		}
		catch (Exception e ) {
			logger.warn("Failed : {}", e.getMessage(), e);
		}
		
		System.out.println("End .............");
	}

	@Ignore
	public void testSrvShutdown() {
		

	}
	
}
