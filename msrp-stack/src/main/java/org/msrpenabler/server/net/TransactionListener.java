package org.msrpenabler.server.net;

import io.netty.util.concurrent.GenericFutureListener;

public abstract interface TransactionListener extends GenericFutureListener<TransactionFuture> {
	
    public abstract void operationComplete(TransactionFuture future);
    
}