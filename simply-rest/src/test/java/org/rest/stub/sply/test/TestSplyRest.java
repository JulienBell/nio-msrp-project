package org.rest.stub.sply.test;

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
import org.rest.stub.sply.client.SplyHttpCltConnector;
import org.rest.stub.sply.client.SplyHttpCltPoolConnector;
import org.rest.stub.sply.client.SplyRestCltFactory;
import org.rest.stub.sply.server.SplyRestSrvFactory;
import junit.framework.TestCase;

public class TestSplyRest extends TestCase {

	SplyRestSrvFactory srvFactory = SplyRestSrvFactory.getInstance();
	SplyRestCltFactory cltFactory = SplyRestCltFactory.getInstance();

	final HttpVersion HTTP_2_0 = new HttpVersion("HTTP", 2, 0, false);
	public int cptThd;
	
	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	
	public class TestThread extends Thread {
		
		private SplyHttpCltConnector cnx;
		private String id;
		private TestSplyRest parent;

		protected TestThread(SplyHttpCltConnector cnx, String id, TestSplyRest parent) {
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
		        		HttpVersion.HTTP_1_1, HttpMethod.GET, uri.getRawPath());
		        request.headers().set(HttpHeaders.Names.HOST, "localhost");
		        //request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
				
			} catch (URISyntaxException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	        
			
			
			RestStubFullHttpResponse resp=null;
			try {
				resp = cnx.handleCltRequest(request);

				//System.out.println( this.getName() + ": Response on id "+id+" : " + resp);
				
				byte[] cont = new byte[resp.content().readableBytes()];
				resp.content().readBytes(cont);
				
				System.out.println( this.getName() + ": Response on id "+id+" content: " + new String(cont) );
			
			} catch (RestHttpException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				
			}
			
			
			parent.cptThd--;
		}
	}
	
	//@Test
	@Ignore
	public void testSplyRest() {
		
		RestHttpServlet servlet = new RestHttpServlet() {
			
			int nbReq=0;
			
			RestStubFullHttpRequest reqDelayed;
			RestHttpConnector cnxToRespondDelayed;
			
			@Override
			public void handleRcvRequest(RestStubFullHttpRequest req,
					RestHttpConnector cnxToRespond) {
				System.out.println("Receive request: "+ req.getUri());
				System.out.println("On cnx: "+ cnxToRespond);
				
				nbReq++;
				
				if (nbReq == 2) {
					reqDelayed = req;
					reqDelayed.retain();
					cnxToRespondDelayed = cnxToRespond;
					return;
				}
				RestStubFullHttpResponse resp = new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.ACCEPTED);
				
				resp.content().writeBytes(req.getUri().getBytes()); 
				
				try {
					cnxToRespond.handleSendResponse(req, resp);
				} catch (RestHttpException e) {
					System.out.println("Failed " + e);
				}

				if (nbReq == 3) {
					RestStubFullHttpResponse resp1 = new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

					resp1.content().writeBytes(reqDelayed.getUri().getBytes()); 

					try {
						cnxToRespondDelayed.handleSendResponse(reqDelayed, resp1 );
						reqDelayed.release();
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

	
		System.out.println("Start client");
		
		SplyHttpCltPoolConnector pool = null;
		
		try {
			pool = cltFactory.getOrCreateCltPoolConnector("http://localhost:8080", 2);
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
		
		SplyHttpCltConnector cnx = pool.getConnector();
		
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
		
		System.out.println("End .............");
	}

	@Ignore
	public void testSrvShutdown() {
		
		srvFactory.shutdown();
	}
	
}
