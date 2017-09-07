package org.msrpenabler.server.net;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.msrpenabler.server.api.DisconnectReason;
import org.msrpenabler.server.api.MsrpMessageData;
import org.msrpenabler.server.api.MsrpReportData;
import org.msrpenabler.server.api.internal.MsrpMsgTypeEnum;
import org.msrpenabler.server.exception.TransactionException;
import org.msrpenabler.server.util.GenerateIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.timeout.IdleStateHandler;



public class TransactionHandler {

	private static final Logger logger =
        LoggerFactory.getLogger(TransactionHandler.class);
	

	private Channel channel;
	// Map Transaction Id - msg
	// Transaction map
	private volatile boolean cnxMarkAsClosing = false;
	private volatile boolean isNewSessBindWaited = false; 
	
//	private SessionTransactionData masterSession;
	
	protected volatile boolean isConnected=false;

	private Map<String, SessionTransactionData> mapWaitedBindSess = new HashMap<String, SessionTransactionData>();
	private Map<String, SessionTransactionData> mapActiveSess = new HashMap<String, SessionTransactionData>();
	
	final private Map<String, TransactionFuture> mapOutboundTransaction
												= new HashMap<String, TransactionFuture>();
	//private Map<String, MsrpMessageWrapper> mapInboundTransaction = new ConcurrentHashMap<String, MsrpMessageWrapper>();

	final private Object lockSessMaps = new Object();


//	public TransactionHandler(SessionTransactionData masterSession) {
//		this.masterSession = masterSession;
//	}

	// Getter - Setter
	public Channel getChannel() {
		return channel;
	}	

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	
	
	/*
	 * Cnx Listener
	 */

	public void evtCnxCltFailure(SessionTransactionData sessData, Throwable throwable) {
		
		sessData.evtCnxSessionFailure(throwable);
	}

	public void evtCnxCltSuccess(SessionTransactionData sessData) {
		
		// Don't wait msrpEmpty answer before allowing send message
		// set as connected on socket connection
		logger.info("set msrpCnx as isConnected {}", isConnected);

		isConnected = true;
		synchronized (lockSessMaps) {
			mapActiveSess.put(sessData.getLocalSessId(), sessData);
		}
		
		// Call sessConnect listener
		sessData.evtCnxSessionAccepted();

		// Send an empty msg and set a new listener to notify user session on Msrp response 
		TransactionListener transListener = new TransactionListener() {
			@Override
			public void operationComplete(TransactionFuture future) {

				logger.info("Call op complete on send empty msg {}", this);

				if (future.isSuccess()) {
//					synchronized (lockSessMaps) {
//						mapActiveSess.put(future.sessData.getLocalSessPath(), future.sessData);
//					}
					
//					future.sessData.evtCnxSessionAccepted();
				}
				else {
					future.sessData.evtCnxSessionFailure(future.cause());
				}

				// Clean transaction
				retrieveTransaction(future.transID);
			}
		};

		sendCnxEmptyMsg(sessData, transListener);
	}

	
	public boolean checkLocalPathRegister(String sessionId) {
		boolean isRegister = false;
		synchronized (lockSessMaps) {
			if (mapActiveSess.containsKey(sessionId) || mapWaitedBindSess.containsKey(sessionId)) {
				isRegister = true;
			}
		}
		return isRegister;
	}

	public void setBindPath(SessionTransactionData sessData) throws TransactionException {

		if ( checkLocalPathRegister(sessData.getLocalSessId()) ) {
    		throw new TransactionException("Session is already connected or bind for local path: "+sessData.getLocalSessPath());
    	}
		
		synchronized (lockSessMaps) {
			if (cnxMarkAsClosing) throw new TransactionException("This connexion is closed"); 
				
			mapWaitedBindSess.put(sessData.getLocalSessId(), sessData);
			isNewSessBindWaited = true;
		}
	}	
	
	private void checkSessConnect(String localSessId, String remoteSessId) {
		SessionTransactionData  sessData=null;
		
		if (isNewSessBindWaited) {
			synchronized (lockSessMaps ) {
				sessData = mapWaitedBindSess.remove(localSessId);
				if (mapWaitedBindSess.isEmpty()) {
					isNewSessBindWaited = false;
				}
				if (null != sessData) {
					if (sessData.getRemoteSessId().equals(remoteSessId)) {
						mapActiveSess.put(localSessId, sessData);
					}
					else {
						// that was a mistake, roll back
						mapWaitedBindSess.put(localSessId, sessData);
						isNewSessBindWaited = true;
						sessData = null;
					}
				}
			}
			
			if (null != sessData) {
				isConnected = true;
				sessData.evtCnxSessionAccepted();
				sessData.getCnxFuture().setSuccess();
			}
		}
		
	}
	
	private SessionTransactionData getSessActive(String localSessId, String remoteSessId) {
		SessionTransactionData sessData;
		
		synchronized (lockSessMaps) {
			sessData = mapActiveSess.get(localSessId);
		}
		if (sessData == null || ! sessData.getRemoteSessId().equals(remoteSessId)) {
			sessData = null;
		}
			
		return sessData;
	}
	

	public void closeSess(final String localSessId, String remoteSessId) throws TransactionException {
		
		SessionTransactionData sessData;
		
		synchronized (lockSessMaps) {
			sessData = mapActiveSess.get(localSessId);
			if (sessData == null && isNewSessBindWaited) {
				sessData = mapWaitedBindSess.remove(localSessId);
			}
			// if last session, we really close the cnx 
			if (mapActiveSess.size() + mapWaitedBindSess.size() == 1) {
				cnxMarkAsClosing  = true;
			}
		}
		if (sessData == null) {
			logger.info("Close session already asked {}, channel isActive {}", localSessId, channel.isActive());
			return;
		}
		
		// Mark sess as closeAsked
		sessData.closeAsked = true;
		sessData.setConnected(false);

		if (cnxMarkAsClosing) {

			ChannelFuture future = channel.close();
			future.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {

					// notify all sessions
					notifyCloseAllSessions(DisconnectReason.LOCAL_CLOSE);
				}
			});
		}

	}

	
	/**
	 * Notify sending failure on all sent message on failure
	 * 
	 * @param sessData : session context
	 */
	protected List<MsrpMessageData> retrieveMsgTrans(SessionTransactionData sessData) {
		
		// iterate on each transId associated to this session
		LinkedList<MsrpMessageData> listMsg = null;
		
		String transId = sessData.poolTransaction();
		while (  transId != null ) {
			TransactionFuture future = retrieveTransaction(transId);
			
			if (future != null) {
				MsrpMessageWrapper msg = future.msgSend;

				// Chunk should be managed at session level  
				if (msg != null && !msg.isChunkPartMsg()) {
					if (listMsg == null) {
						listMsg = new LinkedList<MsrpMessageData>();
					}
					listMsg.add( msg );
				}
			}
			transId = sessData.poolTransaction();
		}
		return listMsg;
	}

	
	
	private void sendCnxEmptyMsg(SessionTransactionData sessData, TransactionListener transListener) {

		// Fill msg and transaction listener
		MsrpMessageWrapper msgEmpty = new MsrpMessageWrapper();
	
		msgEmpty.msgType = MsrpMsgTypeEnum.MSRP_SEND;
		msgEmpty.toPath = sessData.getRemoteSessPath();
		msgEmpty.fromPath = sessData.getLocalSessPath();
		msgEmpty.setContentType("empty/empty");
		msgEmpty.byteRangeStart = "1";
		msgEmpty.byteRangeLength = "0";
		msgEmpty.byteRangeEnd = "0";
		msgEmpty.hasByteRange = true;
		
		msgEmpty.setMessageId(GenerateIds.createMessageId());
		msgEmpty.continuationFlag='$';

		final TransactionFuture transFuture = new TransactionFuture(channel, msgEmpty, sessData, true, false);

		transFuture.addListener(transListener);

		try {
			
			getAndSetNewTransationID(transFuture);
			
		} catch (TransactionException e) {
			logger.error("Failed on sending empty cnx msrp msg", e);

			transFuture.setMsgCause("Failed on sending empty cnx msrp msg");
			transFuture.setFailure(e);
			return ;
		}
		
		// Send message on channel
		ChannelFuture chFuture = channel.write(msgEmpty);
		
		chFuture.addListener( new ChannelFutureListener() {
			public void operationComplete(ChannelFuture future) throws Exception {
				// Notify in case of failure, otherwise wait remote response
            	if (! future.isSuccess()) {
            		transFuture.setMsgCause("Failure on sending message");
            		transFuture.setFailure(future.cause());
            	}
			}
		});
		
		channel.flush();
	}

	
	// Manage Msg, report or resp
	public void evtHandlerReceiveMsg(MsrpMessageWrapper msg) {
	
		SessionTransactionData sessData;
		
		switch (msg.msgType) 
		{

		case MSRP_SEND:
			
			// Check if session connect has to be triggered into active one
			checkSessConnect(msg.toPathSessionId, msg.fromPathSessionId);
			
			// Now we can check if sess is active
			sessData = getSessActive(msg.toPathSessionId, msg.fromPathSessionId);
			
			if (sessData == null) {
				sendResponse(msg, "481", "Invalid MSRP Session");
				return;
			}
	
			// Check if the request has to be respond
			// See RFC 4975 - 7.2
			//      Respond send 200 OK if Failure-Report isn't present or set to yes
			//		Respond error code if not able to manage request and Report-Failure set to partial
			//      Never send response if Failure-Report set to no, or set to partial and not immediate error
			
			String headFailureReport = msg.headersMap.get("Failure-Report");
			
			if (headFailureReport == null || headFailureReport.equals("yes")) {
				// Generate message response before notify user
				// thus if on notify, user ask for close, the msg is correctly acknowledge 
				sendResponse(msg, "200", "OK");
			}
			
			sessData.evtReceiveMsg(msg);

			break;
			
		case MSRP_REPORT:

			sessData = getSessActive(msg.toPathSessionId, msg.fromPathSessionId);

            MsrpReportData reportData = null;
            try {
                reportData = msg.getReportData();
            } catch (TransactionException e) {
                fireException(e);
            }

            sessData.evtReceiveReport(msg.getMessageId(), reportData);

			// Check if a close has been asked
			checkNotifyDisconnect(sessData);
			
			break;

		case MSRP_RESPONSE:

			//sessData = getSessActive(msg.toPathSessionId, msg.fromPathSessionId);

			// Use the transaction listener if set
			TransactionFuture future = mapOutboundTransaction.get(msg.transactionId);
			if (null == future) {
				logger.warn("Receive an unwaiting response, tid: {}", msg.transactionId);
				// Ignore msg  
				return;
			}

			logger.info("Receive a response, tid: {}", msg.transactionId);

			// associate response with initial msg
			msg.setAssociatedMessageData( future.msgSend );
			if (future.msgSend != null) {
				msg.messageId = future.msgSend.getMessageId();
			}

            future.msgResponse = msg.getResponseData();
			
			// If Transaction listener is set we call it
			future.setSuccess();
			logger.info("After set success future response: {}", msg.transactionId);

			// Check if a close has been asked
			checkNotifyDisconnect(future.sessData);
			
			break;

		case MSRP_UNDEF:

			logger.error(" Receive an unknow Message !!! ");
			// TODO Close connection ??
			
			// Call evtExceptionCautgh
			fireException(new TransactionException("Received an unknow message type" + msg.getFirstLine()));

            break;
            
		default:
			logger.error(" Receive an Message type: {} ", msg.msgType );
			// TODO Close connection ??
			
			break;
        }
		

//		// Save the transaction if Success Request is asked
//		// Should be managed at session level
//
//		String headSuccessReport = msg.headersMap.get("Success-Report");
//
//		if (headSuccessReport != null && msg.msgType == MsrpMsgTypeEnum.MSRP_SEND && headSuccessReport.equals("yes")) {
//
//			logger.warn("Success-Report not managed yet at transaction level");
//			
//			if (mapInboundTransaction.containsKey(msg.transactionId)) {
//				logger.error("Failure received a transaction Id ({}) that already exist ", msg.transactionId);
//				fireException(new TransactionException("Failure received a transaction Id ("+msg.transactionId+") that already exist "));
//				return;
//			}
//			mapInboundTransaction.put(msg.transactionId, msg);
//		}
		
	}

	

	public void sendReportSuccess(MsrpMessageWrapper msg, int startByteRange, int lengthByteRange, int endByteRange) {

		// Fill msg and transaction listener
		MsrpMessageWrapper msgWrp = new MsrpMessageWrapper();

		msgWrp.msgType = MsrpMsgTypeEnum.MSRP_REPORT;
		msgWrp.toPath = msg.fromPath;
		msgWrp.fromPath = msg.toPath;
		msgWrp.continuationFlag='$';
		msgWrp.messageId = msg.messageId;

		msgWrp.byteRangeStart = Integer.toString(startByteRange);
		msgWrp.byteRangeLength = Integer.toString(lengthByteRange);
		msgWrp.byteRangeEnd = Integer.toString(endByteRange);

		msgWrp.headersMap.put("Status", "000 200 OK");
		
		// Create the transaction associate to the msg and sessData
		final TransactionFuture transFuture = new TransactionFuture(channel, msgWrp, null, false, false);

		try {
			getAndSetNewTransationID(transFuture);
		} catch (TransactionException e) {
			logger.error("Failed on sending report msrp msg", e);
			return ;
		}
		
		// Send message on channel
		ChannelFuture chFuture = channel.write(msgWrp);
		
		chFuture.addListener( new ChannelFutureListener() {
			public void operationComplete(ChannelFuture future) throws Exception {
				// Notify in case of failure, otherwise wait remote response
            	if (! future.isSuccess()) {
        			logger.error("Failed on sending report msrp msg");
        			transFuture.setFailure(future.cause());
            	} 
            	else {
            		transFuture.setSuccess();
            	}
            	
            	// Don't keep transation ID in map as no response is waited
            	retrieveTransaction(transFuture.transID);
            	
            	// release msg on end of write as no listener call for report at that time
            	if (transFuture.msgSend != null) transFuture.msgSend.release();
			}
		});
		
		channel.flush();
		
	}
		

	// Check if close asked on a multi session Cnx that has to be still active
	private void checkNotifyDisconnect(SessionTransactionData sessData) {

		if (sessData.closeAsked) {
			if (! sessData.isActiveTransaction()) {
				sessData.evtSessDisconnect(DisconnectReason.LOCAL_CLOSE);
				mapActiveSess.remove(sessData.getLocalSessId());
			}
		}
	}

	private void sendResponse(MsrpMessageWrapper msg, String statusCode, String comment) {

		MsrpMessageWrapper resp = msg.generateResponseMsg(statusCode, comment);
		
		channel.writeAndFlush(resp);
	}
	
	//@SuppressWarnings("static-method")
	private void fireException(Exception exception) {
		logger.error("Exception : ", exception);

		// TODO - call a listener here ??
	}
	
	
	public TransactionFuture retrieveTransaction(String transId) {
		TransactionFuture future = mapOutboundTransaction.remove(transId);
		if (null != future) {
			future.removeTansactionId(transId);
		}
		return future;
	}
	
	protected void getAndSetNewTransationID(TransactionFuture transFuture) throws TransactionException {

		// Use only aplphanum
		String tokenToUse = GenerateIds.createTokenList('A', 'Z');
		
		tokenToUse+= GenerateIds.createTokenList('a','z');
		tokenToUse+= GenerateIds.createTokenList('0','9');
		
		String transId;
		boolean toRegenerate;
		int bcl=0;

		do {
			transId = GenerateIds.generateId(tokenToUse, 6, 10);
		
			// Thread safe op
			synchronized(mapOutboundTransaction) {
				toRegenerate = mapOutboundTransaction.containsKey(transId);
				if (!toRegenerate) mapOutboundTransaction.put(transId, transFuture);
			}
			bcl++;
		} while (toRegenerate && bcl < 100);
			
		if (toRegenerate) {
			throw new TransactionException("Failed to get a valid transaction Id ! Number of tries :  "+ bcl );
		}
		
		// set transId in the Handler and its fields msg && sessData 
		transFuture.setTansactionId(transId);
		
	}

	public void setInactivityTO(int readTO, int writeTO, int rwTO) {
		
		// Set TO handler if not yet set only on valid channel
		if (channel != null && channel.pipeline().get("idleStateHandler") == null ){

			// Idle Time Out
			logger.info("Add idleStateHandler in the pipeline with rwTO {} ", rwTO);
			channel.pipeline().addFirst("idleStateHandler", new IdleStateHandler(readTO, writeTO, rwTO));
			
			logger.info("Channel added: {}", channel.pipeline().first().toString());
		}

	}
	
	public void evtInactivityChannel() {

		cnxMarkAsClosing = true;
		
		notifyCloseAllSessions(DisconnectReason.INACTIVITY_RW);
	}

	
	public void evtCloseChannel() {
		
		// Disconnect will be fired by closeSess listener or was fired on inactivity
		if (cnxMarkAsClosing) {
			return;
		}
		
		cnxMarkAsClosing = true;
		
		notifyCloseAllSessions(DisconnectReason.REMOTE_CLOSE);
		
	}

	private void notifyCloseAllSessions(DisconnectReason reason) {
		Iterator<String> iter = mapWaitedBindSess.keySet().iterator();
		
		while (iter.hasNext()) {
			
			SessionTransactionData sessData = mapWaitedBindSess.remove(iter.next());
			
			sessData.evtSessDisconnect( reason);
		}

		iter = mapActiveSess.keySet().iterator();
		
		while (iter.hasNext()) {
			
			SessionTransactionData sessData = mapActiveSess.remove(iter.next());
			
			List<MsrpMessageData> listMsgFailed = retrieveMsgTrans(sessData);
			
			sessData.evtSessDisconnect(reason, listMsgFailed);
			
		}
	}

	
	public void sendMsrpMessage(SessionTransactionData sessData, MsrpMessageWrapper msgWrp) throws TransactionException {

		
		if (!isConnected) {
			logger.info("Transaction Handler obj {}", this);

			throw new TransactionException("Connexion is not yet connected: "+ sessData.getLocalSessPath());
		}
		
		// Fill msg and transaction listener

		msgWrp.msgType = MsrpMsgTypeEnum.MSRP_SEND;
		msgWrp.toPath = sessData.getRemoteSessPath();
		msgWrp.fromPath = sessData.getLocalSessPath();

		// TODO - Check if transaction has to be stored
		String strFailReport = msgWrp.headersMap.get("Failure-Report");
		String strSuccessReport ;
		boolean failureReport = false;
		boolean successReport = false;
		
		// store Transaction if 200 OK resp waited 
		// in case of partial, the message failure should be managed at session level
		if ( strFailReport == null || strFailReport.equals("yes")) {
			failureReport = true;
		}
		else {
			// store Transaction if Success Report waited
			strSuccessReport = msgWrp.headersMap.get("Success-Report");
			
			if (strSuccessReport != null && strSuccessReport.equals("yes")) {
				successReport = true;
			}
		}
			
			
		// Create the transaction associate to the msg and sessData
		final TransactionFuture transFuture = new TransactionFuture(channel, msgWrp, sessData, failureReport, successReport);

		// Listener definition		
		TransactionListener transListener = new TransactionListener() {
			@Override
			public void operationComplete(TransactionFuture future) {

				if (future.isSuccess()) {

					// No response on sending if no Failure-Report
					if ( future.msgResponse != null && future.failureReport) {
						future.sessData.evtReceiveResponse(future.msgResponse);
					}
					
					// TODO - Success report should be managed at session level
					if (future.successReport) {
						// TODO - retrieve transaction or wait a timer or let id alive until end of session ?
						logger.warn("SuccessReport not managed at transaction level - TODO at session level OK ?");
					}
				}
				else {
					if (future.failureReport) {
						future.sessData.evtMsgSendFailure(future.msgSend);
					}
				}

				// Clean transaction
				retrieveTransaction(future.transID);
				
				// Release initial msg content after listener calls 
				if (future.msgSend != null) future.msgSend.release();
				
			}
		};
		
		// Set the listener definition		
		transFuture.addListener(transListener);

		try {
			
			getAndSetNewTransationID(transFuture);
			
		} catch (TransactionException e) {
			logger.error("Failed on sending empty cnx msrp msg", e);

			transFuture.setMsgCause("Failed on sending empty cnx msrp msg");
			transFuture.setFailure(e);
			return ;
		}
		
		// Send message on channel
		ChannelFuture chFuture = channel.write(msgWrp);
		final String msgId = msgWrp.getMessageId();
		
		chFuture.addListener( new ChannelFutureListener() {
			public void operationComplete(ChannelFuture future) throws Exception {
				// Notify in case of failure, otherwise wait remote response only if 200 OK is waited 
            	if (! future.isSuccess()) {
            		transFuture.setMsgCause("Failure on sending message");
            		transFuture.setFailure(future.cause());
        			logger.warn("Failed on sending msg Id {} ", msgId);
            	}
            	else if ( future.isSuccess() && ! transFuture.failureReport) {
            		// Clean transaction immediately
            		transFuture.setSuccess();
            	}
			}
		});
		
		channel.flush();
		
	}

}
