package org.msrpenabler.server.test;

import java.net.InetAddress;
import java.util.List;

import org.msrpenabler.server.api.DisconnectReason;
import org.msrpenabler.server.api.EnumSessionOptions;
import org.msrpenabler.server.api.MsrpChunkData;
import org.msrpenabler.server.api.MsrpMessageData;
import org.msrpenabler.server.api.MsrpResponseData;
import org.msrpenabler.server.api.MsrpReportData;
import org.msrpenabler.server.api.MsrpSessionHd;
import org.msrpenabler.server.api.MsrpSessionHdFuture;
import org.msrpenabler.server.api.MsrpSessionListener;
import org.msrpenabler.server.api.MsrpSessionsFactory;
import org.msrpenabler.server.cnx.MsrpAddrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;


public class TestInactivityTO extends TestCase {

	private static final Logger logger = LoggerFactory.getLogger(TestInactivityTO.class);
	
	protected void setUp() throws Exception {
	}

	protected void tearDown() throws Exception {
	}

	
	public void testInactTimeout() throws Exception {

		
		try {
			
			MsrpSessionsFactory sessFactory = (MsrpSessionsFactory) MsrpSessionsFactory.getDefaultInstance();

			class MyMsrpSessionListener extends MsrpSessionListener {

				private String prefixe;
				//int cpt=0;

				MyMsrpSessionListener(String str) {
					this.prefixe = "["+str+"] ";
				}
				
				@Override
				public void evtSessConnect() {
					logger.info(prefixe+"EVT SessConnect ");
				}

				@Override
				public void evtSessDisconnect(DisconnectReason disconnectReason) {
					logger.info(prefixe+"EVT SessDisconnect {}", disconnectReason);
				}

				@Override
				public void evtSessDisconnect( DisconnectReason disconnectReason,
						List<MsrpMessageData> listMsgFailed) {
					logger.info(prefixe+"EVT SessDisconnect {}", disconnectReason);
					
					if (listMsgFailed != null) {

						for (MsrpMessageData msg: listMsgFailed) {
							logger.info(prefixe+"EVT SessDisconnect msg {}", msg.toString());
						}
					}
				}

				@Override
				public void evtRcvMessage(MsrpMessageData msrpContent,
						boolean wasChunked) {
					logger.info(prefixe+"EVT evtRcvMessage, content: {}", msrpContent.content().toString());
					logger.info(prefixe+"EVT evtRcvMessage, content: {}", msrpContent.toString());
					logger.info(prefixe+"EVT evtRcvMessage, wasChunked : {}", wasChunked);
				
					if (prefixe.equals("[CLT] ")) {
						try {
							getSessionHd().close();
						} catch (Exception e) {
							logger.error(prefixe+"Failed on close session : {}", getSessionHd().getSessionId());
						}
					}
					//cpt++;
				}

				@Override
				public void evtSendReportRcv(MsrpReportData msrpContent) {
					logger.info(prefixe+"EVT evtSendReportRcv, msg {}", msrpContent);
				}

				@Override
				public void evtSendMsgSuccess(MsrpMessageData msrpContent) {
					logger.info(prefixe+"EVT evtSendMsgSuccess, msg {}", msrpContent);
				}

				@Override
				public void evtSendMsgFailure(MsrpMessageData msg) {
					logger.info(prefixe+"EVT evtSendMsgFailure ");
					logger.info(prefixe+"EVT SessDisconnect msg {}", msg.toString());
				}

				@Override
				public void evtRcvMsrpChunk(MsrpChunkData msrpChunk) {
					logger.info(prefixe+"EVT evtRcvMsrpChunk {}", msrpChunk);
				}

				@Override
				public void evtRcvAbortMsrpChunck(MsrpChunkData msrpChunk) {
					logger.info(prefixe+"EVT evtRcvAbortMsrpChunck {}", msrpChunk);
				}

				@Override
				public void evtSendChunkedMsgFailure(MsrpChunkData msrpChunk) {
					logger.info(prefixe+"EVT evtSendChunkedMsgFailure ");
				}

				@Override
				public void evtRcvResponse(MsrpResponseData respMsg) {
					logger.info(prefixe+"EVT evtRcvResponse: {}, {}", respMsg.getMessageId(), respMsg.getStatus());
					logger.info(prefixe+"EVT evtRcvResponse: initial full msg {}", respMsg.getAssociatedMessageData());
					
				}

				@Override
				public void evtRcvChunckResponse(MsrpResponseData respMsg) {
					logger.info(prefixe+"EVT evtRcvChunckResponse: {}, {}", respMsg.getMessageId(), respMsg.getStatus());
					logger.info(prefixe+"EVT evtRcvChunckResponse: initial chunk msg {}", respMsg.getAssociatedChunkMsgData());
				}

				
			};

			MsrpSessionListener listenerClt = new MyMsrpSessionListener("CLT");
			MsrpSessionListener listenerSrv = new MyMsrpSessionListener("SRV");
			
			InetAddress inetHost = InetAddress.getLocalHost();

			logger.info("Start server on {} ", inetHost.getHostAddress());
			MsrpAddrServer addrServ = new MsrpAddrServer("msrp",inetHost, 2855);
			sessFactory.cnxFactory.startServer(addrServ );

	        //MsrpSessionHdImpl sessServHd = new MsrpSessionHdImpl(sessFactory, null, false);
	        //MsrpSessionHd sessServHd = sessFactory.createSession(inetHost,listenerSrv,false, 30);

	        //sessServHd.setOption(EnumSessionOptions.SESS_NOTIFY_FULL_RESPONSE);

	        
	        //String fromPath = sessHd.getLocalPath();
	        //logger.info("Local path: {}", sessServHd.getLocalPath());
			
	        MsrpSessionHd sessServHd = sessFactory.createSimpleSession(inetHost,listenerSrv,false, 30);

	        sessServHd.setOption(EnumSessionOptions.SESS_NOTIFY_FULL_RESPONSE);

			
	        //String remoteURI = "msrp://10.255.255.1:80/abcd;tcp";
	        String remoteURI = sessServHd.getLocalPath();
			
			logger.info("Init client with inact TO set to 5");
	        MsrpSessionHd sessCltHd = sessFactory.createSimpleSession(inetHost,listenerClt,false, 5);

	        sessCltHd.setOption(EnumSessionOptions.SESS_NOTIFY_FULL_RESPONSE);

			logger.info("Connect client on a fake path {}", remoteURI);
	        sessCltHd.setRemotePath(remoteURI);
	        logger.info("Client path: {}", sessCltHd.getLocalPath());

	        
			logger.info("Bind server");
	        sessServHd.setRemotePath(sessCltHd.getLocalPath());
	        logger.info("Server path: {}", sessServHd.getLocalPath());

	        MsrpSessionHdFuture futSrv = sessServHd.bind();

	        MsrpSessionHdFuture futClt = sessCltHd.connect(2).sync();
	        
	        if (!futClt.isSuccess()) {
	        	logger.error("Connexion failure on client side ");
	        }
	        
			// wait cnx on server
	        futSrv.sync();
			
	        if (!futSrv.isSuccess()) {
	        	logger.error("Connexion failure on server side");
	        }
	        else {
				// Send Msg from server to client
		        logger.info("Server is here !" );
	        }

	        // Wait inactivity CB
	        logger.info("Wait CB inactivity" );
			Thread.sleep(10000);
			
	        logger.info("end waiting" );
			
			try {
				sessCltHd.close();
				sessServHd.close();
			}
			catch (Exception e) {
				logger.error("Failed to close sesssion: {}, msg: {} ", sessCltHd.getLocalPath(), e.getMessage());
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
