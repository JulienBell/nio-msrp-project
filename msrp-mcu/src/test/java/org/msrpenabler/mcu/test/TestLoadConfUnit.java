package org.msrpenabler.mcu.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import junit.framework.TestCase;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.enabler.restlib.RestHttpConnector;
import org.enabler.restlib.RestHttpException;
import org.enabler.restlib.RestHttpServlet;
import org.enabler.restlib.wrapper.RestStubFullHttpRequest;
import org.enabler.restlib.wrapper.RestStubFullHttpResponse;
import org.junit.Test;
import org.msrpenabler.mcu.test.TestMcuCltApi.SessionInfo;
import org.msrpenabler.mcu.test.TestMcuCltApi.StatusResponse;
import org.msrpenabler.server.api.EnumSessionOptions;
import org.msrpenabler.server.api.InfoLogMsrpSessionListener;
import org.msrpenabler.server.api.MsrpChunkData;
import org.msrpenabler.server.api.MsrpMessageData;
import org.msrpenabler.server.api.MsrpSessionHd;
import org.msrpenabler.server.api.MsrpSessionHdFuture;
import org.msrpenabler.server.api.MsrpSessionListener;
import org.msrpenabler.server.api.MsrpSessionsFactory;
import org.msrpenabler.server.cnx.MsrpAddrServer;
import org.msrpenabler.server.exception.SessionHdException;
import org.rest.stub.sply.client.SplyHttpCltConnector;
import org.rest.stub.sply.client.SplyHttpCltPoolConnector;
import org.rest.stub.sply.client.SplyRestCltFactory;
import org.rest.stub.sply.server.SplyRestSrvFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class TestLoadConfUnit extends TestCase {

	private static final Logger logger = LoggerFactory.getLogger(TestLoadConfUnit.class);

	SplyRestSrvFactory srvHttpFactory = SplyRestSrvFactory.getInstance();
	SplyRestCltFactory cltHttpFactory = SplyRestCltFactory.getInstance();

//	static String hostname = "Julien-PC";
	static String hostname = "localhost";
	//static String httpNotifURI = "http://localhost:9091";
	static String httpNotifURI = "http://"+ hostname +":9091";
	
	static public Boolean mcuServerIsStarted=false;


	private URI httpBaseURI = null; 
	private SplyHttpCltConnector cnx;
	private TestMcuCltApi cltAPI;

	public volatile int cptThd;

	private volatile Process procMcu;


	public StructTimers timers[];	

	public class Tuple {
		public long v1;
		public long v2;
		public long v3=Long.MAX_VALUE;
		
		public String res(int nbVal) {
			
			return "avg: " + v1/nbVal/1000 + ", max: "+ v2/1000 +", min: "+v3/1000;
		}
	};
	
	final static class MyFunct {
		static Tuple apply(Tuple tuple, long val, StructTimers result, String label, int iter) {

			long v = ( val >= 0 ? val : 0 );
			if (val < 0) {
				logger.warn("failed getMicroSecDelay timer {} on iter {}", label, iter );
				logger.warn("failed getMicroSecDelay list msg1 clt1 : {}", result.rcvTabMsg[0][0]);
				logger.warn("failed getMicroSecDelay list msg2 clt1 : {}", result.rcvTabMsg[0][1]);
				logger.warn("failed getMicroSecDelay list msg1 clt2 {}", result.rcvTabMsg[1][0]);
				logger.warn("failed getMicroSecDelay list msg2 clt2 : {}", result.rcvTabMsg[1][1]);
			}
			
			tuple.v1 += v;
			tuple.v2 = Math.max(v, tuple.v2);
			tuple.v3 = Math.min(v, tuple.v3);
					
			return tuple;
		}
	}

	protected void setUp() throws Exception {
		super.setUp();

		//inetHost = InetAddress.getLocalHost();

		try {
			//httpBaseURI = new URI("http://"+inetHost.getHostAddress()+":8080/");
			httpBaseURI = new URI("http://"+hostname+":8080/");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		cltAPI = new TestMcuCltApi(httpBaseURI);
	}



	protected void tearDown() throws Exception {

		// TODO how to send kill signal
		//procMcu.destroy();

		//McuStop.stop(null);
		

		ProcessBuilder pb = new ProcessBuilder();

		String[] params = new String [1];
		String projectPath = System.getProperty("user.dir");
		params[0] = projectPath+"\\target\\MsrpMcuServer\\bin\\MsrpMcuServerStop.bat";
		logger.info("TestMultiConf - MCU Stop Path {}", params[0]);
		
		try {
			pb.command(params);

			pb.inheritIO();

			procMcu = pb.start();
			logger.info("TestMultiConf - mcu started " + procMcu.toString());

			procMcu.waitFor();
			logger.info("end of wait for mcu");

		} catch (IOException e) {
			logger.error("TestMultiConf - mcu stop failure ", e);
		} catch (InterruptedException e) {
			logger.error("TestMultiConf - mcu stop failure ", e);
		}		
	
		super.tearDown();
	}

	class MyTimestamp {

		private volatile long dateStart;
		private volatile long nanosStart;

		private volatile long dateLast;
		private volatile long nanosLast;

		public void setDateStart() {
			dateStart = System.currentTimeMillis();
			nanosStart = System.nanoTime();
		}

		public void setDateLast() {
			dateLast = System.currentTimeMillis();
			nanosLast = System.nanoTime();
		}

		public long getMicrosecDelay() {
			if (nanosLast <= nanosStart) {
				logger.warn("Negative delay ? {}", (nanosLast - nanosStart) /1000);
				logger.warn("   start {}", (nanosStart) /1000);
				logger.warn("   end   {}", (nanosLast ) /1000);
				return -1;
			}

			return ((nanosLast - nanosStart) /1000);
		}

		public long getDateStart() {
			return dateStart;
		}

		public long getDateLast() {
			return dateLast;
		}

	}

	class StructTimers {

		public MyTimestamp init;
		public MyTimestamp sendChunk1;
		public MyTimestamp sendChunk2;
		public MyTimestamp sendMsg;

		public MyTimestamp[][] rcvTabMsgTimers;
		public String[][] rcvTabMsg; 

		private StructTimers(int nbClt, int nbMsg) {

			init = new MyTimestamp();
			this.rcvTabMsgTimers = new MyTimestamp[nbClt][nbMsg];
			this.rcvTabMsg = new String[nbClt][nbClt];

			for(int i=0;i<nbClt;i++) {
				for(int j=0;j<nbMsg;j++) {
					rcvTabMsgTimers[i][j] = new MyTimestamp();
				}
			}
		}

		//		public MyTimestamp rcvChunk1Clt1;
		//		public MyTimestamp rcvChunk1Clt2;
		//		
		//		public MyTimestamp rcvChunk2Clt1;
		//		public MyTimestamp rcvChunk2Clt2;
		//		
		//		public MyTimestamp rcvMsgClt1;
		//		public MyTimestamp rcvMsgClt2;

	}

	class MyMsrpSessionListener extends InfoLogMsrpSessionListener {

		StructTimers timers;
		int id;
		volatile int nbRcv=0;
		volatile int nbChunck=0;
		volatile byte[] chk1 = null;
		volatile byte[] chk2 = null;
		volatile String strChk1, strChk2, msgIdChk;

		public MyMsrpSessionListener(String name, int id, StructTimers timers) {
			super(name);
			this.timers = timers;
			this.id = id;
		}

		@Override
		public void evtRcvMsrpChunk(MsrpChunkData msrpChunk) {
			logger.debug("EVT evtRcvMsrpChunk {}", msrpChunk);
			
			String strContent = new String(msrpChunk.getContentByte());
			String msgId = msrpChunk.getMessageId();
			if (nbChunck==0) {
				chk1 = msrpChunk.getContentByte();
				if (! strContent.equals("This is")) {
					try {
						throw new Exception();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						logger.error(" chunk 1 unexpected {}, {}", chk1, strContent, e);
					}
				}
				strChk1 = strContent;
				msgIdChk = msgId;
			}
			else {
				chk2 = msrpChunk.getContentByte();

				if (! strContent.equals(" a test !")) {
					try {
						throw new Exception();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						logger.error(" chunk 1 unexpected {}, {}", chk1, strContent, e);
					}
				}
				strChk2 = strContent;
				if (! msgId.equals(msgIdChk) ) logger.error("Unexpected Id {}, waited: {}", msgId, msgIdChk); 
			}
			nbChunck++;

		
		}
		
		@Override
		public void evtRcvMessage(MsrpMessageData msrpContent,
				boolean wasChunked) {

			super.evtRcvMessage(msrpContent, wasChunked);

			String content=new String(msrpContent.getContentByte());

			//			logger.info("chuncked: {}", wasChunked);
			logger.debug("Content: {}", content);
			//			logger.info("Name: {}", getName());

			if (wasChunked == false) {
				assertEquals(getName(),content);
			}


			int idRcv = nbRcv;
			nbRcv++;

			timers.rcvTabMsg[id][idRcv] = content;
			if (id >= 0) { 
				timers.rcvTabMsgTimers[id][idRcv].setDateLast();
				long delay = timers.rcvTabMsgTimers[id][idRcv].getMicrosecDelay();
				logger.debug("Timer  rcv msg {} : {}", new Object[]{ id, idRcv }, delay);
				if (delay < 0) {
					logger.warn("Bad value delay for id {}, idMsg {}", id, idRcv);
				}
				
				
				String msg1 = new String("This is a test !");
				if (idRcv == 0) {
					if (! content.equals(msg1)) {
						logger.error("Recieve unexpected msg 1 {}, size {}, id {}", content, msrpContent.getContentLength(), msrpContent.getMessageId());
						logger.error("... nbChunck {}, msgIdChk {}", nbChunck, msgIdChk);
						try {
							logger.error(" local sessId {}", this.getSessionHd().getLocalPath());
						} catch (SessionHdException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (nbChunck >= 1) logger.error("... Chunck 1 {}, {}", chk1, strChk1) ;
						if (nbChunck == 2) logger.error("... Chunck 2 {}, {}", chk2, strChk2) ;
					}
				}
				if (idRcv == 1) {
					if (! content.equals(getName())) {
						logger.error("Recieve unexpected msg 2 {}, size {}", content, msrpContent.getContentLength());
					}
				}
				
			}


		}


	}



	//InetAddress inetHost ;



	public class TestThread extends Thread {

		private String id;
		private TestLoadConfUnit parent;
		private SplyHttpCltPoolConnector pool;

		StructTimers timers = new StructTimers(2,2);

		String prefix;

		protected TestThread(SplyHttpCltPoolConnector poolIn, String idThd, int ind, TestLoadConfUnit parent) {
			//this.cnx = cnx;
			//this.id = id;
			this.parent = parent;
			this.pool = poolIn;
			this.id = idThd+ind;

			prefix = "Thd_"+id+"| ";

			parent.timers[Integer.valueOf(ind)] = this.timers;
		}

		public void run() {

			try {

				cnx = pool.getConnector();

				StatusResponse resStatus  = cltAPI.new StatusResponse();


				timers.init.setDateStart();

				logger.debug(prefix+"Create conf on serveur mcu");
				String confId = cltAPI.create_conf(cnx, httpNotifURI, resStatus);

				logger.debug(prefix+"Create sess1 on serveur mcu");
				SessionInfo sessMsrp1 = cltAPI.create_session(cnx, httpNotifURI, false, false, false, true, false, true, true,
						false, true, 180, resStatus);

				cltAPI.attach_session(cnx, confId, sessMsrp1.sessId, resStatus);

				logger.debug(prefix+"Create sess2 on serveur mcu");
				SessionInfo sessMsrp2 = cltAPI.create_session(cnx, httpNotifURI, false, false, false, true, false, true, true,
						false, true, 180, resStatus);

				cltAPI.attach_session(cnx, confId, sessMsrp2.sessId, resStatus);

				logger.debug(prefix+"Create sess3 on serveur mcu");
				SessionInfo sessMsrp3 = cltAPI.create_session(cnx, httpNotifURI, false, false, false, true, false, true, true,
						false, true, 180, resStatus);

				cltAPI.attach_session(cnx, confId, sessMsrp3.sessId, resStatus);


				// Create local MSRP client sessions

				MsrpSessionsFactory sessMsrpFactory = (MsrpSessionsFactory) MsrpSessionsFactory.getDefaultInstance();

				logger.debug(prefix+"Start all local MSRP client stuff");

				MsrpSessionListener listenerClt1 = new MyMsrpSessionListener(id, -1, timers);
				MsrpSessionListener listenerClt2 = new MyMsrpSessionListener(id, 0, timers);
				MsrpSessionListener listenerClt3 = new MyMsrpSessionListener(id, 1, timers);


				logger.debug(prefix+"Start client sessions on {} ", InetAddress.getByName(hostname));

				logger.debug(prefix+"Start client msrp sess1 ");
				MsrpSessionHd sessCltHd1 = sessMsrpFactory.createSimpleSession(InetAddress.getByName(hostname),listenerClt1,false, 120);

				sessCltHd1.setOption(EnumSessionOptions.SESS_NOTIFY_FULL_RESPONSE);

				logger.debug(prefix+"Bind server 1");
				sessMsrp1.remotePath = sessCltHd1.getLocalPath();
				logger.debug(prefix+"Server path: {}", sessMsrp1.localPath);

				cltAPI.bind_session(cnx, sessMsrp1.sessId, sessMsrp1.remotePath, resStatus);

				logger.debug(prefix+"Connect client 1");
				sessCltHd1.setRemotePath(sessMsrp1.localPath);
				logger.debug(prefix+"Client 1 path: {}", sessCltHd1.getLocalPath());

				MsrpSessionHdFuture futClt1 = sessCltHd1.connect(5).sync();
				logger.debug(prefix+"Connect client 1 is connected ");

				if (futClt1.isSuccess()) {
					logger.debug(prefix+"Sess Clt 1 is connected ");
				}
				else {
					logger.error(prefix+"Sess Clt 1 connection failed  ", futClt1.cause());
					assertTrue("Failure on Client 1 connection ", false);
				}


				logger.debug(prefix+"Start client msrp sess2 ");
				MsrpSessionHd sessCltHd2 = sessMsrpFactory.createSimpleSession(InetAddress.getByName(hostname),listenerClt2,false, 120);

				sessCltHd2.setOption(EnumSessionOptions.SESS_NOTIFY_FULL_RESPONSE);

				logger.debug(prefix+"Bind server 2");
				sessMsrp2.remotePath = sessCltHd2.getLocalPath();
				logger.debug(prefix+"Server path: {}", sessMsrp2.localPath);

				cltAPI.bind_session(cnx, sessMsrp2.sessId, sessMsrp2.remotePath, resStatus);

				logger.debug(prefix+"Connect client 2");
				sessCltHd2.setRemotePath(sessMsrp2.localPath);
				logger.debug(prefix+"Client 2 path: {}", sessCltHd2.getLocalPath());

				MsrpSessionHdFuture futClt2 = sessCltHd2.connect(5).sync();
				logger.debug(prefix+"Connect client 2 is connected ");

				if (futClt2.isSuccess()) {
					logger.debug(prefix+"Sess Clt 2 is connected ");
				}
				else {
					logger.error(prefix+"Sess Clt 2 connection failed  ", futClt2.cause());
					assertTrue("Failure on Client 2 connection ", false);
				}


				logger.debug(prefix+"Start client msrp sess 3 ");
				MsrpSessionHd sessCltHd3 = sessMsrpFactory.createSimpleSession(InetAddress.getByName(hostname),listenerClt3,false, 120);

				sessCltHd3.setOption(EnumSessionOptions.SESS_NOTIFY_FULL_RESPONSE);

				logger.debug(prefix+"Bind client 3");
				logger.debug(prefix+"Client 3 path: {}", sessCltHd3.getLocalPath());
				logger.debug(prefix+"Server sess 3 path: {}", sessMsrp3.localPath);

				sessCltHd3.setRemotePath(sessMsrp3.localPath);

				sessMsrp3.remotePath = sessCltHd3.getLocalPath();

				logger.debug(prefix+"Command mcu to bind sess 3");

				cltAPI.bind_session(cnx, sessMsrp3.sessId, sessMsrp3.remotePath, resStatus);

				MsrpSessionHdFuture futClt3 = sessCltHd3.connect(5).sync();
				logger.debug(prefix+"Connect client 3 is connected ");

				if (futClt3.isSuccess()) {
					logger.debug("Sess Clt 3 is connected ");
				}
				else {
					logger.error(prefix+"Sess Clt 3 connection failed  ", futClt3.cause());
					assertTrue("Failure on Client 3 connection ", false);
				}

				
				Thread.sleep(1000);

				// First Msg
				for(int i=0;i<2;i++) {
					timers.rcvTabMsgTimers[i][0].setDateStart();
				}

				ByteBuf buffchk1 = Unpooled.buffer();
				ByteBuf buffchk2 = Unpooled.buffer();
				ByteBuf buffMsg2 = PooledByteBufAllocator.DEFAULT.buffer();

				if (!futClt1.isSuccess()) {
					logger.error(prefix+"Connexion failure on client side");
				}
				else {

					//Thread.sleep(10);

					logger.debug(prefix+"Send Msg from client sess1 to server");
					// Send Msg from client to server
					buffchk1.writeBytes( "This is".getBytes() );

					String msgId = MsrpSessionsFactory.createMessageId();

					MsrpChunkData msrpChunk = MsrpSessionsFactory.createChunkMsg(msgId, "text/plain", buffchk1,
							1, buffchk1.readableBytes(), 16);

					sessCltHd1.sendMsrpChunk(msrpChunk, true);


					Thread.sleep(10);


					buffchk2.writeBytes( " a test !".getBytes() );

					msrpChunk = MsrpSessionsFactory.createChunkMsg(msgId, "text/plain", buffchk2,
							8, buffchk2.readableBytes()+8-1, 16);

					sessCltHd1.sendMsrpChunk(msrpChunk, true);



					Thread.sleep(5000);

					// Third Msg
					for(int i=0;i<2;i++) {
						timers.rcvTabMsgTimers[i][1].setDateStart();
					}
					buffMsg2.writeBytes( id.getBytes() );

					MsrpMessageData msrpMessage = MsrpSessionsFactory.createMsg("text/plain", buffMsg2);
					sessCltHd1.sendMsrpMessage(msrpMessage);
				}



				boolean ok=false;
				int nbIter=0;
				while (!ok && nbIter < 30) {

					nbIter++;
					Thread.sleep(1000);

					String clt1msg1 = timers.rcvTabMsg[0][0];
					String clt1msg2 = timers.rcvTabMsg[0][1];
					String clt2msg1 = timers.rcvTabMsg[1][0];
					String clt2msg2 = timers.rcvTabMsg[1][1];

					if ( (clt1msg1 == null) || (clt1msg2 == null) || (clt2msg1 == null) || (clt2msg2 == null) )
					{
						logger.debug("[{}] msg clt1msg1 {}", id, clt1msg1);
						logger.debug("[{}] msg clt1msg2 {}", id, clt1msg2);
						logger.debug("[{}] msg clt2msg1 {}", id, clt2msg1);
						logger.debug("[{}] msg clt2msg2 {}", id, clt2msg2);
					}
					else {
						timers.init.setDateLast();
						ok=true;
					}
				}

				try {
					sessCltHd1.close();
					sessCltHd2.close();
					sessCltHd3.close();

					Thread.sleep(100);
				}
				catch (Exception e) {
					logger.error(prefix+"Failed to close sesssion: ", e);
				}


			} catch (Exception e) {

				logger.error("Exception : ", e);
			}
			finally {

				synchronized (parent) {
					parent.cptThd--;
				}

			}
		}


	}


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

					String cp = System.getProperty("java.class.path");

					logger.info("TestMultiConf - Classpath property {}", cp);

					for(Entry<String,String> entry : System.getenv().entrySet()) {
						logger.info("TestMultiConf - logback envir {}={}",
								entry.getKey(), entry.getValue() );
					}

					logger.info("TestMultiConf - Current dir {}", System.getProperty("user.dir"));
					
					ProcessBuilder pb = new ProcessBuilder();

//					String[] params = new String [6];
//					params[0] = "java";
//					params[1] = "-cp";
//					params[2] = cp;
//					params[3] = "-Dlogback.configurationFile=logback-mcu.xml";
//					params[4] = "-Dio.netty.leakDetectionLevel=paranoid";  //advanced"; 
//					params[5] = "org.msrpenabler.mcu.start.McuStart" ;
//					//			    params[4] = "target/msrp-mcu-1.0.CR1.jar";

					String[] params = new String [1];
					String projectPath = System.getProperty("user.dir");
					params[0] = projectPath+"\\target\\MsrpMcuServer\\bin\\MsrpMcuServer.bat";
					logger.info("TestMultiConf - MCU Patch {}", params[0]);
					
					try {
						pb.command(params);

						pb.inheritIO();

						logger.info("TestMultiConf - start MCU ...");
						procMcu = pb.start();
						logger.info("TestMultiConf - mcu started " + procMcu.toString());


						mcuServerIsStarted = true;

						synchronized (mcuServerIsStarted) {
							mcuServerIsStarted.notifyAll();
						}
						
						
						procMcu.waitFor();
						logger.info("end of wait for mcu");
						

					} catch (IOException e) {
						logger.error("TestMultiConf - mcu start failure - ", e);
					} catch (InterruptedException e) {
						logger.error("TestMultiConf - mcu start failure - ", e);
					}
					
				}
			};

			logger.info("Start MCU Server ");

			thdMcu.start();

			synchronized(mcuServerIsStarted) {
				mcuServerIsStarted.wait(5000);
			}
			
			if (! mcuServerIsStarted ) {
				logger.error("MCU Server start has failed !! ");
				return;
			}
				


			RestHttpServlet servlet = new RestHttpServlet() {

				public void handleRcvRequest(RestStubFullHttpRequest req,
						RestHttpConnector cnxToRespond) {
					logger.debug("Receive request: "+ req);
					logger.debug("On cnx: "+ cnxToRespond);

					QueryStringDecoder query = new QueryStringDecoder(req.getUri());

					String path = query.path();
					Map<String, List<String>> params = query.parameters();

					logger.debug("received query path: "+ path);
					logger.debug("Received query params size: "+ params.size());								

					RestStubFullHttpResponse resp = new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

					try {
						cnxToRespond.handleSendResponse(req, resp);
					} catch (RestHttpException e) {
						logger.error("Failed ", e);
					}

				}

			};
			
			// Leak memory detector
			ResourceLeakDetector.setLevel(Level.ADVANCED);

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
				pool = cltHttpFactory.getOrCreateCltPoolConnector(httpBaseURI.toString(), 4);
			} catch (RestHttpException e1) {
				logger.error("failed on create http client connector with mcu ",e1);
				return;
			}


			logger.info("Set local MSRP port on client");
			MsrpAddrServer addrServ = new MsrpAddrServer("msrp",InetAddress.getByName(hostname), 33000);
			MsrpSessionsFactory.getDefaultInstance().cnxFactory.startServer(addrServ);

			Thread.sleep(1500);


			int maxThread = 100;
			cptThd=maxThread;
			TestThread thd;
			timers = new StructTimers[cptThd+1];

			for(int idThread=0;idThread<maxThread;idThread++) {

				thd = new TestThread(pool, "test_", idThread, this);
				thd.start();
				//Thread.yield();
				Thread.sleep(20); // 50 cnx / sec
				
				// Wait after start the first 10 thread for warming
				if (idThread == 10) {
					Thread.sleep(1000);
				}
				
			}

			while (cptThd > 0){

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			System.out.println("End of all thread...");

			Thread.sleep(1000);

		}
		catch(Exception e) {

			logger.info("Exception : ", e);
		}


		int nbVal=0;
//		long sumInit=0;
		//		float sumChk1Clt1 = 0;
		//		float sumChk1Clt2 = 0;
		//
		//		float sumChk2Clt1 = 0;
		//		float sumChk2Clt2 = 0;

//		long sumMsg1Clt1 = 0;
//		long sumMsg1Clt2 = 0;
//
//		long sumMsg2Clt1 = 0;
//		long sumMsg2Clt2 = 0;
		int iter=0;

		SummaryStatistics stdInit = new SummaryStatistics();
		SummaryStatistics stdMsg1Clt1 = new SummaryStatistics();
		SummaryStatistics stdMsg1Clt2 = new SummaryStatistics();
		SummaryStatistics stdMsg2Clt1 = new SummaryStatistics();
		SummaryStatistics stdMsg2Clt2 = new SummaryStatistics();


		Tuple resIni = new Tuple();
		Tuple resMsg1Clt1 = new Tuple();
		Tuple resMsg1Clt2 = new Tuple();
		Tuple resMsg2Clt1 = new Tuple();
		Tuple resMsg2Clt2 = new Tuple();
		
		for (StructTimers timersTh: timers) {
			if (timersTh == null) continue;

			iter++;
			resIni = MyFunct.apply(resIni, timersTh.init.getMicrosecDelay(), timersTh, "ini", iter);
			stdInit.addValue(timersTh.init.getMicrosecDelay()/1000);

			//			sumChk1Clt1 += timersTh.rcvTabMsgTimers[0][0].getMicrosecDelay();
			//			sumChk1Clt2 += timersTh.rcvTabMsgTimers[1][0].getMicrosecDelay();
			//			
			//			sumChk2Clt1 += timersTh.rcvTabMsgTimers[0][1].getMicrosecDelay();
			//			sumChk2Clt2 += timersTh.rcvTabMsgTimers[1][1].getMicrosecDelay();

			resMsg1Clt1 = MyFunct.apply(resMsg1Clt1,timersTh.rcvTabMsgTimers[0][0].getMicrosecDelay(), timersTh, "msg1Clt1", iter);
			stdMsg1Clt1.addValue(timersTh.rcvTabMsgTimers[0][0].getMicrosecDelay()/1000);
			
			resMsg1Clt2 = MyFunct.apply(resMsg1Clt2,timersTh.rcvTabMsgTimers[1][0].getMicrosecDelay(), timersTh, "msg1Clt2", iter);
			stdMsg1Clt2.addValue(timersTh.rcvTabMsgTimers[1][0].getMicrosecDelay()/1000);

			resMsg2Clt1 = MyFunct.apply(resMsg2Clt1, timersTh.rcvTabMsgTimers[0][1].getMicrosecDelay(), timersTh, "msg2Clt1", iter);
			stdMsg2Clt1.addValue(timersTh.rcvTabMsgTimers[0][1].getMicrosecDelay()/1000);
			
			resMsg2Clt2 = MyFunct.apply(resMsg2Clt2, timersTh.rcvTabMsgTimers[1][1].getMicrosecDelay(), timersTh, "msg2Clt2", iter);
			stdMsg2Clt2.addValue(timersTh.rcvTabMsgTimers[1][1].getMicrosecDelay()/1000);

			nbVal++;
		}


		logger.info(" NbVal : {}", nbVal );
		logger.info(" res Thread time : {}", stdInit );

		//		logger.info(" Sum chk1 clt1 : {}", sumChk1Clt1 );
		//		logger.info(" Sum chk1 clt2 : {}", sumChk1Clt2 );
		//		
		//		logger.info(" Sum chk2 clt1 : {}", sumChk2Clt1 );
		//		logger.info(" Sum chk2 clt2 : {}", sumChk2Clt2 );

		logger.info(" res msg1 clt1 : {}", stdMsg1Clt1 );
		logger.info(" res msg1 clt2 : {}", stdMsg1Clt2 );

		logger.info(" res msg2 clt1 : {}", stdMsg2Clt1 );
		logger.info(" res msg2 clt2 : {}", stdMsg2Clt2 );

//		logger.info(" Average Thread time : {}", sumInit/nbVal/1000 );
//
//		logger.info(" Average msg1 clt1 : {}", sumMsg1Clt1/nbVal/1000 );
//		logger.info(" Average msg1 clt2 : {}", sumMsg1Clt2/nbVal/1000 );
//
//		logger.info(" Average msg2 clt1 : {}", sumMsg2Clt1/nbVal/1000 );
//		logger.info(" Average msg2 clt2 : {}", sumMsg2Clt2/nbVal/1000 );

		System.out.println("End .............");

	}

}

