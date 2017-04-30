package org.msrpenabler.server.api;

public class NotifOptions {

	private boolean notifRcvMsg;
	private boolean notifRcvMsgChunck;

	private boolean notifSendMsgFailure;
	private boolean notifSendChunckFailure;
	

	
	public boolean isNotifMsg() {
		return notifRcvMsg;
	}

	public void setNotifMsg(boolean notifMsg) {
		this.notifRcvMsg = notifMsg;
	}

	public boolean isNotifMsgChunck() {
		return notifRcvMsgChunck;
	}

	public void setNotifMsgChunck(boolean notifMsgChunck) {
		this.notifRcvMsgChunck = notifMsgChunck;
	}

	
	public NotifOptions(boolean notifMsg, boolean notifMsgChunck, boolean notifSendMsgFailure, boolean notifSendChunckFailure) {
		setNotifMsg(notifMsg);
		setNotifMsgChunck(notifMsgChunck);
		setNotifSendMsgFailure(notifSendMsgFailure);
		setNotifSendChunckFailure(notifSendChunckFailure);
	}

	public boolean isNotifSendMsgFailure() {
		return notifSendMsgFailure;
	}

	public void setNotifSendMsgFailure(boolean notifSendMsgFailure) {
		this.notifSendMsgFailure = notifSendMsgFailure;
	}

	public boolean isNotifSendChunckFailure() {
		return notifSendChunckFailure;
	}

	public void setNotifSendChunckFailure(boolean notifSendChunckFailure) {
		this.notifSendChunckFailure = notifSendChunckFailure;
	}

//	public boolean isOutpuLock() {
//		return lockOutput;
//	}
//
//	public boolean islistenersLocked() {
//		return lockInput;
//	}

}
