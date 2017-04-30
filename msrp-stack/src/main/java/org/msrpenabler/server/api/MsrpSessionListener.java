package org.msrpenabler.server.api;

import java.util.List;

/**
 * @author Julien Bellanger
 *
 */
public abstract class MsrpSessionListener {
	
	private MsrpSessionHd sessionHd = null;

	/*
	 * Session Mgnt
	 */
	public void attachSessionHd(MsrpSessionHd sessHd) {
		this.sessionHd = sessHd;
	}

	public MsrpSessionHd getSessionHd() {
		return this.sessionHd;
	}

	/*
	 * Connection Mgnt
	 */
	public abstract void evtSessConnect();

	public abstract void evtSessDisconnect(DisconnectReason disconnectReason);

	public abstract void evtSessDisconnect(DisconnectReason disconnectReason, List<MsrpMessageData> listMsgFailed);

	/*
	 * Return false if you want manually ACK this message 
	 */
	public abstract void evtRcvMessage(MsrpMessageData msrpContent, boolean wasChunked);

	public abstract void evtRcvResponse(MsrpResponseData respMsg);


	// Usage to confirm
	public abstract void evtSendReportRcv(MsrpReportData msrpContent);

	public abstract void evtSendMsgSuccess(MsrpMessageData msrpContent);

	public abstract void evtSendMsgFailure(MsrpMessageData msgSend);



	/*
	 * Chunk Management
	 */
	public abstract void evtRcvMsrpChunk(MsrpChunkData msrpChunk);

	public abstract void evtRcvChunckResponse(MsrpResponseData respMsg);

	public abstract void evtRcvAbortMsrpChunck(MsrpChunkData msrpChunk);

	public abstract void evtSendChunkedMsgFailure(MsrpChunkData msrpChunk);
	  
	  
}
