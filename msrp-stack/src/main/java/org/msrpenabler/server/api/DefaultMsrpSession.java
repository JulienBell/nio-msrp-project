package org.msrpenabler.server.api;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.msrpenabler.server.api.internal.MsrpChunkAggregatorImpl;
import org.msrpenabler.server.cnx.MsrpAddrServer;
import org.msrpenabler.server.cnx.MsrpConnexion;
import org.msrpenabler.server.exception.SessionHdException;
import org.msrpenabler.server.exception.TransactionException;
import org.msrpenabler.server.net.ConnectionFuture;
import org.msrpenabler.server.net.MsrpMessageWrapper;
import org.msrpenabler.server.net.NioMsrpSockClientBootStrap;
import org.msrpenabler.server.net.SessionTransactionData;
import org.msrpenabler.server.util.MsrpSyntaxRegexp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * 
 * TODO List
 * 
 * 	- Suspend read on input Cnx in order to manage flow control or Cnx lost simulation
 *  ( see Channel.config().setAutoRead(false) option 
 *    and call ctx.read() each times you want to allow socket read -> should be always done manually
 *    To check: capability to set on/off this option on an active channel ? --> a priori a la lecture du code oui)
 * 
 *  - Deactivate temporary automatic 200 OK response on a given session input to manage flow control at session level
 * 
 * 
 * 
 */


public class DefaultMsrpSession extends SessionTransactionData implements MsrpSessionHd  {

    private static final Logger logger =
        LoggerFactory.getLogger(DefaultMsrpSession.class);

    private final MsrpSessionsFactory sessMgnt;

    private final String sessionId;
	
	private String localURI;

	private MsrpConnexion msrpCnx;
	
	protected MultiListeners defaultSessListeners = new MultiListeners();

	private boolean isReusable;

	private boolean isUnRef = false;


    private LinkedList<String> listInMsgId = null ; // Ordered received msg Id list
	private HashMap<String, MsrpChunkAggregator> mapInChunckedMsg = null ;

//	private LinkedList<String> listOutMsgId = null ; // Ordered sent msg Id list 
	private HashMap<String, MsrpChunkAggregator> mapOutChunckedMsg = null ;
	
	private HashSet<EnumSessionOptions> setOfOptions = new HashSet<EnumSessionOptions>();

	private int inactivityTO;

	private Object userCtx;

	private MsrpAddrServer localAddr;
	
	public MultiListeners getSessListener() {
		return defaultSessListeners;
	}

	public void setSessListener(MsrpSessionListener sessListener) {
		addSessListener( sessListener );
	}

	public void addSessListener(MsrpSessionListener sessListener) {
		defaultSessListeners.addListener( sessListener );
	}

	@Override
	public void removeSessListener(MsrpSessionListener sessListener) {
		this.defaultSessListeners.removeListener(sessListener);
		
	}
	

	/**
	 * 
	 * @param sessMgnt
     * @param inet
	 * @param reusable
     * @param inactivityTO
	 * @throws URISyntaxException
	 * @throws UnknownHostException
	 * @throws SessionHdException
	 */
	public DefaultMsrpSession(MsrpSessionsFactory sessMgnt, InetAddress inet, boolean reusable, int inactivityTO) throws URISyntaxException, UnknownHostException, SessionHdException {
		this(sessMgnt, inet, null, reusable, inactivityTO);
	}

	/**
	 *   Create a Session managed by the given Session Manager
	 * @param sessMgnt
     * @param host
	 * @param sessListener
	 * @param reusable
     * @param inactivityTO
	 * @throws URISyntaxException
	 * @throws UnknownHostException 
	 * @throws SessionHdException 
	 */
	public DefaultMsrpSession(MsrpSessionsFactory sessMgnt, String host, MsrpSessionListener sessListener,
			boolean reusable, int inactivityTO) throws URISyntaxException, UnknownHostException, SessionHdException {

		
		super(null, null);
		this.sessMgnt = sessMgnt;
		this.sessionId = sessMgnt.getNewMsrpSessionId(this);	
		this.inactivityTO = inactivityTO;

		this.localAddr = sessMgnt.cnxFactory.selectLocalServer();
		String localURI = localAddr.getLocalURI();
		
		init(localURI, sessListener, reusable);
	}
	

    /**
     *
     * @param sessMgnt
     * @param inet
     * @param sessListener
     * @param reusable
     * @param inactivityTO
     * @throws URISyntaxException
     * @throws SessionHdException
     * @throws UnknownHostException 
     */
	public DefaultMsrpSession(MsrpSessionsFactory sessMgnt, InetAddress inet,
			MsrpSessionListener sessListener, boolean reusable, int inactivityTO) throws URISyntaxException, SessionHdException, UnknownHostException {
		super(null, null);
		this.sessMgnt = sessMgnt;
		this.sessionId = sessMgnt.getNewMsrpSessionId(this);
		this.inactivityTO = inactivityTO;

		this.localAddr = sessMgnt.cnxFactory.selectLocalServer();
		String localURI = localAddr.getLocalURI();
		
		init(localURI, sessListener, reusable);
	}

    /**
    *
    * @param sessMgnt
    * @param inet
    * @param sessListener
    * @param reusable
    * @param inactivityTO
    * @throws URISyntaxException
    * @throws SessionHdException
    */
	public DefaultMsrpSession(MsrpSessionsFactory sessMgnt, MsrpAddrServer mAddr,
			MsrpSessionListener sessListener, boolean reusable, int inactivityTO) throws URISyntaxException, SessionHdException {
		super(null, null);
		this.sessMgnt = sessMgnt;
		this.sessionId = sessMgnt.getNewMsrpSessionId(this);
		this.inactivityTO = inactivityTO;
		this.localAddr = mAddr;
		
		String localURI = localAddr.getLocalURI();

		init(localURI, sessListener, reusable);
	}

	private void init(String localURI, MsrpSessionListener sessListener,
			boolean reusable) throws URISyntaxException, SessionHdException {
		
		this.localURI = localURI;
		
		if (sessListener != null) {
			addSessListener( sessListener );
		}
		this.isReusable = reusable;
		
		String localPath = createLocalPath(localURI);
		
		super.setLocalSessPath(localPath);
		
		
		setOption(EnumSessionOptions.SESS_NOTIFY_COMPLETED_CHUNCKED_MSG);
		setOption(EnumSessionOptions.SESS_NOTIFY_EACH_CHUNCK);
	}
	
	
	/**
	 * 
	 * @param option : session option to add in the options list
	 */
	public void setOption(EnumSessionOptions option) {

		setOfOptions.add(option);
	}

	private void checkValidity() throws SessionHdException {
		if (isUnRef) {
			throw new SessionHdException("This session has been unref and could not be used anymore "+sessionId);
		}
	}


	private String createLocalPath(String localURI) throws URISyntaxException {
		
		return (localURI + "/" + sessionId + ";tcp");
	}

	@Override
	public void setRemotePath(String remotePath) throws SessionHdException {

		checkValidity();
		
		// Check if remote path is already set
		if (getRemotePath() != null) {
			throw new SessionHdException("Remote path already set to: "+ remotePath);
		}
		
		// Check remote path syntax
		if (! MsrpSyntaxRegexp.patt_msrpUri.matcher(remotePath).matches() ) {
			throw new SessionHdException("Remote path invalid msrp_uri syntax: "+ remotePath);
		}
		
		super.setRemoteSessPath(remotePath);
	}

	
	/**
	 * Connect to the remote path 
	 * @throws Exception 
	 */
	@Override
    public ConnectionFuture connect(int connectTOsec) throws Exception {
    	 	
    	final ConnectionFuture cnxFuture;
    	final SessionTransactionData sessData = this;

    	ChannelFutureListener listenerCnx = new ChannelFutureListener() {
			
			@Override
			public void operationComplete(ChannelFuture future) {

				MsrpConnexion cnx = ((ConnectionFuture) future).getMsrpConnection();
				
				logger.info("Call listener complete Cnx ");
				if (future.isSuccess()) {
					cnx.evtCnxCltSuccess(sessData);
				}
				else {
					cnx.evtCnxCltFailure(sessData, future.cause());
				}
				
			}
		};
    	

		// In case of msrCnx reuse ??
		if (msrpCnx != null && msrpCnx.getChannel() != null){

			if (msrpCnx.checkLocalPathRegister(getLocalSessId())) {

				cnxFuture = new ConnectionFuture(msrpCnx);

				cnxFuture.addListener(listenerCnx);

				TransactionException cause = new TransactionException("Session is already connected for local path: "+ getLocalSessPath());
				cnxFuture.setFailure(cause);

				return cnxFuture;
			}
		}

		msrpCnx = new MsrpConnexion();
		
    	InetAddress remoteAddr = MsrpSyntaxRegexp.getInetAddress(getRemoteSessPath());
    	int remotePort = MsrpSyntaxRegexp.getPortAddress(getRemoteSessPath());

        NioMsrpSockClientBootStrap clientBootStrap = new NioMsrpSockClientBootStrap(sessMgnt.cnxFactory.getWorkerGroup(), msrpCnx, localAddr);
    	
    	ChannelFuture chFuture = clientBootStrap.connect(remoteAddr, remotePort, connectTOsec * 1000);
    	
		logger.info("Connect channel {}", msrpCnx.getChannel());

		msrpCnx.setChannel(chFuture.channel());
        
        // Now that the channel is set we can define the future
		logger.info("Connect transaction Handler obj {}", this);

		cnxFuture = new ConnectionFuture(msrpCnx);

        cnxFuture.addListener(listenerCnx);
        
        chFuture.addListener( new ChannelFutureListener() {
            public void operationComplete(ChannelFuture chFuture) {

            	logger.info("Call listener complete Cnx channel {}", chFuture.isSuccess());
            	if (chFuture.isSuccess()) {
					cnxFuture.setSuccess();
            	}
            	else {
            		logger.warn("Failure on client connexion {}", chFuture.cause());
            	
            		cnxFuture.setFailure(new TransactionException("Connection failed for local path: "+ getLocalSessPath()
    						+"with remote path: "+ getRemoteSessPath(), chFuture.cause()));
            	}
            }
        });
        
        // Return future
		return cnxFuture;
    }

	
	@Override
	public MsrpSessionHdFuture bind() throws SessionHdException, TransactionException {

		checkValidity();
		
		// check that remote path is set
		if (getRemoteSessPath() == null) {
			throw new SessionHdException("Remote Path is not set for session "+sessionId);
		}
		
		// Create or reuse a connection Channel and ask to bind and wait Remote Cnx
		//msrpCnx = sessMgnt.cnxFactory.bind( this );
		
		MsrpAddrServer addrServ = sessMgnt.cnxFactory.getServerList().get(getLocalURI()); 
		
		if ( addrServ.mapBindCnx.containsKey(sessionId) ) {
			logger.error("A session with the same local path already exist: {}", getLocalPath());
			throw new SessionHdException("A session with the same local path already exist: "+getLocalPath());
		}
		else {
			
			msrpCnx = new MsrpConnexion();

			// Create a listener on the server channel executor
			ConnectionFuture cnxInBoundCnx = new ConnectionFuture(msrpCnx, addrServ.bootStrapServ.getChannelBind());
			
			ChannelFutureListener listener = new ChannelFutureListener() {
				
				public void operationComplete(ChannelFuture future) {

					logger.info("Connection bind complete {}", this);
				}

			};
			
			cnxInBoundCnx.addListener(listener);
			
			setCnxFuture(cnxInBoundCnx);
			
			addrServ.mapBindCnx.put(sessionId, msrpCnx);
		
			msrpCnx.setBindPath(this);
			
			return cnxInBoundCnx;
		}
		
	}

	@Override
	public void close() throws TransactionException, SessionHdException{

		checkValidity();

		// If connection not yet bind or connected, nothing todo
		if (msrpCnx != null) {
			msrpCnx.closeSess(this);
		}
	}



	@Override
	public String getRemotePath() {
		return super.getRemoteSessPath();
	}

	@Override
	public String getLocalPath() {
		return super.getLocalSessPath();
	}

	public String getLocalURI() {
		return localURI;
	}
	
	@Override
	public String getSessionIdNoChk() {
		return sessionId;
	}
	
	@Override
	public String getSessionId() {
		//checkValidity();
		return sessionId;
	}

	@Override
	public boolean isReusable() throws SessionHdException {
		checkValidity();
		return isReusable;
	}

	@Override
	public boolean isRef() {
		return !isUnRef;
	}
	
	@Override
	public boolean isUnRef() {
		return isUnRef;
	}

	
	public void setUnref() {
		sessMgnt.unrefMsrpSession(this);
		isUnRef = true;
	}

	public void setAsUnref() {
		isUnRef = true;
	}
	

	/**
	 * 
	 * @param msrpMessage : message to send
	 * @throws SessionHdException 
	 * @throws TransactionException 
	 */
	@Override
	public void sendMsrpMessage(MsrpMessageData msrpMessage) throws SessionHdException, TransactionException {

		checkValidity();
		
		// TODO - Add to message list sent out, if message has to be split into chunks
		// TODO - and set in this ChunkAggregator a field that indicate automatic chuncking
		
		// TODO - To split in chunck
		//		create all sub-chunck in a chunck Aggregator
		//		call the chunks sending in the EventExecutor thread in order to not block the caller ?
		//      set max chunk / sec to send ? or based on the handler traffic writer ?
		
		
		
		// TODO - If Failure-report explicitly set to no and Success-Report not set to yes,
		// TODO - it's not necessary to save the sending trace in out map as No event is waited
		
		// TODO - If Failure-report not set or set to yes, message is stored in the Transaction level
		// TODO - It's not necessary to save it at this session level
		
		// TODO - If Success-report set to yes, we may have to follow the message sending at this session level 
		// TODO - if we want to arm a timer and notify of no Success ??

		// TODO - If Failure-report set to partial, we may have to store msg at session level 
		// TODO - if we want to arm a timer and notify Failure ???
		
		
		if ( msrpMessage instanceof MsrpMessageWrapper) {
			logger.info("Send entire msg on msrpCnx {}", msrpCnx);
			msrpCnx.sendMsrpMessage(this, (MsrpMessageWrapper) msrpMessage);
		}
		else if ( msrpMessage instanceof MsrpChunkAggregator) {
			logger.info("Send chuncked msg on msrpCnx {}", msrpCnx);
			sendMsrpChunkedMsg( (MsrpChunkAggregator) msrpMessage);
		}
		else {
			throw new SessionHdException("Unknown type of message "+ msrpMessage.getClass().getName());
		}

	}

	
	/*
	 * Chunk Management
	 */

	/**
	 * 
	 * @throws SessionHdException 
	 * 
	 */
	@Override
	public MsrpChunkAggregator createMsrpChunkedMsg(MsrpMessageData msrpMsgToChunck, int chunckLength) throws SessionHdException {
		checkValidity();
		return null;
	}
	
	
	@Override
	public void sendMsrpChunkedMsg(MsrpChunkAggregator msrpAggChunk) throws SessionHdException, TransactionException {
		checkValidity();
		
		// Send all chuncks
		
		Iterator<MsrpChunkData> it = msrpAggChunk.iterator();
		
		while (it.hasNext()) {
			sendMsrpChunk(it.next(), false);
		}
		
	}


	@Override
	public MsrpChunkAggregator sendMsrpChunk(MsrpChunkData msrpChunk, boolean saveChunks) throws SessionHdException, TransactionException {
		
		checkValidity();
		
		// Check if message id in message list out
		String msgId = msrpChunk.getMessageId();
		
		if (! saveChunks) {
			msrpCnx.sendMsrpMessage(this, (MsrpMessageWrapper) msrpChunk);
			return null;
		}
		
		if (mapOutChunckedMsg == null) {
			mapOutChunckedMsg = new HashMap<String, MsrpChunkAggregator>();
		}
		
		MsrpChunkAggregator msg = mapOutChunckedMsg.get(msgId);
		if (msg == null) {
			msg = new MsrpChunkAggregatorImpl(msgId);
			mapOutChunckedMsg.put(msgId, msg);
		}

		if ( msg instanceof MsrpChunkAggregatorImpl) {

			((MsrpChunkAggregatorImpl) msg).addChunkedMsg(msrpChunk);
			
			msrpCnx.sendMsrpMessage(this, (MsrpMessageWrapper) msrpChunk);

		}
		else {
			throw new SessionHdException("Try to send a chunck on a msgId identical to a current standalone message"+msgId);
		}
		
		return msg;
	}


	@Override
	public void sendAbortMsrpChunck(MsrpChunkAggregator msrpAggChunk) throws SessionHdException{
		checkValidity();

        // TODO ---
	}



	/*
	 *  Internal event handler
	 *  
	 */
	@Override
	protected void evtReceiveMsg(MsrpMessageWrapper msg) {

		logger.info("Received message for {}", getLocalSessPath());
		logger.info("Content type: {}", msg.getContentType());
		
		if (msg.getContentLength() != 0) {
			if (logger.isDebugEnabled()) {
				
				if (msg.content().unwrap() != null) {
					logger.debug("Message received:\n{}", msg.content().unwrap().toString(Charset.forName("UTF-8")) );
				}
				else {
					logger.debug("Message received:\n{}", msg.content().toString(Charset.forName("UTF-8")) );
				}
			}
		}
		
		
		// Check if it's a chunk or a complete message
		MsrpChunkAggregatorImpl msgChunkAgg = null;
		MsrpChunkData msgChunk = null;
		MsrpMessageData msgData = null;
		String msgId;
		
		boolean wasChuncked=false;
		
		if ( msg.isChunkPartMsg() ) {
			msgChunk = msg.getChunkData();
			msgId = msgChunk.getMessageId();
			
			// If aggregation of chunck part is asked
			if (setOfOptions.contains(EnumSessionOptions.SESS_NOTIFY_COMPLETED_CHUNCKED_MSG)) {
				
				if (null == listInMsgId) {
					listInMsgId = new LinkedList<String>();
					
					mapInChunckedMsg = new HashMap<String, MsrpChunkAggregator>(5);
				}
	
				msgChunkAgg = (MsrpChunkAggregatorImpl) mapInChunckedMsg.get(msgId);
					
				if (msgChunkAgg == null) {
					msgChunkAgg = new MsrpChunkAggregatorImpl(msgId);
					mapInChunckedMsg.put(msgId, msgChunkAgg);
					listInMsgId.add(msgId);
				}
			
				try {
					// refCount of the chunck will be inc on success
					msgChunkAgg.addChunkedMsg(msgChunk);
				} catch (SessionHdException e) {
					logger.error("Exception on listener call ", e);
					
					// TODO - set in error state rather ??
					msgChunkAgg = null;
				}

				// If aggregator available
				if ( msgChunkAgg != null ) {
	
					// If Chunk message is complete, call listener if necessary
					if ( msgChunkAgg.isComplete() ) {
						msgData = msgChunkAgg;
						wasChuncked = true;
					}
				}
			}
			
			// If Chunk message listener is set call it
			if ( setOfOptions.contains(EnumSessionOptions.SESS_NOTIFY_EACH_CHUNCK) ) {
				if ( getSessListener() != null) {
					try {
						if (msgChunk.getContinuationFlag() == '#') {
							getSessListener().evtRcvAbortMsrpChunck(msgChunk);
						}
						else {
							getSessListener().evtRcvMsrpChunk(msgChunk);
						}
					}
					catch (Exception e) {
						logger.error("Exception on listener call ", e);
					}
				}
			}
			
		}
		else {
			// Just call the listener
			msgData = msg.getMessageData();
		}
		
		if (msgData != null) {
			
			if ( getSessListener() != null) {
				try {
					getSessListener().evtRcvMessage(msgData, wasChuncked);
					if (listInMsgId != null) {
						listInMsgId.remove(msgData.getMessageId());
					}
				}
				catch (Exception e) {
					logger.error("Exception on listener call ", e);
				}
			}
		}
		
	}

	
	@Override
	protected void evtReceiveReport(String messageId, MsrpReportData msg) {

		logger.info("Received report for {}", getLocalSessPath());
		
		// TODO  Report received... 
		// TODO - Here ?
		// TODO - If receive a Failure report for a msg that is currently being delivered, it should be aborted with transaction flag '#'

		// TODO - if Success-Report or Failure retrieve the Msg from the list 
		
	}

	@Override
	protected void evtReceiveResponse(MsrpResponseData msgResp) {
		logger.info("Response receive for {}, msgId {}", getLocalSessPath(), msgResp.getMessageId());
		
		// Response received... 
		
		// If response correspond to a chuncked Msg, increment number of chunck ACK in the Agg if exist
		MsrpChunkData sentChunck = msgResp.getAssociatedChunkMsgData();
		if ( sentChunck != null ) {
			
			// Call listener if option set
			if ( getSessListener() != null &&
					( setOfOptions.contains(EnumSessionOptions.SESS_NOTIFY_CHUNCK_RESPONSE)
							|| setOfOptions.contains(EnumSessionOptions.SESS_NOTIFY_FULL_RESPONSE) ) ) {
				try {
					getSessListener().evtRcvChunckResponse(msgResp);
				}
				catch (Exception e) {
					logger.error("Exception on listener call ", e);
				}
			}
			
			MsrpChunkAggregatorImpl msgAgg = null;
			if (null != mapOutChunckedMsg) {
				msgAgg = (MsrpChunkAggregatorImpl) mapOutChunckedMsg.get(sentChunck.getMessageId());
			}
			
			// msgAgg may be null in case we don't want to follow Chunck sending progress 
			if (msgAgg != null) {
				msgAgg.incNbChunkAck();

				// If the message was not initially chuncked, we create a response on last chunck Ack 
				if (msgAgg.isComplete() && msgAgg.getNbChunk() == msgAgg.getNbChunkAck()) {
					
					if (msgAgg.isAutomaticRechunckMsg() && 
							(setOfOptions.contains(EnumSessionOptions.SESS_NOTIFY_MSG_RESPONSE)
									|| setOfOptions.contains(EnumSessionOptions.SESS_NOTIFY_FULL_RESPONSE)) ) {
						if ( getSessListener() != null) {
							
							try {
								// Replace last chunck associated by the complete aggregated message 
								((MsrpMessageWrapper) msgResp).setAssociatedMessageData(msgAgg);

								getSessListener().evtRcvResponse(msgResp);
							}
							catch (Exception e) {
								logger.error("Exception on listener call ", e);
							}
						}
					}
					
					// We automatically release initial Msg Content after notify 200 OK
					msgAgg.release();
					logger.debug("After call release on msgAgg Id {}, Cpt: {}", msgAgg.getMessageId(), msgAgg.refCnt());
				}
			}
		}
		
		// If response to a completed message, retrieve the message from the list  
		MsrpMessageData sentMsg = msgResp.getAssociatedMessageData();

		if ( sentMsg != null ) {
			if ( getSessListener() != null && 
					(setOfOptions.contains(EnumSessionOptions.SESS_NOTIFY_MSG_RESPONSE)
							|| setOfOptions.contains(EnumSessionOptions.SESS_NOTIFY_FULL_RESPONSE)) ) {
				try {
					getSessListener().evtRcvResponse(msgResp);
				}
				catch (Exception e) {
					logger.error("Exception on listener call ", e);
				}
			}
			
			// We don't automatically release initial Msg Content after notify 200 OK, it's done by the Transaction Handler
			// sentMsg.release();
		}
		
	}

	
	@Override
	protected void evtCnxSessionAccepted() {

		logger.info("Connexion succeed for {}", getLocalSessPath());

		if (! isConnected()) {
			// Set timer on connection
			msrpCnx.setInactivityTO(inactivityTO, inactivityTO, inactivityTO);
			logger.info("Timer inactivity set to {}", inactivityTO);

			setConnected(true);

			if (getSessListener() != null) {
				try {
					getSessListener().evtSessConnect();
				}
				catch (Exception e) {
					logger.error("Exception on listener call ", e);
				}
			}
		}
		else {
			logger.warn("Session already marked as connected {}", getLocalPath());
		}
		
	}

	@Override
	protected void evtCnxSessionFailure(Throwable cause) {

		logger.warn("Connexion Failed for {}, error {}", getLocalSessPath(), cause);

		if (getSessListener() != null) {
			try {
				getSessListener().evtSessDisconnect(DisconnectReason.CNX_FAILURE);
			}
			catch (Exception e) {
				logger.error("Exception on listener call ", e);
			}
		}
	}


	@Override
	protected void evtSessDisconnect(DisconnectReason reason, List<MsrpMessageData> listMsgFailed) {

		logger.info("Disconnection for {}, with cause {}", getLocalSessPath(), reason);

		if (getSessListener() != null) {
			try {
				if (listMsgFailed == null) {
					getSessListener().evtSessDisconnect(reason);
				}
				else {
					
					//    or wrapper implement interface MsrpMessageData
					getSessListener().evtSessDisconnect(reason, listMsgFailed );
				}
			}
			catch (Exception e) {
				logger.error("Exception on listener call ", e);
			}
			//setUnref();
		}
	}

	@Override
	protected void evtSessDisconnect(DisconnectReason reason) {
		logger.info("Disconnection for {}, with cause {}", getLocalSessPath(), reason);

		if (getSessListener() != null) {
			try {
				getSessListener().evtSessDisconnect(reason);
			} catch (Exception e) {
				logger.error("Exception on listener call ", e);
			}
		}
		//setUnref();
	}


	@Override
	public void evtMsgSendFailure(MsrpMessageWrapper msgSend) {

		if (! msgSend.isChunkPartMsg()) {
			if (getSessListener() != null) {
				try {
					getSessListener().evtSendMsgFailure(msgSend);
				} catch (Exception e) {
					logger.error("Exception on listener call ", e);
				}
			}
		}
		else {
			if (getSessListener() != null) {
				try {
					getSessListener().evtSendChunkedMsgFailure(msgSend);
				} catch (Exception e) {
					logger.error("Exception on listener call ", e);
				}
			}
		}
	}

	
	
	@Override
	public void sendExplicitResponse(MsrpMessageData receivedMessage)
			throws SessionHdException {
		// TODO Auto-generated method stub
		// TODO - May be useful with an option manual Response 
		
	}

	@Override
	public void setUserContext(Object ctx) {
		userCtx = ctx;
	}

	@Override
	public Object getUserContext() {
		// TODO Auto-generated method stub
		return userCtx;
	}


}
