package org.msrpenabler.server.net;


public class TransactionContext {
		
	TransactionFuture transFutur;
	MsrpMessageWrapper message;

	public TransactionContext(MsrpMessageWrapper message, TransactionFuture transFutur, SessionTransactionData sessData) {
		this.transFutur = transFutur;
		this.message = message;
		this.transFutur.sessData = sessData;
	}

	
	public void setTansactionId(String transId) {
		message.transactionId = transId;
		transFutur.setTansactionId(transId);
	}
	public void removeTansactionId(String transId) {
		message.transactionId = null;
		transFutur.removeTansactionId(transId);
	}	
	
}

