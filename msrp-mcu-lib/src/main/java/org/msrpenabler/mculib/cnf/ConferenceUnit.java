package org.msrpenabler.mculib.cnf;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.msrpenabler.server.api.MsrpChunkData;
import org.msrpenabler.server.api.MsrpMessageData;
import org.msrpenabler.server.api.MsrpSessionHd;
import org.msrpenabler.server.exception.SessionHdException;
import org.msrpenabler.server.exception.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConferenceUnit {

    private static final Logger logger =
            LoggerFactory.getLogger(ConferenceUnit.class);
	
	private String confId;
	
	private CnfGlobalListener globalListener = null;
	
	
	public void setGlobalListener(CnfGlobalListener globalListener) {
		this.globalListener = globalListener;
	}



	private class DescMsrpSession {
		public DescMsrpSession(MsrpSessionHd sessMsrp,
				CnfMsrpSessionListener cnfMsrpSessionListener) {
			sessHd = sessMsrp;
			sessListener = cnfMsrpSessionListener;
		}
		public MsrpSessionHd sessHd;
		public CnfMsrpSessionListener sessListener;
	}
	
	private Map<String, DescMsrpSession> mapSession = new ConcurrentHashMap<String, DescMsrpSession>();
	
	
	public void setConfId(String confId) {
		this.confId = confId;
	}


	public String getConfId() {
		return confId;
	}


	public void attachSession(MsrpSessionHd sessMsrp) throws SessionHdException {
		
		DescMsrpSession elt = new DescMsrpSession(sessMsrp, new CnfMsrpSessionListener(this));
		
		mapSession.put(sessMsrp.getSessionId(), elt);
		sessMsrp.addSessListener(elt.sessListener);
		return;
	}

	public void detachSession(MsrpSessionHd sessMsrp) throws SessionHdException {
		
		DescMsrpSession elt = mapSession.remove(sessMsrp.getSessionId());
		if (elt != null) {
			sessMsrp.removeSessListener(elt.sessListener);
		}
		return;
	}

	

	/**
	 * Manage data received from a Msrp Session 
	 * 
	 * @param sessMsrp
	 * @param msrpMessage
	 * @param wasChunked
	 */
	public void evtRcvMessage(MsrpSessionHd sessMsrp,
			MsrpMessageData msrpMessage, boolean wasChunked) {

		
		// Chunks have already been broadcast  by the method evtRcvChunk  
		if (wasChunked) return;

		try {
			// send the message to other Leg, retain message if more than one leg
			Iterator<Entry<String, DescMsrpSession>> it = mapSession.entrySet().iterator();
			MsrpSessionHd sessElt;
			
			logger.debug("evtRcvMessage for msg received on {},  sessId {} ", sessMsrp, sessMsrp.getSessionId());

			if (null != globalListener) {
				// Here the content data is retained in memory for this particular call
				// user has to manage the message release if necessary... 
				msrpMessage.retain();
				try {
					logger.debug("call global evtRcvMessage for msg received on {} from {}", sessMsrp.getLocalPath(), sessMsrp.getRemotePath());
				} catch (SessionHdException e) {
					// TODO Auto-generated catch block
					logger.warn("Failed on log local & remote path on sessId {}", sessMsrp.getSessionId());;
				}
				globalListener.evtRcvMessage(this, sessMsrp, msrpMessage, wasChunked);
			}

			while (it.hasNext()) {
				sessElt = it.next().getValue().sessHd;
				logger.debug("Iteration on element {},  sessId {} ", sessElt, sessElt.getSessionId());
				if (! sessElt.equals(sessMsrp)) {
	
					try {
						msrpMessage.retain();
						logger.debug("call send msg to {} from {}", sessElt.getRemotePath(), sessElt.getLocalPath());
						sessElt.sendMsrpMessage( ((MsrpMessageData) msrpMessage.duplicate()) );

					} catch (SessionHdException  e) {
						logger.error("Failed to send message on session {}", sessElt.getSessionId(), e);
					} catch (TransactionException e) {
						logger.error("Failed to send message on session {}", sessElt.getSessionId(), e);
					}
				}
			}

		}
		finally {
			// Always release msg here as it has been always retained before call each listeners
			msrpMessage.release();
		}
	}

	
	public void evtRcvChunk(MsrpSessionHd sessMsrp,
			MsrpChunkData chunck) {

		// send the message to other Leg, retain message if more than one leg
		Iterator<Entry<String, DescMsrpSession>> it = mapSession.entrySet().iterator();
		MsrpSessionHd sessElt;
		
		try {

			if (null != globalListener) {
				// Here the content data is retained in memory for this particular call
				// user has to manage the message release if necessary... 
				chunck.retain();
				globalListener.evtRcvMsrpChunk(this, sessMsrp, chunck);
			}

			while (it.hasNext()) {
				sessElt = it.next().getValue().sessHd;
				if (! sessElt.equals(sessMsrp)) {

					try {
						// retain data in memory for next session on to we have to send the msg
						chunck.retain();
						sessElt.sendMsrpChunk((MsrpChunkData) chunck.duplicate(), false);


					} catch (SessionHdException  e) {
						logger.error("Failed to send message on session {}", sessElt.getSessionId(), e);
					} catch (TransactionException e) {
						logger.error("Failed to send message on session {}", sessElt.getSessionId(), e);
					}
				}
			}
		}
		finally {
			// Always release msg here as it has been always retained before call each listeners
			chunck.release();
		}

	}

	
	/**
	 * Inject a message into the conference
	 * Message is release in the conference an must not be call after this call,
	 * or the caller has to call retain() before and release() as soon as possible to avoid memory leak 
	 *  
	 * @param msrpContent
	 * @param fromSessMsrp :  indicates that msg has not to be send to this session. NULL if msg has to be inject to all sessions.
	 */
	public void injectMessage(MsrpMessageData msrpContent, MsrpSessionHd fromSessMsrp) {

		// send the message to other Leg, retain message if more than one leg
		Iterator<Entry<String, DescMsrpSession>> it = mapSession.entrySet().iterator();
		MsrpSessionHd sessElt;
		
		
		while (it.hasNext()) {
			sessElt = it.next().getValue().sessHd;
			
			if ( fromSessMsrp == null || ! sessElt.equals(fromSessMsrp)) {
				try {
					// retain data in memory for next session on to we have to send the msg
					msrpContent.retain();

					sessElt.sendMsrpMessage( ((MsrpMessageData) msrpContent.duplicate()) );

				} catch (SessionHdException  e) {
					logger.error("Failed to send message on session {}", sessElt.getSessionId(), e);
				} catch (TransactionException e) {
					logger.error("Failed to send message on session {}", sessElt.getSessionId(), e);
				}
			}
		}

		// Always release msg as it has been retain for each sub-call
		msrpContent.release();
		
	}


	/**
	 * 
	 */
	public void detachAllSessions() {


		Iterator<String> it = mapSession.keySet().iterator();
		
		// TODO - What happens if a message is managed during this phase ???
		
		while (it.hasNext()) {
			DescMsrpSession elt = mapSession.remove(it.next());
			elt.sessHd.removeSessListener(elt.sessListener);
		}
		
		return;
	}
	
}
