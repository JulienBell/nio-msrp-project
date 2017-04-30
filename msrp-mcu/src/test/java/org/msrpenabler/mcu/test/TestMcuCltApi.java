/**
 * 
 */
package org.msrpenabler.mcu.test;

import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringEncoder;

import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.enabler.restlib.RestHttpException;
import org.enabler.restlib.wrapper.RestStubFullHttpRequest;
import org.rest.stub.sply.client.SplyHttpCltConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Julien
 *
 */
public class TestMcuCltApi {

	private static final Logger logger = LoggerFactory.getLogger(TestMcuCltApi.class);;
	

	private final URI httpBaseURI; 
	
	public TestMcuCltApi(URI httpBaseURI) {
		this.httpBaseURI = httpBaseURI;
	}

	public class StatusResponse {
		
		public StatusResponse() {
		}
		
		public int statusCode;
		public String statusLabel;
		
	}

	public class SessionInfo {
		
		public String sessId;
		public String localPath;
		public String remotePath;
	}
	
	public static Configuration call_method(SplyHttpCltConnector cnx,
								URI baseURI, String method, HashMap<String, String> parameters, StatusResponse resStatus) {
		
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
			logger.debug("call http request :"+ request.toString());
			
			resp = cnx.handleCltRequest(request);

			logger.debug("Response : " + resp);
			
			if (resp != null && resStatus != null) {
				resStatus.statusCode =  resp.getStatus().code();
				resStatus.statusLabel = resp.getStatus().reasonPhrase();
			}

			if (resp != null && resp.content() != null) {
				
				String content = resp.content().toString(StandardCharsets.UTF_8);
				logger.debug("Response content : " + content );
				
				ByteBufInputStream inputStream = new  ByteBufInputStream(resp.content());

				InputStreamReader inReader = new InputStreamReader(inputStream);
				PropertiesConfiguration propRes = new PropertiesConfiguration();

				try {
					propRes.load(inReader);
				} catch (ConfigurationException e) {
					logger.error("Exception ", e);
				}
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
	
	
	public String create_conf(SplyHttpCltConnector cnx, String notifURI, StatusResponse resStatus) {

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("notifuri", notifURI);

		Configuration result = call_method(cnx, httpBaseURI, "create_conf", params, resStatus);

		if (result == null) return null;
		
		return result.getString("confid");
	}

	public void delete_conf(SplyHttpCltConnector cnx, String cnfId, StatusResponse resStatus) {

		//String params[] = new String[] { cnfId };
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("confid", cnfId);
		
		call_method(cnx, httpBaseURI, "delete_conf", params, resStatus);
	}
	
	public SessionInfo create_session(SplyHttpCltConnector cnx, String notifURI, boolean lockIn, boolean lockOut,
									boolean dropIn, 
									boolean listenMsgIn, boolean listenChunckIn,
									boolean notifMsgSendFailure, boolean notifChunckSendFailure,
									boolean setReportSuccess, boolean setReportFailure,
									int inactityTO,
									StatusResponse resStatus) {

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("notifuri", notifURI);
		params.put("inactivityTO", Integer.toString(inactityTO));

		Configuration result = call_method(cnx, httpBaseURI, "create_sess", params, resStatus);
		
		SessionInfo res = new SessionInfo();
		res.sessId = result.getString("sessid");
		res.localPath = result.getString("localpath");
		
		return res;
	}
	
	public void delete_session(SplyHttpCltConnector cnx, String sessId, StatusResponse resStatus) {

		HashMap<String, String> params= new HashMap<String, String>();
        params.put("sessid", sessId);
		
		call_method(cnx, httpBaseURI, "delete_sess", params, resStatus);
	}
	
	public void attach_session(SplyHttpCltConnector cnx, String confId, String sessId, StatusResponse resStatus) {

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("confid", confId);
		params.put("sessid", sessId);

		call_method(cnx, httpBaseURI, "attach_sess", params, resStatus);
	}
	
	public void detach_session(SplyHttpCltConnector cnx, String confId, String sessId, StatusResponse resStatus) {

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("confid", confId);
		params.put("sessid", sessId);

		call_method(cnx, httpBaseURI, "detach_sess", params, resStatus);
	}
	

	public void connect_session(SplyHttpCltConnector cnx, String sessId, String remotePath, StatusResponse resStatus) {

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("sessid", sessId);
		params.put("remotepath", remotePath);

		call_method(cnx, httpBaseURI, "connect_sess", params, resStatus);
	}

	public void bind_session(SplyHttpCltConnector cnx, String sessId, String remotePath, StatusResponse resStatus) {

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("sessid", sessId);
		params.put("remotepath", remotePath);

		call_method(cnx, httpBaseURI, "bind_sess", params, resStatus);
	}
	
}
