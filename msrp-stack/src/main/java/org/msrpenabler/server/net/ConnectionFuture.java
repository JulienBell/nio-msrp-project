package org.msrpenabler.server.net;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.DefaultChannelPromise;

import org.msrpenabler.server.api.MsrpSessionHdFuture;
import org.msrpenabler.server.cnx.MsrpConnexion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionFuture extends DefaultChannelPromise implements MsrpSessionHdFuture 
{
	private static final Logger logger = LoggerFactory.getLogger(ConnectionFuture.class);

	protected Boolean isLastListenerCalled= Boolean.FALSE;
	private MsrpConnexion cnx;

	public ConnectionFuture(MsrpConnexion msrpCnx) {
		super(msrpCnx.getChannel());
		this.cnx = msrpCnx;
	}

	public ConnectionFuture(MsrpConnexion msrpCnx, Channel channel) {
		super(channel);
		this.cnx = msrpCnx;
	}

	@Override
	public ConnectionFuture sync() throws InterruptedException {

		//super.sync();
		
		ChannelFutureListener notifListener = new ChannelFutureListener() {
			
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				
				ConnectionFuture cnxFut = (ConnectionFuture) future;
				
				logger.info("future isLastListenerCalled {}", cnxFut.isLastListenerCalled);
				
				synchronized (cnxFut) {
					cnxFut.isLastListenerCalled = true;
					cnxFut.notifyAll();
				}
			}
		};

		this.addListener(notifListener);

		// 10 secondes max
		int iLoop = 0;
		synchronized (this) {
			while(!isLastListenerCalled && iLoop < 20 ) {
				try {
					wait(500);
					iLoop++;
				} catch (InterruptedException e) {
					logger.warn("Interrupt received in ConnectionFuture sync() ");
				}
			}
		}
		
		logger.info("end of sync: {}", isLastListenerCalled);
		
		return this;
	}



	/**
	 * 
	 * @return
	 */
	public MsrpConnexion getMsrpConnection() {

		return cnx;
		
	}





}