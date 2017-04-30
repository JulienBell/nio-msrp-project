package org.msrpenabler.mcu.restclt;

import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.List;

import org.msrpenabler.mcu.model.McuSessionContext;
import org.msrpenabler.mcu.restsrv.SwMSRPCompliantPathMethodEnum;
import org.msrpenabler.mcu.restsrv.SwMSRPCompliantQueryParamsEnum;
import org.msrpenabler.mculib.cnf.ConferenceUnit;
import org.msrpenabler.server.api.DisconnectReason;
import org.msrpenabler.server.api.MsrpChunkData;
import org.msrpenabler.server.api.MsrpMessageData;
import org.msrpenabler.server.api.MsrpReportData;
import org.msrpenabler.server.api.MsrpResponseData;
import org.msrpenabler.server.api.MsrpSessionListener;
import org.msrpenabler.server.api.NotifOptions;
import org.msrpenabler.server.exception.SessionHdException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwMSRPCompliantMcuSessListener extends MsrpSessionListener {

    private static final Logger logger =
        LoggerFactory.getLogger(SwMSRPCompliantMcuSessListener.class);
    
    final NotifOptions notifOptions;

	public NotifOptions getNotifOptions() {
		return notifOptions;
	}

	public SwMSRPCompliantMcuSessListener(NotifOptions notifOptions) {
		this.notifOptions = notifOptions;
	}

	@Override
	public void evtSessConnect() {
	
		McuSessionContext ctx = (McuSessionContext) getSessionHd().getUserContext();
		
		ConferenceUnit confHd = (ConferenceUnit) ctx.getAttribute("hub");
		
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(SwMSRPCompliantQueryParamsEnum.event.toString(), SwMSRPCompliantPathMethodEnum.SESS_CONNECT.getValue());
		
		params.put(SwMSRPCompliantQueryParamsEnum.hub.toString(), confHd.getConfId());
		
		try {
			params.put(SwMSRPCompliantQueryParamsEnum.sess.toString(), ctx.getSess().getLocalPath());
		} catch (SessionHdException e) {
			logger.warn("Failed to get Local path ", e);
		}
		
		ctx.call_notif(null, params);
	}

	@Override
	public void evtSessDisconnect(DisconnectReason disconnectReason) {

		logger.info("Disconnection with cause {}", disconnectReason);
		
		McuSessionContext ctx = (McuSessionContext) getSessionHd().getUserContext();
		
		ConferenceUnit confHd = (ConferenceUnit) ctx.getAttribute("hub");

		String confId = "_unknown_";
		if (confHd != null) {
			try {
				confId = confHd.getConfId();
				confHd.detachSession(getSessionHd());
			} catch (SessionHdException e) {
				logger.warn("Failed to detach session {} from conf {} ", getSessionHd(), confId, e);
			}
		}
		
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(SwMSRPCompliantQueryParamsEnum.event.toString(), SwMSRPCompliantPathMethodEnum.SESS_DISCONNECT.getValue());
		
		params.put(SwMSRPCompliantQueryParamsEnum.hub.toString(), confId);
		
		try {
			params.put(SwMSRPCompliantQueryParamsEnum.sess.toString(), ctx.getSess().getLocalPath());
		} catch (SessionHdException e) {
			logger.warn("Failed to get Local path ", e);
		}
		
		
		String cause;
		
		switch (disconnectReason) {
		
		case CNX_FAILURE:
			cause = "bind failed";
			break;
			
		case INACTIVITY_RW:
			cause = "INACTIVITY";
			break;
			
		case LOCAL_CLOSE:
			cause = "eof stack";
			break;
			
		case REMOTE_CLOSE:
			cause = "TCP";
			break;
			
		default:
			cause = "switch failure";
			break;

		}
		
		params.put(SwMSRPCompliantQueryParamsEnum.cause.toString(), cause);
				
		ctx.call_notif(null, params);
	}

	@Override
	public void evtSessDisconnect(DisconnectReason disconnectReason,
			List<MsrpMessageData> listMsgFailed) {

		for( MsrpMessageData msg : listMsgFailed) {
			evtSendMsgFailure(msg);
		}
		
		evtSessDisconnect(disconnectReason);
	}

	@Override
	public void evtRcvMessage(MsrpMessageData msrpContent, boolean wasChunked) {
		
		logger.info("Check message counter: {}, msgId {}", msrpContent.refCnt(), msrpContent.getMessageId());

		if ( ! notifOptions.isNotifMsg() ) {

			// Release counter on message
			logger.debug("notif Msg is set to Null: {}", notifOptions.isNotifMsg());
			msrpContent.release();

			return;
		}
		
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(SwMSRPCompliantQueryParamsEnum.event.toString(), SwMSRPCompliantPathMethodEnum.DIRECT_MSG.getValue());
		
		McuSessionContext ctx = (McuSessionContext) getSessionHd().getUserContext();
		
		ConferenceUnit confHd = (ConferenceUnit) ctx.getAttribute("hub");
		
		params.put(SwMSRPCompliantQueryParamsEnum.hub.toString(), confHd.getConfId());
		
		try {
			params.put(SwMSRPCompliantQueryParamsEnum.sess.toString(), ctx.getSess().getLocalPath());
		} catch (SessionHdException e) {
			logger.warn("Failed to get Local path ", e);
		}
		
		params.put(SwMSRPCompliantQueryParamsEnum.msgid.toString(), msrpContent.getMessageId());
		params.put(SwMSRPCompliantQueryParamsEnum.method.toString(), "SEND");

		ByteBuf content = msrpContent.content().duplicate();
		String contentType=msrpContent.getContentType();
		
		logger.debug("content type: {}, content ref count {}", contentType, content.refCnt());
		if (0 == content.readableBytes()) {
			logger.warn("Strange send a msg with 0 as nb readable = {}", content.readableBytes());
		}
		
		// Msg has not to be already here
		// Don't deallocate this buf after having call_notif, it will be done on writing op
			
		ctx.call_notif(null, params, contentType, content);
	}

	@Override
	public void evtRcvResponse(MsrpResponseData respMsg) {

		logger.warn("Not implemented !!! ");
	}

	@Override
	public void evtSendReportRcv(MsrpReportData msrpContent) {
		logger.warn("Not implemented !!! ");
	}

	@Override
	public void evtSendMsgSuccess(MsrpMessageData msrpContent) {
		logger.warn("Not implemented !!! ");
	}

	@Override
	public void evtSendMsgFailure(MsrpMessageData msgSend) {

		
		logger.info("Check message counter: {}, msgId {}", msgSend.refCnt(), msgSend.getMessageId());
		if ( ! notifOptions.isNotifSendMsgFailure() ) {

			logger.debug("notif Msg Failure is set to Null: {}", notifOptions.isNotifSendMsgFailure());
			msgSend.release();
			return;
		}

		
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(SwMSRPCompliantQueryParamsEnum.event.toString(), SwMSRPCompliantPathMethodEnum.CANNOT_SEND.getValue());

		McuSessionContext ctx = (McuSessionContext) getSessionHd().getUserContext();
		
		ConferenceUnit confHd = (ConferenceUnit) ctx.getAttribute("hub");
		
		params.put(SwMSRPCompliantQueryParamsEnum.hub.toString(), confHd.getConfId());
		
		try {
			params.put(SwMSRPCompliantQueryParamsEnum.sess.toString(), ctx.getSess().getLocalPath());
		} catch (SessionHdException e) {
			logger.warn("Failed to get Local path ", e);
		}
		
		params.put(SwMSRPCompliantQueryParamsEnum.msgid.toString(), msgSend.getMessageId());
		params.put(SwMSRPCompliantQueryParamsEnum.method.toString(), "SEND");

		ByteBuf content = msgSend.content().duplicate();
		String contentType=msgSend.getContentType();
		
		logger.debug("content type: {}, content ref count {}", contentType, content.refCnt());
		if (0 == content.readableBytes()) {
			logger.warn("Strange send a msg with 0 as nb readable = {}", content.readableBytes());
		}
		
		// Msg has not to be already here
		// Don't deallocate this buf after having call_notif, it will be done on writing op
			
		ctx.call_notif(null, params, contentType, content);
	}

	@Override
	public void evtRcvMsrpChunk(MsrpChunkData msrpChunk) {

		logger.info("Check message counter: {}, msgId {}, start range {}", msrpChunk.refCnt(), msrpChunk.getMessageId(), msrpChunk.getByteStartRange());
		if ( ! notifOptions.isNotifMsgChunck()) {
			
			logger.debug("notif chunck receive set to Null: {}", notifOptions.isNotifMsgChunck());
			
			msrpChunk.release();
			
			return;
		}

		logger.warn("Not implemented !!! ");
		msrpChunk.release();

	}

	@Override
	public void evtRcvChunckResponse(MsrpResponseData respMsg) {

		logger.warn("Not implemented !!! ");

	}

	@Override
	public void evtRcvAbortMsrpChunck(MsrpChunkData msrpChunk) {

		logger.warn("Not implemented !!! ");
		msrpChunk.release();

	}

	@Override
	public void evtSendChunkedMsgFailure(MsrpChunkData msrpChunk) {

		logger.info("Check message counter: {}, msgId {}, start range {}", msrpChunk.refCnt(), msrpChunk.getMessageId(), msrpChunk.getByteStartRange());
		if ( ! notifOptions.isNotifSendChunckFailure() ) {
			logger.debug("notif chunck send failure is set to Null: {}", notifOptions.isNotifSendChunckFailure());
			msrpChunk.release();
			return;
		}

		logger.warn("Not implemented !!! ");
		msrpChunk.release();
	}

}
