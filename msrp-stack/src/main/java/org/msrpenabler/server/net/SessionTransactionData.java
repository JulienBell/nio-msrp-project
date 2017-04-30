package org.msrpenabler.server.net;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.msrpenabler.server.api.DisconnectReason;
import org.msrpenabler.server.api.MsrpMessageData;
import org.msrpenabler.server.api.MsrpResponseData;
import org.msrpenabler.server.api.MsrpReportData;
import org.msrpenabler.server.exception.SessionHdException;
import org.msrpenabler.server.util.MsrpSyntaxRegexp;


public abstract class SessionTransactionData {

	private String localSessPath;
	private String remoteSessPath;
	private String localSessId=null;
	private String remoteSessId=null;
	
	private ConnectionFuture cnxFuture;
	
	private Queue<String> setOfTransId;
	volatile public boolean closeAsked;
	volatile private boolean isConnected=false;

	public SessionTransactionData(String localPath, String remotePath) throws SessionHdException {
		this.localSessPath = localPath;
		this.remoteSessPath = remotePath;
		
		if (localPath != null) {
			this.localSessId = MsrpSyntaxRegexp.getSessionId(localPath);
		}
		if (remotePath != null) {
			this.remoteSessId = MsrpSyntaxRegexp.getSessionId(remotePath);
		}
		
		this.closeAsked = false;
		this.setOfTransId = new LinkedList <String>();
	}

	
	public String getRemoteSessId() {
		return remoteSessId;
	}


	public String getLocalSessId() {
		return localSessId;
	}


	protected String getRemoteSessPath() {
		return remoteSessPath;
	}

	protected String getLocalSessPath() {
		return localSessPath;
	}

	public void setLocalSessPath(String localPath) throws SessionHdException {
		this.localSessPath = localPath;
		if (localPath != null) {
			this.localSessId = MsrpSyntaxRegexp.getSessionId(localPath);
		}
		else {
			this.localSessId = null;
		}
	}

	public void setRemoteSessPath(String remotePath) throws SessionHdException {
		this.remoteSessPath = remotePath;
		if (remotePath != null) {
			this.remoteSessId = MsrpSyntaxRegexp.getSessionId(remotePath);
		}
		else {
			this.remoteSessId = null;
		}
	}
	
	public void addTransId( String  transId) {
		setOfTransId.add(transId);
	}

	public void removeTransId( String  transId) {
		setOfTransId.remove(transId);
	}
	
	public String poolTransaction() {

		return setOfTransId.poll();
	}

	public boolean isActiveTransaction() {
		
		return ! setOfTransId.isEmpty();
	}

	public void setCnxFuture(ConnectionFuture cnxInBoundCnx) {
		this.cnxFuture = cnxInBoundCnx;
	}

	public ConnectionFuture getCnxFuture() {
		return cnxFuture;
	}
	
	
	public void setConnected(boolean connected) {
		isConnected=connected;
	}
	
	public boolean isConnected() {
		return isConnected;
	}
	
	
	/*
	 * Abstract listener interfaces
	 */
	protected abstract void evtReceiveMsg(MsrpMessageWrapper msg);
	
	protected abstract void evtReceiveReport(String messageId, MsrpReportData msg) ;
	
	protected abstract void evtReceiveResponse(MsrpResponseData msg) ;

	protected abstract void evtCnxSessionAccepted() ;
	
	protected abstract void evtCnxSessionFailure(Throwable cause) ;

	protected abstract void evtSessDisconnect(DisconnectReason reason) ;	

	protected abstract void evtSessDisconnect(DisconnectReason reason, List<MsrpMessageData> listMsgFailed) ;

	public abstract void evtMsgSendFailure(MsrpMessageWrapper msgSend) ;


	
}
