package org.msrpenabler.server.api;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MultiListeners extends MsrpSessionListener {

    private static final Logger logger =
            LoggerFactory.getLogger(MultiListeners.class);


	ConcurrentLinkedQueue<MsrpSessionListener> listenersList = new ConcurrentLinkedQueue<MsrpSessionListener>();
	
	
	public void addListener(MsrpSessionListener listener) {
		if (null != listener) {
			listenersList.add(listener);
			listener.attachSessionHd(getSessionHd());
		}
	}

	public void attachSessionHd(MsrpSessionHd sessHd) {
		
		super.attachSessionHd(sessHd);

        for (MsrpSessionListener aListener : listenersList) {
        	aListener.attachSessionHd(sessHd);
        }
	}
	
	public boolean removeListener(MsrpSessionListener listener) {

		return listenersList.remove(listener);
	}

	@Override
	public void evtSessConnect() {

        for (MsrpSessionListener aListener : listenersList) {
        	aListener.evtSessConnect();
        }
	}

	@Override
	public void evtSessDisconnect(DisconnectReason disconnectReason) {

        for (MsrpSessionListener aListener : listenersList) {
        	aListener.evtSessDisconnect(disconnectReason);
        }
	}

	@Override
	public void evtSessDisconnect(DisconnectReason disconnectReason, List<MsrpMessageData> listMsgFailed) {

        for (MsrpSessionListener aListener : listenersList) {
        	aListener.evtSessDisconnect(disconnectReason, listMsgFailed);
        }
		
	}

	@Override
	public void evtRcvMessage(MsrpMessageData msrpContent, boolean wasChunked) {

        for (MsrpSessionListener aListener : listenersList) {
			msrpContent.retain();
        	aListener.evtRcvMessage(msrpContent, wasChunked);
        }
		msrpContent.release();
	}

	@Override
	public void evtRcvResponse(MsrpResponseData respMsg) {

        for (MsrpSessionListener aListener : listenersList) {
        	aListener.evtRcvResponse(respMsg);
        }
	}

	@Override
	public void evtSendReportRcv(MsrpReportData msrpContent) {

        for (MsrpSessionListener aListener : listenersList) {
        	aListener.evtSendReportRcv(msrpContent);
        }
	}

	@Override
	public void evtSendMsgSuccess(MsrpMessageData msrpContent) {
		
        for (MsrpSessionListener aListener : listenersList) {
			msrpContent.retain();
			aListener.evtSendMsgSuccess(msrpContent);
        }
		msrpContent.release();
	}

	@Override
	public void evtSendMsgFailure(MsrpMessageData msgSend) {

        for (MsrpSessionListener aListener : listenersList) {
			msgSend.retain();
			aListener.evtSendMsgFailure(msgSend);
		}
		msgSend.release();
	}

	@Override
	public void evtRcvMsrpChunk(MsrpChunkData msrpChunk) {

		for (MsrpSessionListener aListener : listenersList) {
			logger.debug("new retain before call listener");
			msrpChunk.retain();
			aListener.evtRcvMsrpChunk(msrpChunk);
		}
		// Release are done by listener, we have just to release for the last one 
		logger.debug("Last release on multi listener");
		msrpChunk.release();
	}

	@Override
	public void evtRcvChunckResponse(MsrpResponseData respMsg) {

		for (MsrpSessionListener aListener : listenersList) {
			aListener.evtRcvChunckResponse(respMsg);
		}
	}

	@Override
	public void evtRcvAbortMsrpChunck(MsrpChunkData msrpChunk) {

		for (MsrpSessionListener aListener : listenersList) {
			msrpChunk.retain();
			aListener.evtRcvAbortMsrpChunck(msrpChunk);
		}
		// Release are done by listener, we have just to release for the last one 
		msrpChunk.release();
	}

	@Override
	public void evtSendChunkedMsgFailure(MsrpChunkData msrpChunk) {

		for (MsrpSessionListener aListener : listenersList) {
			msrpChunk.retain();
			aListener.evtSendChunkedMsgFailure(msrpChunk);
		}
		// Release are done by listener, we have just to release for the last one 
		msrpChunk.release();
	}

}
