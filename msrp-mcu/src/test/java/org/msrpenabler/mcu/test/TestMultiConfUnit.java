package org.msrpenabler.mcu.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import junit.framework.TestCase;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.enabler.restlib.RestHttpConnector;
import org.enabler.restlib.RestHttpException;
import org.enabler.restlib.RestHttpServlet;
import org.enabler.restlib.wrapper.RestStubFullHttpRequest;
import org.enabler.restlib.wrapper.RestStubFullHttpResponse;
import org.junit.Test;
import org.msrpenabler.server.api.EnumSessionOptions;
import org.msrpenabler.server.api.InfoLogMsrpSessionListener;
import org.msrpenabler.server.api.MsrpChunkData;
import org.msrpenabler.server.api.MsrpMessageData;
import org.msrpenabler.server.api.MsrpSessionHd;
import org.msrpenabler.server.api.MsrpSessionHdFuture;
import org.msrpenabler.server.api.MsrpSessionListener;
import org.msrpenabler.server.api.MsrpSessionsFactory;
import org.msrpenabler.server.cnx.MsrpAddrServer;
import org.rest.stub.sply.client.SplyHttpCltConnector;
import org.rest.stub.sply.client.SplyHttpCltPoolConnector;
import org.rest.stub.sply.client.SplyRestCltFactory;
import org.rest.stub.sply.server.SplyRestSrvFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class TestMultiConfUnit extends TestCase {

	private static final Logger logger = LoggerFactory.getLogger(TestMultiConfUnit.class);

	SplyRestSrvFactory srvHttpFactory = SplyRestSrvFactory.getInstance();
	SplyRestCltFactory cltHttpFactory = SplyRestCltFactory.getInstance();
	
	static String httpNotifURI = "http://localhost:9091";
	
	private URI httpBaseURI = null; 
	private SplyHttpCltConnector cnx;

	public int cptThd;

    private volatile Process procMcu;
	
	protected void setUp() throws Exception {
		super.setUp();
		
		try {
			httpBaseURI = new URI("http://localhost:8080/");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
	}
	
	

	protected void tearDown() throws Exception {
		
		// TODO how to send kill signal
		procMcu.destroy();
		
		super.tearDown();
	}

	public class StatusResponse {
		
		public int statusCode;
		public String statusLabel;
		
	}

	public class SessionInfo {
		
		public String sessId;
		public String localPath;
		public String remotePath;
	}
	
	public Configuration call_method(URI baseURI, String method, HashMap<String, String> parameters, StatusResponse resStatus) {
		
		QueryStringEncoder query = new QueryStringEncoder(baseURI.toString()+method); 
		
		RestStubFullHttpRequest request;
		if (parameters != null) {
            for (Entry<String, String> elt : parameters.entrySet()) {

                query.addParam(elt.getKey(), elt.getValue());
            }
		}

		request = new RestStubFullHttpRequest(
				HttpVersion.HTTP_1_1, HttpMethod.GET, query.toString());
		request.headers().set(HttpHeaders.Names.HOST, baseURI.getHost());
		
        DefaultFullHttpResponse resp=null;
		try {
			logger.info("call http request :"+ request.toString());
			
			resp = cnx.handleCltRequest(request);

			logger.info("Response : " + resp);
			
			if (resp != null && resStatus != null) {
				resStatus.statusCode =  resp.getStatus().code();
				resStatus.statusLabel = resp.getStatus().reasonPhrase();
			}

			if (resp != null && resp.content() != null) {
				
				String content = resp.content().toString(StandardCharsets.UTF_8);
				System.out.println("Response content : " + content );
				
				ByteBufInputStream inputStream = new  ByteBufInputStream(resp.content());

				InputStreamReader inReader = new InputStreamReader(inputStream);
				PropertiesConfiguration propRes = new PropertiesConfiguration();

				try {
					propRes.load(inReader);
				} catch (ConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				//String value = propRes.getString(keyResponse);

				return propRes;
			}
			
		} catch (RestHttpException e1) {
			logger.info("Exception: ", e1);
		}
		finally {
			if (resp != null) {
				// Free response buffer
				resp.release();
			}
		}
		
		return null;
	}
	
	
	public String create_conf(String notifURI, StatusResponse resStatus) {

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("notifuri", notifURI);

		Configuration result = call_method(httpBaseURI, "create_conf", params, resStatus);

		if (result == null) return null;
		
		return result.getString("confid");
	}

	public void delete_conf(String cnfId, StatusResponse resStatus) {

		//String params[] = new String[] { cnfId };
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("confid", cnfId);
		
		call_method(httpBaseURI, "delete_conf", params, resStatus);
	}
	
	public SessionInfo create_session(String notifURI, boolean lockIn, boolean lockOut,
									boolean dropIn, 
									boolean listenMsgIn, boolean listenChunckIn,
									boolean notifMsgSendFailure, boolean notifChunckSendFailure,
									boolean setReportSuccess, boolean setReportFailure,
									StatusResponse resStatus) {

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("notifuri", notifURI);

		Configuration result = call_method(httpBaseURI, "create_sess", params, resStatus);
		
		SessionInfo res = new SessionInfo();
		res.sessId = result.getString("sessid");
		res.localPath = result.getString("localpath");
		
		return res;
	}
	
	public void delete_session(String sessId, StatusResponse resStatus) {

		HashMap<String, String> params= new HashMap<String, String>();
        params.put("sessid", sessId);
		
		call_method(httpBaseURI, "delete_sess", params, resStatus);
	}
	
	public void attach_session(String confId, String sessId, StatusResponse resStatus) {

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("confid", confId);
		params.put("sessid", sessId);

		call_method(httpBaseURI, "attach_sess", params, resStatus);
	}
	
	public void detach_session(String confId, String sessId, StatusResponse resStatus) {

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("confid", confId);
		params.put("sessid", sessId);

		call_method(httpBaseURI, "detach_sess", params, resStatus);
	}
	

	public void connect_session(String sessId, String remotePath, StatusResponse resStatus) {

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("sessid", sessId);
		params.put("remotepath", remotePath);

		call_method(httpBaseURI, "connect_sess", params, resStatus);
	}

	public void bind_session(String sessId, String remotePath, StatusResponse resStatus) {

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("sessid", sessId);
		params.put("remotepath", remotePath);

		call_method(httpBaseURI, "bind_sess", params, resStatus);
	}
	
//	public class TestThread extends Thread {
//		
//		//private SplyHttpCltConnector cnx;
//		//private String id;
//		private TestMultiConfUnit parent;
//
//		protected TestThread(SplyHttpCltConnector cnx, String id, TestMultiConfUnit parent) {
//			//this.cnx = cnx;
//			//this.id = id;
//			this.parent = parent;
//			
//		}
//		
//		public void run() {
//			
//			try {
//				
//				
//			}
//			finally {
//				
//				parent.cptThd--;
//				
//			}
//		}
//	}
	
	@Test
	//@Ignore
	public void testSplyRest() {

		try {
			
		/**
		 * Start McuServer in another JVM
		 */
		Thread thdMcu = new Thread() {
			
			@Override
			public void run() {
				
//				Map<String, String> env = System.getenv();
//		        for (String envName : env.keySet()) {
//		            System.out.format("env %s=%s%n",  envName,         env.get(envName));
//		        }
		        String cp = System.getProperty("java.class.path");

		        System.out.format("TestMultiConf - Classpath property %s%n", cp);
		        
		        for(Entry<String,String> entry : System.getenv().entrySet()) {
		        	System.out.format("TestMultiConf - logback envir %s=%s%n",
		        			entry.getKey(), entry.getValue() );
		        }

				ProcessBuilder pb = new ProcessBuilder();
				
//		        System.out.format("TestMultiConf - logback envir %s%n", 
//		        					pb.environment().get("logback.configurationFile") );
//		        
//		        for(Entry<String,String> entry : pb.environment().entrySet()) {
//		        	System.out.format("TestMultiConf - logback envir %s=%s%n",
//		        			entry.getKey(), entry.getValue() );
//		        }
//		        
//		        System.out.format("TestMultiConf - logback envir %s%n", 
//    					System.getenv("logback.configurationFile") );
//
//		        System.out.format("TestMultiConf - logback envir %s%n", 
//		        		System.getProperty("logback.configurationFile") );
		        
		        
				
				String[] params = new String [5];
			    params[0] = "java";
                params[1] = "-cp";
                params[2] = cp;
			    params[3] = "-Dlogback.configurationFile=logback-mcu.xml";
                params[4] = "org.msrpenabler.mcu.start.McuStart" ;
//			    params[4] = "target/msrp-mcu-1.0.CR1.jar";
			    try {
			    	pb.command(params);

                    pb.inheritIO();

//			    	pb.redirectErrorStream(true);
//			    	File file = new File("/logs/mcu.log");
//			    	
//					pb.redirectOutput(file);
//					
//			    	pb.redirectInput(Redirect.INHERIT);
			    	
					procMcu = pb.start();
			        System.out.println("TestMultiConf - mcu started " + procMcu.toString());
					
			    	procMcu.waitFor();
			        System.out.println("end of wait for mcu");

			    } catch (IOException e) {
                    System.out.println("TestMultiConf - mcu start failure" + e);
				} catch (InterruptedException e) {
                    System.out.println("TestMultiConf - mcu start failure" + e);
				}		
			}
		};
		
		thdMcu.start();
		
		
		
		RestHttpServlet servlet = new RestHttpServlet() {
			
			public void handleRcvRequest(RestStubFullHttpRequest req,
					RestHttpConnector cnxToRespond) {
				logger.info("Receive request: "+ req);
				logger.info("On cnx: "+ cnxToRespond);

				QueryStringDecoder query = new QueryStringDecoder(req.getUri());
				
				String path = query.path();
				Map<String, List<String>> params = query.parameters();
				
				logger.info("received query path: "+ path);
				logger.info("Received query params size: "+ params.size());								
				
				RestStubFullHttpResponse resp = new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

				try {
					cnxToRespond.handleSendResponse(req, resp);
				} catch (RestHttpException e) {
					System.out.println("Failed " + e);
				}

			}

		};
		
		logger.info("Start http notif Server"+httpNotifURI);
		try {
			srvHttpFactory.getSrvBindConnector(httpNotifURI, servlet);
		} catch (RestHttpException e2) {
			logger.error("Failed to bind to "+httpNotifURI, e2);
		}

	
		try {
			Thread.sleep(100);
		} catch (InterruptedException e2) {
			logger.info("Interrupt on sleep ");
		}
		logger.info("Start http client to command serveur mcu");
		
		SplyHttpCltPoolConnector pool = null;
		
		try {
			pool = cltHttpFactory.getOrCreateCltPoolConnector(httpBaseURI.toString(), 2);
		} catch (RestHttpException e1) {
			logger.error("failed on create http client connector with mcu ",e1);
			return;
		}
		

			Thread.sleep(2000);

			cnx = pool.getConnector();

			StatusResponse resStatus  = new StatusResponse();

			logger.info("Create conf on serveur mcu");
            String confId = create_conf(httpNotifURI, resStatus);

            logger.info("Create sess1 on serveur mcu");
			SessionInfo sessMsrp1 = create_session( httpNotifURI, false, false, false, true, false, true, true,
					false, true, resStatus);

			attach_session(confId, sessMsrp1.sessId, resStatus);

			logger.info("Create sess2 on serveur mcu");
			SessionInfo sessMsrp2 = create_session( httpNotifURI, false, false, false, true, false, true, true,
					false, true, resStatus);

			attach_session(confId, sessMsrp2.sessId, resStatus);

			logger.info("Create sess3 on serveur mcu");
			SessionInfo sessMsrp3 = create_session( httpNotifURI, false, false, false, true, false, true, true,
					false, true, resStatus);

			attach_session(confId, sessMsrp3.sessId, resStatus);

		
		// Create local MSRP client sessions
		
		MsrpSessionsFactory sessMsrpFactory = (MsrpSessionsFactory) MsrpSessionsFactory.getDefaultInstance();

		logger.info("Start all local MSRP client stuff");

		class MyMsrpSessionListener extends InfoLogMsrpSessionListener {

			public MyMsrpSessionListener(String name) {
				super(name);
			}

			@Override
			public void evtRcvMessage(MsrpMessageData msrpContent,
					boolean wasChunked) {

				super.evtRcvMessage(msrpContent, wasChunked);
			}
		}
		
		
		try {

			MsrpSessionListener listenerClt1 = new MyMsrpSessionListener("CLT_1");
			MsrpSessionListener listenerClt2 = new MyMsrpSessionListener("CLT_2");
			MsrpSessionListener listenerClt3 = new MyMsrpSessionListener("CLT_3");

			InetAddress inetHost = InetAddress.getLocalHost();


			logger.info("Start local MSRP bind srver on client");
			MsrpAddrServer addrServ = new MsrpAddrServer("msrp",inetHost, 2860);
			sessMsrpFactory.cnxFactory.startServer(addrServ);
			
			logger.info("Start client sessions on {} ", inetHost.getHostAddress());
			
			System.out.println("Start client msrp sess1 ");
			MsrpSessionHd sessCltHd1 = sessMsrpFactory.createSimpleSession(inetHost,listenerClt1,false, 30);

			sessCltHd1.setOption(EnumSessionOptions.SESS_NOTIFY_FULL_RESPONSE);

			logger.info("Bind server 1");
			sessMsrp1.remotePath = sessCltHd1.getLocalPath();
			logger.info("Server path: {}", sessMsrp1.localPath);

			bind_session(sessMsrp1.sessId, sessMsrp1.remotePath, resStatus);

			logger.info("Connect client 1");
			sessCltHd1.setRemotePath(sessMsrp1.localPath);
			logger.info("Client 1 path: {}", sessCltHd1.getLocalPath());

			MsrpSessionHdFuture futClt1 = sessCltHd1.connect(5).sync();
			logger.info("Connect client 1 is connected ");

			if (futClt1.isSuccess()) {
				logger.info("Sess Clt 1 is connected ");
			}
			else {
				assertTrue("Failure on Client 1 connection ", false);
			}
			
			
			System.out.println("Start client msrp sess2 ");
			MsrpSessionHd sessCltHd2 = sessMsrpFactory.createSimpleSession(inetHost,listenerClt2,false, 30);

			sessCltHd2.setOption(EnumSessionOptions.SESS_NOTIFY_FULL_RESPONSE);

			logger.info("Bind server 2");
			sessMsrp2.remotePath = sessCltHd2.getLocalPath();
			logger.info("Server path: {}", sessMsrp2.localPath);

			bind_session(sessMsrp2.sessId, sessMsrp2.remotePath, resStatus);

			logger.info("Connect client 2");
			sessCltHd2.setRemotePath(sessMsrp2.localPath);
			logger.info("Client 2 path: {}", sessCltHd2.getLocalPath());

			MsrpSessionHdFuture futClt2 = sessCltHd2.connect(5).sync();
			logger.info("Connect client 2 is connected ");

			if (futClt2.isSuccess()) {
				logger.info("Sess Clt 2 is connected ");
			}
			else {
				assertTrue("Failure on Client 2 connection ", false);
			}

			
			System.out.println("Start client msrp sess 3 ");
			MsrpSessionHd sessCltHd3 = sessMsrpFactory.createSimpleSession(inetHost,listenerClt3,false, 30);

			sessCltHd3.setOption(EnumSessionOptions.SESS_NOTIFY_FULL_RESPONSE);

			logger.info("Bind client 3");
			logger.info("Client 3 path: {}", sessCltHd3.getLocalPath());
			logger.info("Server sess 3 path: {}", sessMsrp3.localPath);

			sessCltHd3.setRemotePath(sessMsrp3.localPath);

			MsrpSessionHdFuture futClt3 = sessCltHd3.bind();
			logger.info("Connect client 3 is binding ");
			
			sessMsrp3.remotePath = sessCltHd3.getLocalPath();

			logger.info("Command mcu to connect sess 3");

			connect_session(sessMsrp3.sessId, sessMsrp3.remotePath, resStatus);

			futClt3.sync();

			if (futClt3.isSuccess()) {
				logger.info("Sess Clt 3 is connected ");
			}
			else {
				assertTrue("Failure on Client 3 connection ", false);
			}

			
			
	        ByteBuf buffClt = Unpooled.buffer();
	        ByteBuf buffClt2 = Unpooled.buffer();
	        
	        if (!futClt1.isSuccess()) {
	        	logger.error("Connexion failure on client side");
	        }
	        else {
	        
				logger.info("Send Msg from client sess1 to server");
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
				sessCltHd3.close();

				Thread.sleep(100);
			}
			catch (Exception e) {
				logger.error("Failed to close sesssion: ", e);
			}
			
			
			
		}
		catch(Exception e) {
			
			logger.info("Exception : ", e);
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
		
		} catch (Exception e) {

			logger.error("Exception ", e);
		}
		
		System.out.println("End .............");
		
	}

	
}
