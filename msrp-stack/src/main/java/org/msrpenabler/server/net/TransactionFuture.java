package org.msrpenabler.server.net;

import io.netty.channel.Channel;
import io.netty.channel.DefaultChannelPromise;
import org.msrpenabler.server.api.MsrpResponseData;
import org.msrpenabler.server.api.MsrpSessionHdFuture;


public class TransactionFuture extends DefaultChannelPromise implements MsrpSessionHdFuture {

	private String msgCause;
	
	protected String transID=null;
	SessionTransactionData sessData;
	
	protected MsrpMessageWrapper msgSend;
	protected MsrpResponseData msgResponse=null;
	protected boolean failureReport;
	protected boolean successReport;

	public TransactionFuture(Channel channel, MsrpMessageWrapper msg,	SessionTransactionData sessData, boolean failureReport, boolean successReport) {
	
		super(channel);
		
		this.msgSend = msg;
		
		if (msgSend != null) {
			// retain message until end of the transaction
			msgSend.retain();
		}
		
		this.sessData = sessData;
		this.failureReport = failureReport;
		this.successReport = successReport;
	}
	
	public TransactionFuture addListener( TransactionListener listener ) {
		return (TransactionFuture) super.addListener(listener);
	}

	public void setTansactionId(String transId) {

		transID = transId;
		
		if (msgSend != null) {
			msgSend.transactionId = transId;
		}
		sessData.addTransId(transId);
		
	}	
	public void removeTansactionId(String transId) {
		sessData.removeTransId(transId);
		transID = null;
	}
	
	@Override
	public boolean isDone() {
		return super.isDone();
	}
	@Override
	public TransactionFuture setSuccess() {
		return (TransactionFuture) super.setSuccess();
	}
	@Override
	public TransactionFuture setFailure(Throwable cause) {
		return (TransactionFuture) super.setFailure(cause);
	}
	@Override
	public boolean isSuccess() {
		return super.isSuccess();
	}

	@Override
	public Throwable cause() {
		return super.cause();
	}
	public void setMsgCause(String msgCause) {
		this.msgCause = msgCause;
	}
	public String getMsgCause() {
		return msgCause;
	}

	@Override
	public TransactionFuture sync() throws InterruptedException {
		super.sync();
        return this;
	}
	
    @Override
    public TransactionFuture syncUninterruptibly() {
        super.syncUninterruptibly();
        return this;
    }

    @Override
    public TransactionFuture await() throws InterruptedException {
        super.await();
        return this;
    }

    @Override
    public TransactionFuture awaitUninterruptibly() {
        super.awaitUninterruptibly();
        return this;
    }
	
}
	
