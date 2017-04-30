package org.msrpenabler.mculib.groupsess;

import java.net.UnknownHostException;

import org.msrpenabler.server.api.EnumSessionOptions;
import org.msrpenabler.server.api.MsrpChunkAggregator;
import org.msrpenabler.server.api.MsrpChunkData;
import org.msrpenabler.server.api.MsrpMessageData;
import org.msrpenabler.server.api.MsrpSessionHd;
import org.msrpenabler.server.api.MsrpSessionHdFuture;
import org.msrpenabler.server.api.MsrpSessionListener;
import org.msrpenabler.server.exception.SessionHdException;
import org.msrpenabler.server.exception.TransactionException;

public class GroupNamedSession implements MsrpSessionHd {

	
	// TODO :
	// TODO : Create a meta MsrpSession Hd associated to a private name 
	// TODO : and links to many real MsrpSession Hd.
	// TODO : Send method dispatch msg other all sessions
	// TODO : Receive method just call a common handler
	// TODO :
	// TODO : This Meta session can be attached to a conference and allowing to not dispatch msg received on a group 
	// TODO : to others sessions shared the same Group sessions
	
	
	private Object userCtx;

	@Override
	public void addSessListener(MsrpSessionListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeSessListener(MsrpSessionListener sessListener) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public String getSessionId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSessionIdNoChk() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isRef() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isUnRef() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void sendMsrpMessage(MsrpMessageData msrpMessage)
			throws SessionHdException, TransactionException {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendMsrpChunkedMsg(MsrpChunkAggregator msrpAggChunk)
			throws SessionHdException, TransactionException {
		// TODO Auto-generated method stub

	}

	@Override
	public MsrpChunkAggregator sendMsrpChunk(MsrpChunkData msrpChunk,
			boolean saveChunks) throws SessionHdException, TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sendAbortMsrpChunck(MsrpChunkAggregator msrpAggChunk)
			throws SessionHdException {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void setRemotePath(String remotePath) throws SessionHdException {
		throw new SessionHdException("Unsupported method on a GroupNamedSession");
	}

	@Override
	public MsrpSessionHdFuture connect(int connectTOms)
			throws SessionHdException, UnknownHostException, Exception {
		throw new SessionHdException("Unsupported method on a GroupNamedSession");
	}

	@Override
	public MsrpSessionHdFuture bind() throws SessionHdException,
			TransactionException {
		throw new SessionHdException("Unsupported method on a GroupNamedSession");
	}

	@Override
	public String getRemotePath() throws SessionHdException {
		throw new SessionHdException("Unsupported method on a GroupNamedSession");
	}

	@Override
	public String getLocalPath() throws SessionHdException {
		throw new SessionHdException("Unsupported method on a GroupNamedSession");
	}


	@Override
	public boolean isReusable() throws SessionHdException {
		return false;
	}


	@Override
	public void sendExplicitResponse(MsrpMessageData receivedMessage)
			throws SessionHdException {
		throw new SessionHdException("Unsupported method on a GroupNamedSession");
	}

	@Override
	public MsrpChunkAggregator createMsrpChunkedMsg(
			MsrpMessageData msrpMsgToChunck, int chunckLength)
			throws SessionHdException {
		throw new SessionHdException("Unsupported method on a GroupNamedSession");
	}


	@Override
	public void close() throws TransactionException, SessionHdException {
		throw new SessionHdException("Unsupported method on a GroupNamedSession");
	}

	@Override
	public void setOption(EnumSessionOptions option) {
		// TODO Auto-generated method stub
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

	/* (non-Javadoc)
	 * @see org.msrpenabler.server.api.MsrpSessionHd#setAsUnref()
	 */
	@Override
	public void setAsUnref() {
		// TODO Auto-generated method stub
		
	}


}
