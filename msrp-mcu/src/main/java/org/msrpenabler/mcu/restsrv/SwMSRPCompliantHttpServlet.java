package org.msrpenabler.mcu.restsrv;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.enabler.restlib.RestHttpConnector;
import org.enabler.restlib.RestHttpException;
import org.enabler.restlib.RestHttpServlet;
import org.enabler.restlib.wrapper.RestStubFullHttpRequest;
import org.enabler.restlib.wrapper.RestStubFullHttpResponse;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.msrpenabler.mcu.model.McuSessionContext;
import org.msrpenabler.mcu.restclt.SwMSRPCompliantMcuSessListener;
import org.msrpenabler.mculib.cnf.ConferenceUnit;
import org.msrpenabler.mculib.cnf.ConferencesFactory;
import org.msrpenabler.server.api.LockOptions;
import org.msrpenabler.server.api.MsrpMessageData;
import org.msrpenabler.server.api.MsrpSessionHd;
import org.msrpenabler.server.api.MsrpSessionListener;
import org.msrpenabler.server.api.MsrpSessionsFactory;
import org.msrpenabler.server.api.NotifOptions;
import org.msrpenabler.server.api.SupervisedMsrpSession;
import org.msrpenabler.server.cnx.MsrpAddrServer;
import org.msrpenabler.server.exception.SessionHdException;
import org.msrpenabler.server.exception.TransactionException;
import org.msrpenabler.server.util.MsrpSyntaxRegexp;
import org.rest.stub.sply.client.SplyHttpCltConnector;
import org.rest.stub.sply.client.SplyHttpCltPoolConnector;
import org.rest.stub.sply.client.SplyRestCltFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.management.OperatingSystemMXBean;


/**
 * @author Julien Bellanger
 *
 * An alternate API to be compliant with an other Switch MSRP provider (usage deprecated)
 */
@SuppressWarnings("restriction")
public class SwMSRPCompliantHttpServlet implements RestHttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(SwMSRPCompliantHttpServlet.class);

	private final MsrpSessionsFactory msrpSessFactory = MsrpSessionsFactory.getDefaultInstance();
	private final ConferencesFactory cnfFactory = ConferencesFactory.getDefaultInstance();

	/**
	 * 
	 */
	public void handleRcvRequest(RestStubFullHttpRequest req, RestHttpConnector cnxToRespond) {
		logger.debug("Receive on Cnx {}, request: {}", cnxToRespond, req);

		//QueryStringDecoder query = new QueryStringDecoder(req.getUri());
		
		String url = " http://FakeHostForSwCompliance"+req.getUri();
		URL urlPath;
		String path = null;
		
		String[] queryParams = null;

		// INCORRECT TO USE parameters() as the query is not originally encoded
		//Map<String, List<String>> params = query.parameters();

		try {
			urlPath = new URL(url);
			path = urlPath.getPath().substring(urlPath.getPath().lastIndexOf('/')+1);

			if (urlPath.getQuery() != null) {
				queryParams = urlPath.getQuery().split("&");
			}
			else {
				queryParams = new String[0];
			}
			
		} catch (Exception e1) {
			logger.error("URL parsing error {}", url, e1);
			queryParams = new String[0];
		}

		Map<String, List<String>> params = new HashMap<String, List<String>>();
		for (String param: queryParams) {
			String[] keyValue = param.split("=");
			List<String> list = new ArrayList<String>();
			
			if (keyValue.length > 1) {
				list.add(keyValue[1]);
			}
			params.put(keyValue[0], list);
		}
		
		logger.debug("received query url: {}, path: {}, params size {}", url, path, params.size());								


		HttpResponseStatus status = null; 
		RestStubFullHttpResponse httpResponse = null;

		SwMSRPCompliantPathMethodEnum ePath = SwMSRPCompliantPathMethodEnum.getValueOf(path);

		try {

			switch (ePath) {

			case CREATE_HUB:
				httpResponse = createHub(req, params);
				break;

			case DELETE_HUB:
				httpResponse = deleteHub(req, params);
				break;

			case CREATE_SESSION:
				httpResponse = createSession(req, params);
				break;

			case UPDATE_SESSION:
				httpResponse = updateSession(req, params);
				break;

			case DELETE_SESSION:
				httpResponse = deleteSession(req, params);
				break;

			case BIND_SESSION:
				httpResponse = bindSession(req, params);
				break;

			case CLOSE_SESSION:
				httpResponse = closeSession(req, params);
				break;

			case SEND_MSG:
				httpResponse = sendMsg(req, params);
				break;

			case STATS:
				httpResponse = getStats(req, params);
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





	/**
	 * createHub
	 * 
	 * @param req
	 * @param params
	 * @return
	 * @throws RestHttpException
	 */
	private RestStubFullHttpResponse createHub(
			RestStubFullHttpRequest req, Map<String, List<String>> params) throws RestHttpException {

		// TODO -
		// TODO - CB is not currently used 
		// TODO -
		// Get cb
		List<String> strList = params.get(SwMSRPCompliantQueryParamsEnum.cb.toString());
		String notifUri = null;

		// TODO - Notif usage for Hub not define yet
		//SplyHttpCltConnector cnxClt=null;
		
		if (strList != null) {
			notifUri = strList.get(0);

			logger.debug("notifURI: {}", notifUri);
	
			// Check if uri is known and add if necessary

//			SplyHttpCltPoolConnector pool = SplyRestCltFactory.getInstance().getOrCreateCltPoolConnector(notifUri, 2);
//			cnxClt = pool.getConnector();
		}


		// Create conference and map its Id, Ctx and cnx associated to notifUri

		// TODO -
		// TODO - Add a listener on the Conference to catch specific Conf event (inactivity, end of conf event ?...)
		// TODO -

		ConferenceUnit confHd = cnfFactory.createConference();

		
		StringBuilder contentStr = new StringBuilder();

		contentStr.append( "hub="+confHd.getConfId() ) ;
		contentStr.append('\n');
		
		ByteBuf content = PooledByteBufAllocator.DEFAULT.buffer();
		
		content.writeBytes(contentStr.toString().getBytes(StandardCharsets.UTF_8));
		
		// Return result response with local URI		
		RestStubFullHttpResponse resp = new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, "text/plain; charset=UTF-8", content);

		logger.debug("response: {}", resp);

		return resp;
	}



	/**
	 * deleteHub
	 * 
	 * @param req
	 * @param params
	 * @return
	 */
	private RestStubFullHttpResponse deleteHub(RestStubFullHttpRequest req,
			Map<String, List<String>> params) {

		// Get Hub
		String hub=null;
		List<String> lstParam = params.get(SwMSRPCompliantQueryParamsEnum.hub.toString());
		if (lstParam != null) {
			hub = lstParam.get(0);
		}
		
		logger.debug("confId: {}", hub);
		ConferenceUnit confHd = cnfFactory.getMsrpConfById(hub);
		
		HttpResponseStatus code = HttpResponseStatus.OK;
		RestStubFullHttpResponse resp;

		if (confHd == null) {
			code = HttpResponseStatus.NOT_FOUND;
			resp = new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, code);
			return resp;
		}

		// Detach all associated sessions
		confHd.detachAllSessions();
		
		resp = new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, code);
		
		return resp;
	}

	
	
	/**
	 * 
	 * createSession
	 * 
	 * @param req
	 * @param params
	 * @return
	 * @throws RestHttpException
	 * @throws UnknownHostException
	 * @throws URISyntaxException
	 * @throws SessionHdException
	 */
	private RestStubFullHttpResponse createSession(RestStubFullHttpRequest req,	Map<String, List<String>> params) throws RestHttpException, UnknownHostException, URISyntaxException, SessionHdException {

		// Get notifuri
		List<String> strList = params.get(SwMSRPCompliantQueryParamsEnum.cb.toString());
		String notifUri = null;
		SplyHttpCltConnector cnxClt = null;
		
		if (strList != null) {
			notifUri = strList.get(0);

			logger.debug("notifURI: {}", notifUri);

			// Check if uri is known and add if necessary
			SplyHttpCltPoolConnector pool = SplyRestCltFactory.getInstance().getOrCreateCltPoolConnector(notifUri, 2);
			cnxClt = pool.getConnector();
		}

		if (cnxClt == null) {
			logger.debug("Fail to get notifURI [cb parameter]: {}", notifUri);
			return new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
		}

		List<String> lstParam;
		String param;
		

		// Get Hub
		String hub=null;
		ConferenceUnit confHd = null;
		
		lstParam = params.get(SwMSRPCompliantQueryParamsEnum.hub.toString());
		if (lstParam != null) {
			hub = lstParam.get(0);
		
			logger.debug("confId: {}", hub);
			confHd = cnfFactory.getMsrpConfById(hub);
		}
		else {
			logger.debug("Fail to get confId [hub parameter]: {}", lstParam);
			return new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
		}		
		
		// Create session and map its Id, Ctx and cnx associated to notifUri
		MsrpAddrServer localAddrSrv = MsrpSessionsFactory.getDefaultInstance().cnxFactory.selectLocalServer();
		
		byte lock=0;
		boolean sf = false;

		lstParam = params.get(SwMSRPCompliantQueryParamsEnum.lock.toString());
		if (lstParam != null) {
			param = lstParam.get(0);
			lock = Byte.parseByte(param);
		}

		boolean queue = (lock & 1) == 1;
		boolean directmsg = (lock & 2) == 2;
		boolean drop = (lock & 4) == 4;

		lstParam = params.get(SwMSRPCompliantQueryParamsEnum.sf.toString());
		if (lstParam != null) {
			param = lstParam.get(0);
			sf = Boolean.parseBoolean(param);
		}
		
		
		boolean lockInput = false;
		boolean lockOutput = queue;
		boolean discardInput = drop;
		boolean discardOutput = false;		
		
		boolean notifMsg = directmsg;
		boolean notifMsgChunck = false;
		
		int inactivityTO=120;
		lstParam = params.get(SwMSRPCompliantQueryParamsEnum.sesstimeout.toString());
		if (lstParam != null) {
			param = lstParam.get(0);
			inactivityTO = Integer.parseInt(param, 10 );
		}

		
		NotifOptions notifOptions = new NotifOptions(notifMsg, notifMsgChunck, sf, false);

		MsrpSessionListener supervListener = new SwMSRPCompliantMcuSessListener(notifOptions);
		MsrpSessionListener sessListener = null;
		
		LockOptions lockOptions = new LockOptions(lockInput, lockOutput, discardInput, discardOutput);
		
		MsrpSessionHd sessHd = msrpSessFactory.createSession( localAddrSrv,
				sessListener, supervListener, inactivityTO, lockOptions);

		McuSessionContext ctxSession = new McuSessionContext(cnxClt, sessHd);

		sessHd.setUserContext(ctxSession);
		ctxSession.setAttribute("hub", confHd);

		
		// Attach to the Hub
		HttpResponseStatus code = HttpResponseStatus.OK;
		boolean isOk=true;
		try {
			confHd.attachSession(sessHd);
		} catch (SessionHdException e) {
			logger.error("attach session failure: {}", sessHd.getSessionId(), e);
			code = HttpResponseStatus.NOT_FOUND;
			
			isOk=false;
		}
		

		// Map the local_path to the sess Id
		// TODO
		// TODO
		//  should be equal to --> MsrpSyntaxRegexp.getSessionId(localPath)
		// TODO
		// TODO
		logger.info("Return sess path: {}, for session Id {}, sessId extract from path: {}", sessHd.getLocalPath(), sessHd.getSessionId(), 
								MsrpSyntaxRegexp.getSessionId(sessHd.getLocalPath()));
		
		RestStubFullHttpResponse resp;
		
		if (isOk) {
			StringBuilder contentStr = new StringBuilder();
			
			contentStr.append( "sess="+sessHd.getLocalPath() ) ;
			contentStr.append('\n');
		
			ByteBuf content = PooledByteBufAllocator.DEFAULT.buffer();

			content.writeBytes(contentStr.toString().getBytes(StandardCharsets.UTF_8));

			// Return result response with local URI		
			resp = new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, "text/plain; charset=UTF-8", content);
		}
		else
		{
			// Return result response with local URI		
			resp = new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, code);
		}
		
		logger.debug("response: {}", resp);

		return resp;
	}



	/**
	 * 
	 * bindSession
	 * 
	 * @param req
	 * @param params
	 * @return
	 */
	private RestStubFullHttpResponse bindSession(RestStubFullHttpRequest req,
			Map<String, List<String>> params) {

		boolean setupOk=true;

		List<String> paramLst = params.get(SwMSRPCompliantQueryParamsEnum.sess.toString());
		String sessLocalPath = paramLst.get(0);
		String sessId=null;
		
		try {
			sessId = MsrpSyntaxRegexp.getSessionId(sessLocalPath);
		} catch (SessionHdException e1) {
			logger.warn("Failed to get sess {}", sessLocalPath);
			setupOk=false;
		}

		logger.debug("sessId: {}", sessId);
		MsrpSessionHd sessHd = msrpSessFactory.getMsrpSessionById(sessId);
		HttpResponseStatus code = HttpResponseStatus.OK;

		if (sessHd == null) {
			code = HttpResponseStatus.NOT_FOUND;
			setupOk=false;
		}
		else {

			paramLst = params.get(SwMSRPCompliantQueryParamsEnum.peer.toString());
			String remote = paramLst.get(0);

			try {
				sessHd.setRemotePath(remote);
			} catch (SessionHdException e) {
				logger.warn("set remote path failure: {}", sessId, e);
				code = HttpResponseStatus.INTERNAL_SERVER_ERROR;
				setupOk=false;
			}

			
			paramLst = params.get(SwMSRPCompliantQueryParamsEnum.active.toString());
			Integer active = Integer.parseInt(paramLst.get(0));

			
			// TODO set connection listener and TimeOut
			if (setupOk) {

				if (active == 0) {
					try {
						sessHd.bind();
					} catch (Exception e) {
						logger.error("bind session failure: {}", sessId, e);
						code = HttpResponseStatus.INTERNAL_SERVER_ERROR;
					}
				}
				else {
					try {
						sessHd.connect(10);
					} catch (Exception e) {
						logger.error("connect active session failure: {}", sessId, e);
						code = HttpResponseStatus.INTERNAL_SERVER_ERROR;
					}
				}
			}
		}

		// Return result response with local URI		
		RestStubFullHttpResponse resp = new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, code);

		logger.debug("response: {}", resp);

		return resp;
	}
	
	/**
	 * 
	 * deleteSession
	 * 
	 * @param req
	 * @param params
	 * @return
	 */
	private RestStubFullHttpResponse deleteSession(RestStubFullHttpRequest req,
			Map<String, List<String>> params) {

		List<String> paramLst = params.get(SwMSRPCompliantQueryParamsEnum.sess.toString());
		String sessLocalPath = paramLst.get(0);
		String sessId=null;
		
		boolean setupOk=true;
		
		try {
			sessId = MsrpSyntaxRegexp.getSessionId(sessLocalPath);
		} catch (SessionHdException e1) {
			logger.warn("Failed to get sess {}", sessLocalPath);
			setupOk=false;
		}

		logger.debug("sessId: {}", sessId);
		MsrpSessionHd sessHd = null;
		if (sessId != null) {
			sessHd = msrpSessFactory.getMsrpSessionById(sessId);
		}
		
		HttpResponseStatus code = HttpResponseStatus.OK;

		if (sessHd == null) {
			code = HttpResponseStatus.NOT_FOUND;
			setupOk=false;
		}
		else {
			McuSessionContext ctx = (McuSessionContext) sessHd.getUserContext();
			ConferenceUnit confHd = (ConferenceUnit) ctx.getAttribute("hub");
			
			if (confHd != null) {
				try {
					confHd.detachSession(sessHd);
				} catch (SessionHdException e) {
					logger.warn("Failed to detach session {} from conf {} ", sessHd, confHd.getConfId(), e);
				}
			}

			msrpSessFactory.unrefMsrpSession(sessHd);
		}

		// Return result response with local URI		
		RestStubFullHttpResponse resp;

		
		// TODO - Add counters in content
		
		if (setupOk) {
			StringBuilder contentStr = new StringBuilder();
	
			contentStr.append( "nrmsg=0\n") ;
			contentStr.append( "nwmsg=0\n") ;
			contentStr.append( "nrctl=0\n") ;
			contentStr.append( "nwctl=0\n") ;
			contentStr.append( "nrbytes=0\n") ;
			contentStr.append( "nwbytes=0\n") ;
			contentStr.append( "nrgeoloc=0\n") ;
			contentStr.append( "nwgeoloc=0\n") ;
			contentStr.append( "nrftohttp=0\n") ;
			contentStr.append( "nwftohttp=0\n") ;
	
			ByteBuf content = PooledByteBufAllocator.DEFAULT.buffer();

			content.writeBytes(contentStr.toString().getBytes(StandardCharsets.UTF_8));

			// Return result response with local URI		
			resp = new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, "text/plain; charset=UTF-8", content);
			
		}
		else {
			resp = new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, code);
		}
		
		logger.debug("response: {}", resp);
		
		return resp;
		
	}




	private RestStubFullHttpResponse closeSession(RestStubFullHttpRequest req,
			Map<String, List<String>> params) {

		List<String> paramLst = params.get(SwMSRPCompliantQueryParamsEnum.sess.toString());
		String sessLocalPath = paramLst.get(0);
		String sessId=null;
		
		try {
			sessId = MsrpSyntaxRegexp.getSessionId(sessLocalPath);
		} catch (SessionHdException e1) {
			logger.warn("Failed to get sess {}", sessLocalPath, e1);
		}

		logger.debug("sessId: {}", sessId);
		MsrpSessionHd sessHd = msrpSessFactory.getMsrpSessionById(sessId);
		
		HttpResponseStatus code = HttpResponseStatus.OK;

		
		if (sessHd == null) {
			code = HttpResponseStatus.NOT_FOUND;
		}
		else {
			
			McuSessionContext ctx = (McuSessionContext) sessHd.getUserContext();
			ConferenceUnit confHd = (ConferenceUnit) ctx.getAttribute("hub");
			
			if (confHd != null) {
				try {
					confHd.detachSession(sessHd);
				} catch (SessionHdException e) {
					logger.warn("Failed to detach session {} from conf {} ", sessHd, confHd.getConfId(), e);
				}
			}

			try {
				sessHd.close();
			} catch (TransactionException | SessionHdException e) {
				logger.warn("Failed to close session {}  ", sessHd, e);;
				code = HttpResponseStatus.INTERNAL_SERVER_ERROR;
			}
		}

		// Return result response with local URI		
		RestStubFullHttpResponse resp = new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, code);
		
		return resp;
	}


	private RestStubFullHttpResponse updateSession(RestStubFullHttpRequest req,
			Map<String, List<String>> params) {

		List<String> paramLst = params.get(SwMSRPCompliantQueryParamsEnum.sess.toString());
		String sessLocalPath = paramLst.get(0);
		String sessId=null;
		
		try {
			sessId = MsrpSyntaxRegexp.getSessionId(sessLocalPath);
		} catch (SessionHdException e1) {
			logger.warn("Failed to get sess {}", sessLocalPath);
		}

		logger.debug("sessId: {}", sessId);
		SupervisedMsrpSession sessHd = (SupervisedMsrpSession) msrpSessFactory.getMsrpSessionById(sessId);
		
		HttpResponseStatus code = HttpResponseStatus.OK;

		if (sessHd == null) {
			code = HttpResponseStatus.NOT_FOUND;
			return new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, code);
		}

		// Get params
		byte lock=0;

		List<String> lstParam = params.get(SwMSRPCompliantQueryParamsEnum.lock.toString());
		String param;
		if (lstParam != null) {
			param = lstParam.get(0);
			lock = Byte.parseByte(param);
		}

		boolean queue = (lock & 1) == 1;
		boolean directmsg = (lock & 2) == 2;
		boolean drop = (lock & 4) == 4;

		boolean lockInput = false;
		boolean lockOutput = queue;
		boolean discardInput = drop;
		boolean discardOutput = false;		

		sessHd.updateLockOutput(lockOutput);
		sessHd.updateLockInput(lockInput);
		sessHd.updateDiscardInput(discardInput);
		sessHd.updateDiscardOutput(discardOutput);
		
		
		boolean sf = false;
		lstParam = params.get(SwMSRPCompliantQueryParamsEnum.sf.toString());
		if (lstParam != null) {
			param = lstParam.get(0);
			sf = Boolean.parseBoolean(param);
		}
		
		SwMSRPCompliantMcuSessListener supervisorListener = (SwMSRPCompliantMcuSessListener) sessHd.getSupervisorListener();
		
		supervisorListener.getNotifOptions().setNotifMsg(directmsg);
		supervisorListener.getNotifOptions().setNotifSendMsgFailure(sf);

		// Return result response with local URI		
		RestStubFullHttpResponse resp = new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, code);


		return resp;
	}


	private RestStubFullHttpResponse sendMsg(RestStubFullHttpRequest req,
			Map<String, List<String>> params) {
		
		List<String> paramLst = params.get(SwMSRPCompliantQueryParamsEnum.sess.toString());
		String sessLocalPath = paramLst.get(0);
		String sessId=null;
		
		try {
			sessId = MsrpSyntaxRegexp.getSessionId(sessLocalPath);
		} catch (SessionHdException e1) {
			logger.warn("Failed to get sess {}", sessLocalPath);
		}

		logger.debug("sessId: {}", sessId);
		SupervisedMsrpSession sessHd = (SupervisedMsrpSession) msrpSessFactory.getMsrpSessionById(sessId);
		
		HttpResponseStatus code = HttpResponseStatus.OK;

		if (sessHd == null) {
			code = HttpResponseStatus.NOT_FOUND;
			return new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, code);
		}


		String contentType = req.headers().get("Content-Type");
		ByteBuf rawContent = req.content();
		
		// Retain the req buffer that will be released by the RestHttPConnector 
		rawContent.retain();
		
		MsrpMessageData msrpMessage = MsrpSessionsFactory.createMsg(contentType, rawContent) ;
		
		try {
			//sessHd.sendMsrpMessage(msrpMessage);
			sessHd.sendMsrpMessageNoLock(msrpMessage);
			
		} catch (SessionHdException | TransactionException e) {
			logger.warn("Failed to send msg: {} , on sessHd {}", msrpMessage, sessId, e);
			code = HttpResponseStatus.INTERNAL_SERVER_ERROR;
			return new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, code);
		}
		
		logger.debug("Content count : {}, msg created: {}", rawContent.refCnt(), msrpMessage.refCnt());
		
		code = HttpResponseStatus.OK;
		return new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, code);

	}

	
	/**
	 * 
	 * getStats
	 * 
	 * @param req
	 * @param params
	 * @return
	 */
	private static RestStubFullHttpResponse getStats(RestStubFullHttpRequest req,
			Map<String, List<String>> params) {

		RestStubFullHttpResponse resp = new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

		StringBuilder contentStr = new StringBuilder();

		OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(  
                OperatingSystemMXBean.class);  
		// What % CPU load this current JVM is taking, from 0.0-1.0  
		int cpuProcLoad = (int) (osBean.getProcessCpuLoad() * 100);
		
		
		// What % load the overall system is at, from 0.0-1.0  
		int cpuSystemLoad = (int) (osBean.getSystemCpuLoad() * 100);  
		
		Sigar sigar = new Sigar(); 


		try {
			FileSystemUsage tmp = sigar.getFileSystemUsage("/tmp");  
			FileSystemUsage log = sigar.getFileSystemUsage("/var/log");  

			double tmpFreePercent = 50;
			double logFreePercent = 50;

			tmpFreePercent = tmp.getUsePercent();
			logFreePercent = log.getUsePercent();

			long freeram = sigar.getMem().getFree();

			contentStr.append( "cpu=").append( cpuSystemLoad).append('\n');
			contentStr.append( "cpuprocess=").append( cpuProcLoad).append('\n');
			//			contentStr.append( "freeram=").append(freeram).append('\n');
			//			contentStr.append( "freetmp=").append(tmpFreePercent).append('\n');
			//			contentStr.append( "freelog=").append(logFreePercent).append('\n');
			contentStr.append( "freeram=").append("2000").append('\n');
			contentStr.append( "freetmp=").append("2000").append('\n');
			contentStr.append( "freelog=").append("2000").append('\n');
			contentStr.append( "hubs=").append(ConferencesFactory.getCurrentConferenceNumber()).append('\n');
			contentStr.append( "sess=").append(MsrpSessionsFactory.getCurrentSessionNumber()).append('\n');

			ByteBuf content = PooledByteBufAllocator.DEFAULT.buffer();

			content.writeBytes(contentStr.toString().getBytes(StandardCharsets.UTF_8));

			// Return result response with local URI		
			resp = new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, "text/plain; charset=UTF-8", content);


		} catch (SigarException e) {
			logger.warn("Failed to retrieve Stat", e);

			resp = new RestStubFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
		}
		

		//logger.debug("response: {}", resp);

		return resp;


	}


	
}
