package org.msrpenabler.mcu.restclt;

import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.List;

import org.msrpenabler.mcu.model.McuSessionContext;
import org.msrpenabler.mcu.restsrv.QueryParamsEnum;
import org.msrpenabler.server.api.DisconnectReason;
import org.msrpenabler.server.api.MsrpChunkData;
import org.msrpenabler.server.api.MsrpMessageData;
import org.msrpenabler.server.api.MsrpReportData;
import org.msrpenabler.server.api.MsrpResponseData;
import org.msrpenabler.server.api.MsrpSessionListener;
import org.msrpenabler.server.api.NotifOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McuSessListener extends MsrpSessionListener {

    private static final Logger logger =
        LoggerFactory.getLogger(McuSessListener.class);
    
    final NotifOptions notifOptions;

	public NotifOptions getNotifOptions() {
		return notifOptions;
	}

	public McuSessListener(NotifOptions notifOptions) {
		this.notifOptions = notifOptions;
	}

	@Override
	public void evtSessConnect() {
	
		McuSessionContext ctx = (McuSessionContext) getSessionHd().getUserContext();
		
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(QueryParamsEnum.sessid.toString(), ctx.getSess().getSessionId());
		
		ctx.call_notif(NotifMethodEnum.SESSION_CONNECT, params);
	}

	@Override
	public void evtSessDisconnect(DisconnectReason disconnectReason) {

		logger.info("Disconnection with cause {}", disconnectReason);
		
		McuSessionContext ctx = (McuSessionContext) getSessionHd().getUserContext();
		
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(QueryParamsEnum.sessid.toString(), ctx.getSess().getSessionId());
		params.put(QueryParamsEnum.reason.toString(), disconnectReason.toString());
		
		ctx.call_notif(NotifMethodEnum.SESSION_DISCONNECT, params);
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
		
		
		//Check release counter management
		logger.info("Start with message counter: {}, msgId {}", msrpContent.refCnt(), msrpContent.getMessageId());
		
		if ( ! notifOptions.isNotifMsg() ) {
			logger.debug("notif Msg set to Null: {}", notifOptions.isNotifMsg());
			msrpContent.release();
			return;
		}
		
		McuSessionContext ctx = (McuSessionContext) getSessionHd().getUserContext();
		
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(QueryParamsEnum.sessid.toString(), ctx.getSess().getSessionId());
		params.put(QueryParamsEnum.msgid.toString(),  msrpContent.getMessageId());
		params.put(QueryParamsEnum.length.toString(),  Integer.toString(msrpContent.getContentLength()));
		
		ByteBuf content = msrpContent.content().duplicate();
		String contentType=msrpContent.getContentType();
		
		logger.debug("content type: {}, ref count {}", contentType, content.refCnt());
		if (0 == content.readableBytes()) {
			logger.warn("Strange send a msg with 0 as nb readable = {}", content.readableBytes());
		}
		
		// Msg has not to be already here
		// Don't deallocate this buf after having call_notif, it will be done on writing op
		
		ctx.call_notif(NotifMethodEnum.RCV_MSG, params, contentType, content);
	}

	@Override
	public void evtRcvResponse(MsrpResponseData respMsg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void evtSendReportRcv(MsrpReportData msrpContent) {
		// TODO Auto-generated method stub

	}

	@Override
	public void evtSendMsgSuccess(MsrpMessageData msrpContent) {
		// TODO Auto-generated method stub

	}

	@Override
	public void evtSendMsgFailure(MsrpMessageData msgSend) {

		// Check counter management
		logger.info("Message id {}, counter: {}", msgSend.getMessageId(), msgSend.refCnt());
		
		if ( ! notifOptions.isNotifSendMsgFailure() ) {
			logger.debug("notif Msg Failure set to Null: {}", notifOptions.isNotifSendMsgFailure());
			msgSend.release();
			return;
		}

		McuSessionContext ctx = (McuSessionContext) getSessionHd().getUserContext();
		
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(QueryParamsEnum.sessid.toString(), ctx.getSess().getSessionId());
		params.put(QueryParamsEnum.msgid.toString(),  msgSend.getMessageId());
		params.put(QueryParamsEnum.length.toString(),  Integer.toString(msgSend.getContentLength()));
		
		ByteBuf content = msgSend.content().duplicate();
		String contentType=msgSend.getContentType();
		
		// Msg has not to be already here
		// Don't deallocate this buf after having call_notif, it will be done on writing op
		
		ctx.call_notif(NotifMethodEnum.SEND_MSG_FAILURE, params, contentType, content);
	}

	@Override
	public void evtRcvMsrpChunk(MsrpChunkData msrpChunk) {

		// Check counter management ??
		logger.info("Chunk from msgId {}, startrange {}, counter: {}", msrpChunk.getMessageId(), msrpChunk.getByteStartRange(), msrpChunk.refCnt());
		
		if ( ! notifOptions.isNotifMsgChunck()) {
			logger.debug("notif chunck receive set to Null: {}", notifOptions.isNotifMsgChunck());
			return;
		}

		McuSessionContext ctx = (McuSessionContext) getSessionHd().getUserContext();
		
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(QueryParamsEnum.sessid.toString(), ctx.getSess().getSessionId());
		params.put(QueryParamsEnum.msgid.toString(),  msrpChunk.getMessageId());
		params.put(QueryParamsEnum.startrange.toString(),  Integer.toString(msrpChunk.getByteStartRange()));
		params.put(QueryParamsEnum.endrange.toString(),  Integer.toString(msrpChunk.getByteEndRange()));
		params.put(QueryParamsEnum.length.toString(),  Integer.toString(msrpChunk.getContentLength()));
		
		
		ByteBuf content = msrpChunk.content().duplicate();
		String contentType=msrpChunk.getContentType();
		
		logger.debug("content type: {}, refcount {}", contentType, content.refCnt());
		if (0 == content.readableBytes()) {
			logger.warn("Strange send a chunk with 0 as nb readable = {}", content.readableBytes());
		}

		// Msg has not to be already here
		// Don't deallocate this buf after having call_notif, it will be done on writing op
		
		ctx.call_notif(NotifMethodEnum.RCV_CHUNK, params, contentType, content);
	}

	@Override
	public void evtRcvChunckResponse(MsrpResponseData respMsg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void evtRcvAbortMsrpChunck(MsrpChunkData msrpChunk) {
		// TODO Auto-generated method stub

	}

	@Override
	public void evtSendChunkedMsgFailure(MsrpChunkData msrpChunk) {

		// Check counter management ??
		logger.info("Chunk message counter: {}, on msgId {}, start range {}", msrpChunk.refCnt(), msrpChunk.getMessageId(), msrpChunk.getByteStartRange());
		
		if ( ! notifOptions.isNotifSendChunckFailure() ) {
			logger.debug("notif chunck send failure set to Null: {}", notifOptions.isNotifSendChunckFailure());
			msrpChunk.release();
			return;
		}

		McuSessionContext ctx = (McuSessionContext) getSessionHd().getUserContext();
		
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(QueryParamsEnum.sessid.toString(), ctx.getSess().getSessionId());
		params.put(QueryParamsEnum.msgid.toString(),  msrpChunk.getMessageId());
		params.put(QueryParamsEnum.startrange.toString(),  Integer.toString(msrpChunk.getByteStartRange()));
		params.put(QueryParamsEnum.endrange.toString(),  Integer.toString(msrpChunk.getByteEndRange()));
		params.put(QueryParamsEnum.length.toString(),  Integer.toString(msrpChunk.getContentLength()));
		
		
		ByteBuf content = msrpChunk.content().duplicate();
		String contentType=msrpChunk.getContentType();
		
		logger.debug("content type: {}", contentType);

		// Msg has not to be already here
		// Don't deallocate this buf after having call_notif, it will be done on writing op
		
		ctx.call_notif(NotifMethodEnum.SEND_CHUNCK_FAILURE, params, contentType, content);
	}

}
