package org.msrpenabler.server.net;

import org.msrpenabler.server.api.internal.MsrpMsgTypeEnum;
import org.msrpenabler.server.cnx.MsrpAddrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class MsgRcvHandler extends SimpleChannelInboundHandler<MsrpMessageWrapper> {

	private TransactionHandler cnx = null;
	private final MsrpAddrServer addrServ;
	
    private static final Logger logger =
        LoggerFactory.getLogger(MsgRcvHandler.class);
	
	public MsgRcvHandler(MsrpAddrServer addrServ) {

		super(false);
        this.addrServ = addrServ;
	}

	public MsgRcvHandler(TransactionHandler cnx) {

		super(false);
		this.addrServ = null;
		this.cnx = cnx;
	}

	
		
	@Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState e = ((IdleStateEvent) evt).state();
            if (e.equals(IdleState.ALL_IDLE)) {
            	logger.info("Inactivity timeout on connection {}", this.cnx);

            	cnx.evtInactivityChannel();
                ctx.close();
                
            } else if (e.equals(IdleState.WRITER_IDLE) ) {
            	logger.info("Writer inactivity timeout on connection {}", this.cnx);
                
            } else if (e.equals(IdleState.READER_IDLE) ) {
            	logger.info("Reader inactivity timeout on connection {}", this.cnx);
            }
        }
    }

	@Override
	public	void channelActive(ChannelHandlerContext ctx) {
		
		logger.info("Channel Active");
	
		// Associated cnx could not be identify before receiving first message
        ctx.fireChannelActive();
        
        // Force to read quickly
        ctx.read();
        ctx.channel().config().setAutoRead(true);
	}
	
	@Override
	public	void channelInactive(ChannelHandlerContext ctx) {
		
		logger.info("Channel Inactive");
		
		if (cnx != null) {
			cnx.evtCloseChannel();
		}
		
		ctx.fireChannelInactive();
	}


	@Override
	protected void channelRead0(
			ChannelHandlerContext ctx,
			MsrpMessageWrapper msg) throws Exception {

		logger.debug("Message received");
		
        logger.debug( "Srv  received type: {}", msg.msgType ); 
        logger.debug( "Srv  received cmd: {}", msg.cmdMSRP ); 
        logger.debug( "Srv  received tid: {}", msg.transactionId ); 
        logger.debug( "Srv  received to_path: {}", msg.toPath ); 
        logger.trace( "Srv  received to_pathSessionID: {}", msg.toPathSessionId ); 
        logger.debug( "Srv  received from_path: {}", msg.fromPath ); 
        logger.trace( "Srv  received from_pathSessionID: {}", msg.fromPathSessionId ); 
        logger.debug( "Srv  received content type: {}", msg.getContentType() ); 
        logger.debug( "Srv  received content length: {}", msg.getContentLength()) ; 
        logger.debug( "Srv  received msg ref count: {}", msg.refCnt()) ; 

        if (cnx == null) {
            if (addrServ == null ) {
                logger.warn("Received an unexpected MSRP request on unbind clt cnx, to-path: {} ", msg.toPath);
                return;
            }

            // This is a server cnx, retrieve the associated session
        	cnx = addrServ.mapBindCnx.remove(msg.toPathSessionId);
            
            if (cnx == null) {
            	logger.warn("Received an unexpected MSRP request on unbind To-Path: {} ", msg.toPath);
            	
            	// Send back an error response in case of SEND request, may be close the channel.
            	if ( msg.msgType == MsrpMsgTypeEnum.MSRP_SEND) {
            		MsrpMessageWrapper resp = msg.generateResponseMsg("481", "Invalid session ");
            		ChannelFuture future = ctx.channel().write(resp);
            		
            		future.addListener(ChannelFutureListener.CLOSE);
            		ctx.channel().flush();
            	}
            	else {
            		ctx.channel().close();
            	}
            	
            	return;
            }
            cnx.setChannel(ctx.channel());

        }

        
        // OK now notify the transaction Handler, it'll do the job
        try {
        	cnx.evtHandlerReceiveMsg(msg);
        }
        catch (Exception e) {
        	logger.error("Exception during msg: {}... exception: ", msg.firstLine, e);
        }

        logger.debug( "End Srv received msg Id {}, ref count: {}", msg.getMessageId(), msg.refCnt()) ; 
        
	}
	
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

		logger.error("Failed on message received", cause);
    	//cause.printStackTrace();
        ctx.close();
    }


}
