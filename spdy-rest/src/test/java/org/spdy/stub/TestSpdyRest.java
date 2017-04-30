package org.spdy.stub;

import java.net.URI;
import java.net.URISyntaxException;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;


import org.enabler.restlib.RestHttpConnector;
import org.enabler.restlib.RestHttpException;
import org.enabler.restlib.RestHttpServlet;
import org.enabler.restlib.wrapper.RestStubFullHttpRequest;
import org.enabler.restlib.wrapper.RestStubFullHttpResponse;
import org.junit.Ignore;
import org.junit.Test;
import org.spdyhttp.stub.client.SpdyHttpCltConnector;
import org.spdyhttp.stub.client.SpdyHttpCltPoolConnector;
import org.spdyhttp.stub.client.SpdyRestCltFactory;
import org.spdyhttp.stub.server.SpdyRestSrvFactory;

import junit.framework.TestCase;

public class TestSpdyRest extends TestCase {

	SpdyRestSrvFactory srvFactory = SpdyRestSrvFactory.getInstance();
	SpdyRestCltFactory cltFactory = SpdyRestCltFactory.getInstance();

	final HttpVersion HTTP_2_0 = new HttpVersion("HTTP", 2, 0, false);
	
	volatile int cptThd=0;
	
	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	
	public class TestThread extends Thread {
		
		private SpdyHttpCltConnector cnx;
		private String id;
		private TestSpdyRest parent;

		protected TestThread(SpdyHttpCltConnector cnx, String id, TestSpdyRest parent) {
			this.cnx = cnx;
			this.id = id;
			this.parent = parent;
		}

		
		public void run() {

			URI uri = null;
			RestStubFullHttpRequest request=null;
			try {
				uri = new URI("http://localhost:8080/test_"+id);
		    
				request = new RestStubFullHttpRequest(
		        		HTTP_2_0, HttpMethod.GET, uri.getRawPath());
		        request.headers().set(HttpHeaders.Names.HOST, "localhost");
		        //request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);

			} catch (URISyntaxException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	        
			
			
			RestStubFullHttpResponse resp = null;
			try {
				System.out.println("Call request id: " + uri);
				
				resp = cnx.handleCltRequest(request);
			} catch (RestHttpException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			if (resp != null) {
				byte[] cont = new byte[resp.content().readableBytes()];
				resp.content().readBytes(cont);
				
				System.out.println("Response on id "+id+" content: " + new String(cont) );
			}
			else {
				System.out.println("Response null !! : " + resp );
			}

			parent.cptThd--;
		}

	}
	
	
	@Test
	public void testSpdyRest() {
		
		RestHttpServlet servlet = new RestHttpServlet() {
			
			int nbReq=0;
			
			RestStubFullHttpRequest reqDelayed;
			RestHttpConnector cnxToRespondDelayed;
			
			public void handleRcvRequest(RestStubFullHttpRequest req,
					RestHttpConnector cnxToRespond) {
				System.out.println("Receive request: "+ req.getUri());
				System.out.println("On cnx: "+ cnxToRespond);
				
				nbReq++;
				
				if (nbReq == 2) {
					reqDelayed = req;
					cnxToRespondDelayed = cnxToRespond;
					return;
				}
				RestStubFullHttpResponse resp = new RestStubFullHttpResponse(HTTP_2_0, HttpResponseStatus.OK);
				
				resp.content().writeBytes(req.getUri().getBytes()); 

				try {
					System.out.println("Send resp: "+nbReq+" "+ resp.content());
					cnxToRespond.handleSendResponse(req, resp );
				} catch (RestHttpException e) {
					System.out.println("Failed " + e);
				}

				if (nbReq == 3) {
					resp = new RestStubFullHttpResponse(HTTP_2_0, HttpResponseStatus.ACCEPTED);

					resp.content().writeBytes(reqDelayed.getUri().getBytes()); 
					
					try {
						System.out.println("Send resp: "+(nbReq-1)+" "+ resp.content());
						cnxToRespondDelayed.handleSendResponse(reqDelayed, resp );
					} catch (RestHttpException e) {
						System.out.println("Failed " + e);
					}
				}
			}

		};
		
		System.out.println("Start Server");
		try {
			srvFactory.getSrvBindConnector("http://localhost:8080", servlet);
		} catch (RestHttpException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

	
		RestHttpServlet notifier = new RestHttpServlet() {
			
			public void handleRcvRequest(RestStubFullHttpRequest req,
					RestHttpConnector cnxToRespond) {
				// TODO Auto-generated method stub
				
			}

		};
		
		System.out.println("Start client");
		
		SpdyHttpCltPoolConnector pool = null;
		
		try {
			pool = cltFactory.createCltConnector("http://localhost:8080", 2, notifier);
		} catch (RestHttpException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
		

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		SpdyHttpCltConnector cnx = pool.getConnector();
		
		TestThread thd1 = new TestThread(cnx, "A", this);
		TestThread thd2 = new TestThread(cnx, "B", this);
		TestThread thd3 = new TestThread(cnx, "C", this);
		
		cptThd=3;
		
		thd1.start();
		thd2.start();
		thd3.start();
		
		while (cptThd > 0){

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
	}

	@Ignore
	public void testSrvShutdown() {
		
		srvFactory.shutdown();
	}
	
}
