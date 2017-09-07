package org.msrpenabler.mcu.restsrv;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;





import org.enabler.restlib.RestHttpConnector;
import org.enabler.restlib.RestHttpException;
import org.enabler.restlib.RestHttpServlet;
import org.enabler.restlib.wrapper.RestStubFullHttpRequest;
import org.enabler.restlib.wrapper.RestStubFullHttpResponse;
import org.msrpenabler.mcu.model.McuSessionContext;
import org.msrpenabler.mcu.restclt.McuSessListener;
import org.msrpenabler.mculib.cnf.ConferenceUnit;
import org.msrpenabler.mculib.cnf.ConferencesFactory;
import org.msrpenabler.server.api.MsrpSessionHd;
import org.msrpenabler.server.api.MsrpSessionListener;
import org.msrpenabler.server.api.MsrpSessionsFactory;
import org.msrpenabler.server.api.NotifOptions;
import org.msrpenabler.server.cnx.MsrpAddrServer;
import org.msrpenabler.server.exception.SessionHdException;
import org.rest.stub.sply.client.SplyHttpCltConnector;
import org.rest.stub.sply.client.SplyHttpCltPoolConnector;
import org.rest.stub.sply.client.SplyRestCltFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Julien Bellanger
 *
 */
public class McuHttpServlet implements RestHttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(McuHttpServlet.class);

	private final MsrpSessionsFactory msrpSessFactory = MsrpSessionsFactory.getDefaultInstance();
	private final ConferencesFactory cnfFactory = ConferencesFactory.getDefaultInstance();


	public void handleRcvRequest(RestStubFullHttpRequest req,
			RestHttpConnector cnxToRespond) {
		logger.debug("Receive request: "+ req);
		logger.debug("On cnx: "+ cnxToRespond);

		QueryStringDecoder query = new QueryStringDecoder(req.getUri());


		String url = query.path();
		URL urlPath;
		String path = null;
		try {
			urlPath = new URL(url);
			path = urlPath.getPath().substring(1);

		} catch (MalformedURLException e1) {
			logger.error("URL parsing error", e1);
		}

		Map<String, List<String>> params = query.parameters();

		logger.debug("received query path: {}, {}", url, path);
		logger.debug("Received query params size: {}", params.size());								


		HttpResponseStatus status = null; 
		RestStubFullHttpResponse httpResponse = null;

		PathMethodEnum ePath = PathMethodEnum.getValueOf(path);

		try {

			switch (ePath) {

			case CREATE_SESSION:
				httpResponse = createSession(req, params);
				break;

			case DELETE_SESSION:
				break;

			case CONNECT_SESSION:
				httpResponse = connectSession(req, params);
				break;

			case BIND_SESSION:
				httpResponse = bindSession(req, params);
				break;

			case CREATE_CONF:
				httpResponse = createConference(req, params);
				break;

			case DELETE_CONF:
				break;

			case ATTACH_SESSION:
				httpResponse = attachSession(req, params);
				break;

			case DETACH_SESSION:
				break;

			default:
				logger.error("Unknown query path method: {}", url);
				status = HttpResponseStatus.BAD_REQUEST;
				break;
			}

		} catch (Exception e) {
			logger.error("Exception on request with path method: {}", url, e);
			status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
		}

		// Response is send if set, or has to be managed later in a Callback
		if (status != null) {
			httpResponse = new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, status);
		}
		if (httpResponse != null) {
			try {
				cnxToRespond.handleSendResponse(req, httpResponse);
			} catch (RestHttpException e) {
				logger.error("Failed to send response {}", httpResponse, e);
			}
		}
		else {
			logger.debug("No response returned");
		}

	}


	private RestStubFullHttpResponse connectSession(
			RestStubFullHttpRequest req, Map<String, List<String>> params) {

		boolean setupOk=true;

		List<String> paramLst = params.get(QueryParamsEnum.sessid.toString());
		String sessId = paramLst.get(0);

		logger.debug("sessId: {}", sessId);
		MsrpSessionHd sessHd = msrpSessFactory.getMsrpSessionById(sessId);
		HttpResponseStatus code = HttpResponseStatus.OK;

		if (sessHd == null) {
			code = HttpResponseStatus.NOT_FOUND;
			setupOk=false;
		}
		else {

			paramLst = params.get(QueryParamsEnum.remotepath.toString());
			String remote = paramLst.get(0);

			try {
				sessHd.setRemotePath(remote);
			} catch (SessionHdException e) {
				logger.error("set remote failure: {}", sessId, e);
				code = HttpResponseStatus.INTERNAL_SERVER_ERROR;
				setupOk=false;
			}

			// TODO set connection listener and TimeOut

			if (setupOk) {

				try {
					sessHd.connect(10);
				} catch (Exception e) {
					logger.error("connect session failure: {}", sessId, e);
					code = HttpResponseStatus.INTERNAL_SERVER_ERROR;
				}

			}
		}


		// Return result response with local URI		
		RestStubFullHttpResponse resp = new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, code);

		logger.debug("response: {}", resp);

		return resp;
	}


	private RestStubFullHttpResponse bindSession(RestStubFullHttpRequest req,
			Map<String, List<String>> params) {

		boolean setupOk=true;

		List<String> paramLst = params.get(QueryParamsEnum.sessid.toString());
		String sessId = paramLst.get(0);

		logger.debug("sessId: {}", sessId);
		MsrpSessionHd sessHd = msrpSessFactory.getMsrpSessionById(sessId);
		HttpResponseStatus code = HttpResponseStatus.OK;

		if (sessHd == null) {
			code = HttpResponseStatus.NOT_FOUND;
			setupOk=false;
		}
		else {

			paramLst = params.get(QueryParamsEnum.remotepath.toString());
			String remote = paramLst.get(0);

			try {
				sessHd.setRemotePath(remote);
			} catch (SessionHdException e) {
				logger.error("set remote failure: {}", sessId, e);
				code = HttpResponseStatus.INTERNAL_SERVER_ERROR;
				setupOk=false;
			}

			// TODO set connection listener and TimeOut
			if (setupOk) {

				try {
					sessHd.bind();
				} catch (Exception e) {
					logger.error("connect session failure: {}", sessId, e);
					code = HttpResponseStatus.INTERNAL_SERVER_ERROR;
				}

			}
		}

		// Return result response with local URI		
		RestStubFullHttpResponse resp = new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, code);

		logger.debug("response: {}", resp);

		return resp;
	}


	private RestStubFullHttpResponse attachSession(RestStubFullHttpRequest req,
			Map<String, List<String>> params) {

		List<String> confList = params.get(QueryParamsEnum.confid.toString());
		String confId = confList.get(0);

		logger.debug("confId: {}", confId);
		ConferenceUnit confHd = cnfFactory.getMsrpConfById(confId);


		List<String> sessLst = params.get(QueryParamsEnum.sessid.toString());
		String sessId = sessLst.get(0);

		logger.debug("sessId: {}", sessId);
		MsrpSessionHd sessHd = msrpSessFactory.getMsrpSessionById(sessId);

		HttpResponseStatus code = HttpResponseStatus.OK;
		try {
			confHd.attachSession(sessHd);
		} catch (SessionHdException e) {
			logger.error("attach session failure: {}", sessId, e);
			code = HttpResponseStatus.NOT_FOUND;
		}

		// Return result response with local URI		
		RestStubFullHttpResponse resp = new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, code);

//		StringBuilder contentStr = new StringBuilder();
//
//		contentStr.append( "confid="+confHd.getConfId() ) ;
//		contentStr.append('\n');
//
//		resp.content().writeBytes(contentStr.toString().getBytes(StandardCharsets.UTF_8));

		logger.debug("response: {}", resp);

		return resp;
	}


	private RestStubFullHttpResponse createConference(
			RestStubFullHttpRequest req, Map<String, List<String>> params) throws RestHttpException {

		// Get notifuri
		List<String> strList = params.get(QueryParamsEnum.notifuri.toString());
		String notifUri = strList.get(0);

		logger.debug("notifURI: {}", notifUri);

		// Check if uri is known and add if necessary
		SplyHttpCltConnector cnxClt = SplyRestCltFactory.getInstance().getCltConnector(notifUri);

		if (cnxClt == null) {
			// TODO - configure number of cnx in the pool
			SplyHttpCltPoolConnector pool = SplyRestCltFactory.getInstance().getOrCreateCltPoolConnector(notifUri, 2);
			cnxClt = pool.getConnector();
		}


		// Create session and map its Id, Ctx and cnx associated to notifUri

		ConferenceUnit confHd = cnfFactory.createConference();

		// Return result response with local URI		
		StringBuilder contentStr = new StringBuilder();

		contentStr.append( "confid="+confHd.getConfId() ) ;
		contentStr.append('\n');

		ByteBuf content = PooledByteBufAllocator.DEFAULT.buffer(contentStr.length(), contentStr.length()+10);
		content.writeBytes(contentStr.toString().getBytes(StandardCharsets.UTF_8));

		RestStubFullHttpResponse resp = new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, "text/plain; charset=UTF-8", content);
		
		logger.debug("response: {}", resp);

		return resp;
	}


	private RestStubFullHttpResponse createSession(RestStubFullHttpRequest req,	Map<String, List<String>> params) throws RestHttpException, UnknownHostException, URISyntaxException, SessionHdException {

		// Get notifuri
		List<String> strList = params.get(QueryParamsEnum.notifuri.toString());
		String notifUri = strList.get(0);

		logger.debug("notifURI: {}", notifUri);

		// Check if uri is known and add if necessary
		SplyHttpCltConnector cnxClt = SplyRestCltFactory.getInstance().getCltConnector(notifUri);

		if (cnxClt == null) {
			// TODO - configure number of cnx in the pool
			SplyHttpCltPoolConnector pool = SplyRestCltFactory.getInstance().getOrCreateCltPoolConnector(notifUri, 2);
			cnxClt = pool.getConnector();
		}


		// Create session and map its Id, Ctx and cnx associated to notifUri

		MsrpAddrServer localAddrSrv = MsrpSessionsFactory.getDefaultInstance().cnxFactory.selectLocalServer();
		
		boolean lockInput = false;
		boolean lockOutput = false;
		boolean discardInput = false;
		boolean discardOutput = false;
		
		List<String> lstParam;
		String param;
		
		lstParam = params.get(QueryParamsEnum.lockIn.toString());
		if (lstParam != null) {
			param = lstParam.get(0);
			lockInput = Boolean.parseBoolean(param);
		}

		lstParam = params.get(QueryParamsEnum.lockOut.toString());
		if (lstParam != null) {
			param = lstParam.get(0);
			lockOutput = Boolean.parseBoolean(param);
		}
		
		lstParam = params.get(QueryParamsEnum.discardIn.toString());
		if (lstParam != null) {
			param = lstParam.get(0);
			discardInput = Boolean.parseBoolean(param);
		}

		lstParam = params.get(QueryParamsEnum.discardOut.toString());
		if (lstParam != null) {
			param = lstParam.get(0);
			discardOutput = Boolean.parseBoolean(param);
		}

		int inactivityTO=120;
		lstParam = params.get(QueryParamsEnum.inactivityTO.toString());
		if (lstParam != null) {
			param = lstParam.get(0);
			inactivityTO = Integer.parseInt(param, 10 );
		}

		
		boolean notifRcvMsg = false ;
		boolean notifRcvMsgChunck = false ;
		boolean notifSendMsgFailure = false ;
		boolean notifSendChunckFailure = false ;

		
		lstParam = params.get(QueryParamsEnum.notifRcvMsg.toString());
		if (lstParam != null) {
			param = lstParam.get(0);
			notifRcvMsg = Boolean.parseBoolean(param);
		}
		
		lstParam = params.get(QueryParamsEnum.notifRcvMsgChunck.toString());
		if (lstParam != null) {
			param = lstParam.get(0);
			notifRcvMsgChunck = Boolean.parseBoolean(param);
		}
		
		lstParam = params.get(QueryParamsEnum.notifSendChunckFailure.toString());
		if (lstParam != null) {
			param = lstParam.get(0);
			notifSendChunckFailure = Boolean.parseBoolean(param);
		}
		
		lstParam = params.get(QueryParamsEnum.notifSendMsgFailure.toString());
		if (lstParam != null) {
			param = lstParam.get(0);
			notifSendMsgFailure = Boolean.parseBoolean(param);
		}
		
		NotifOptions notifOptions = new NotifOptions(notifRcvMsg, notifRcvMsgChunck, notifSendMsgFailure, notifSendChunckFailure);
		
		MsrpSessionListener supervListener = new McuSessListener(notifOptions);
		MsrpSessionListener sessListener = null;

		
		MsrpSessionHd sessHd = msrpSessFactory.createSession( localAddrSrv,
				sessListener, supervListener, inactivityTO,
				lockInput, lockOutput, discardInput, discardOutput);

		McuSessionContext ctxSession = new McuSessionContext(cnxClt, sessHd);

		sessHd.setUserContext(ctxSession);


		// Return result response with local URI		
		StringBuilder contentStr = new StringBuilder();

		contentStr.append( "sessid="+sessHd.getSessionId() ) ;
		contentStr.append('\n');
		contentStr.append( "localpath="+sessHd.getLocalPath() );
		contentStr.append('\n');

		ByteBuf content = PooledByteBufAllocator.DEFAULT.buffer(contentStr.length(), contentStr.length()+5);
		content.writeBytes(contentStr.toString().getBytes(StandardCharsets.UTF_8));

		RestStubFullHttpResponse resp = new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, "text/plain; charset=UTF-8", content);

		logger.debug("response: {}", resp);

		return resp;
	}


}
