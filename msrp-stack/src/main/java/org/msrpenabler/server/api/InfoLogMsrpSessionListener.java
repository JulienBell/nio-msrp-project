package org.msrpenabler.server.api;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Julien Bellanger
 *
 */
public class InfoLogMsrpSessionListener extends MsrpSessionListener {

	private static final Logger logger = LoggerFactory.getLogger(InfoLogMsrpSessionListener.class);
	
	private String name;
	private String prefix;

	public String getName() {
		return name;
	}

	public InfoLogMsrpSessionListener(String name) {
		this.name = name;
		this.prefix= "["+name+"] ";
		
	}

	@Override
	public void evtSessConnect() {
		logger.info(prefix+"EVT SessConnect ");
	}

	@Override
	public void evtSessDisconnect(DisconnectReason disconnectReason) {
		logger.info(prefix+"EVT SessDisconnect {}", disconnectReason);
	}

	@Override
	public void evtSessDisconnect( DisconnectReason disconnectReason,
			List<MsrpMessageData> listMsgFailed) {
		logger.info(prefix+"EVT SessDisconnect {}", disconnectReason);

		if (listMsgFailed != null) {

			for (MsrpMessageData msg: listMsgFailed) {
				logger.info(prefix+"EVT SessDisconnect msg {}", msg.toString());
			}
		}
	}

	@Override
	public void evtRcvMessage(MsrpMessageData msrpContent,
			boolean wasChunked) {
		logger.info(prefix+"EVT evtRcvMessage, content wrapper: {}", msrpContent.toString());
		logger.info(prefix+"EVT evtRcvMessage, content bytebuff: {}", msrpContent.content().toString());
		logger.info(prefix+"EVT evtRcvMessage, wasChunked : {}", wasChunked);
	}

	@Override
	public void evtSendReportRcv(MsrpReportData msrpContent) {
		logger.info(prefix+"EVT evtSendReportRcv, msg {}", msrpContent);
	}

	@Override
	public void evtSendMsgSuccess(MsrpMessageData msrpContent) {
		logger.info(prefix+"EVT evtSendMsgSuccess, msg {}", msrpContent);
	}

	@Override
	public void evtSendMsgFailure(MsrpMessageData msg) {
		logger.info(prefix+"EVT evtSendMsgFailure ");
		logger.info(prefix+"EVT SessDisconnect msg {}", msg.toString());
	}

	@Override
	public void evtRcvMsrpChunk(MsrpChunkData msrpChunk) {
		logger.info(prefix+"EVT evtRcvMsrpChunk {}", msrpChunk);
	}

	@Override
	public void evtRcvAbortMsrpChunck(MsrpChunkData msrpChunk) {
		logger.info(prefix+"EVT evtRcvAbortMsrpChunck {}", msrpChunk);
	}

	@Override
	public void evtSendChunkedMsgFailure(MsrpChunkData msrpChunk) {
		logger.info(prefix+"EVT evtSendChunkedMsgFailure ");
	}

	@Override
	public void evtRcvResponse(MsrpResponseData respMsg) {
		logger.info(prefix+"EVT evtRcvResponse: {}, {}", respMsg.getMessageId(), respMsg.getStatus());
		logger.info(prefix+"EVT evtRcvResponse: initial full msg {}", respMsg.getAssociatedMessageData());

	}

	@Override
	public void evtRcvChunckResponse(MsrpResponseData respMsg) {
		logger.info(prefix+"EVT evtRcvChunckResponse: {}, {}", respMsg.getMessageId(), respMsg.getStatus());
		logger.info(prefix+"EVT evtRcvChunckResponse: initial chunk msg {}", respMsg.getAssociatedChunkMsgData());
	}

		
}
