package org.msrpenabler.mculib.cnf;

import java.util.List;

import org.msrpenabler.server.api.DisconnectReason;
import org.msrpenabler.server.api.MsrpChunkData;
import org.msrpenabler.server.api.MsrpMessageData;
import org.msrpenabler.server.api.MsrpReportData;
import org.msrpenabler.server.api.MsrpResponseData;
import org.msrpenabler.server.api.MsrpSessionHd;
import org.msrpenabler.server.api.MsrpSessionListener;

public class CnfMsrpSessionListener extends MsrpSessionListener {

	private ConferenceUnit confHub;
	
	public CnfMsrpSessionListener(ConferenceUnit conf) {
		confHub = conf;
	}

	@Override
	public void evtSessConnect() {
		
	}

	@Override
	public void evtSessDisconnect(DisconnectReason disconnectReason) {
		// TODO Auto-generated method stub

		// Automatically detached from the Hub ?
	}

	@Override
	public void evtSessDisconnect(DisconnectReason disconnectReason,
			List<MsrpMessageData> listMsgFailed) {
		// TODO Auto-generated method stub

		// Automatically detached from the Hub ?

	}

	@Override
	public void evtRcvMessage(MsrpMessageData msrpContent, boolean wasChunked) {

		// Send the message to conference obj with the originating session Id 
		
		MsrpSessionHd sessMsrp = getSessionHd();

		confHub.evtRcvMessage(sessMsrp, msrpContent, wasChunked);
		
	}

	@Override
	public void evtRcvResponse(MsrpResponseData respMsg) {
		// TODO Auto-generated method stub

		// Send the message to other session
		
	}

	@Override
	public void evtSendReportRcv(MsrpReportData msrpContent) {
		// TODO Auto-generated method stub

		// Send the message to other session
		
	}

	@Override
	public void evtSendMsgSuccess(MsrpMessageData msrpContent) {
		// TODO Auto-generated method stub

		// Send the message to other session
		
	}

	@Override
	public void evtSendMsgFailure(MsrpMessageData msgSend) {
		// TODO Auto-generated method stub

		// Send the message to other session
		
	}

	@Override
	public void evtRcvMsrpChunk(MsrpChunkData msrpChunk) {

		// Send the message to other session
		MsrpSessionHd sessMsrp = getSessionHd();

		confHub.evtRcvChunk(sessMsrp, msrpChunk);
	}

	@Override
	public void evtRcvChunckResponse(MsrpResponseData respMsg) {
		// TODO Auto-generated method stub

		// Send the message to other session
		
	}

	@Override
	public void evtRcvAbortMsrpChunck(MsrpChunkData msrpChunk) {
		// TODO Auto-generated method stub

		// Send the message to other session
		
	}

	@Override
	public void evtSendChunkedMsgFailure(MsrpChunkData msrpChunk) {
		// TODO Auto-generated method stub

		// Send the message to other session
		
	}

}
