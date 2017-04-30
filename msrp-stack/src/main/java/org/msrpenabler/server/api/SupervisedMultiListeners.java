package org.msrpenabler.server.api;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SupervisedMultiListeners extends MultiListeners {

    private static final Logger logger =
        LoggerFactory.getLogger(SupervisedMultiListeners.class);

    
    private MsrpSessionListener supervisorListener=null;
    
	private final LockOptions lockOptions;

	abstract class TaskLockedListener extends TaskLocked<SupervisedMultiListeners> {

		public TaskLockedListener(SupervisedMultiListeners container) {
			super(container);
		}
	}

	private LinkedList<TaskLockedListener> taskQueue = new LinkedList<TaskLockedListener>();
	

	public MsrpSessionListener getSupervisorListener() {
		return supervisorListener;
	}

	public void setSupervisorListener(MsrpSessionListener supervisorListener) {
		this.supervisorListener = supervisorListener;
	}

	
	/**
	 *  Constructor 
	 *  
	 * @param lockOptions
	 */
	public SupervisedMultiListeners(LockOptions lockOptions) {
		this.lockOptions = lockOptions;
	}
	
	public void attachSessionHd(MsrpSessionHd sessHd) {
				
		supervisorListener.attachSessionHd(sessHd);
		
		super.attachSessionHd(sessHd);
	}

	/**
	 * Call on unblock the session
	 */
	public void executeTaskQueue() {
	
		TaskLockedListener task;
		do {
			task = taskQueue.poll();

            if (task != null) {
                try {
                    task.run();
                }
                catch ( Exception e ) {
                    logger.error("Failed on execute listener task !!  {}", e);
                }
            }
		}
		while ( task != null);
	}
	
	
	/**
	 * Listeners overrides
	 */
	
	@Override
	public void evtSessConnect() {

		// Send waiting messages
		if (!lockOptions.isLockOutput()) {
			((SupervisedMsrpSession) getSessionHd()).executeTaskQueue();
		}
		
		if (supervisorListener != null) {
			supervisorListener.evtSessConnect();
		}

		// Locks are not applied on this event
		super.evtSessConnect();
	}

	@Override
	public void evtSessDisconnect(DisconnectReason disconnectReason) {

		if (supervisorListener != null) {
			supervisorListener.evtSessDisconnect(disconnectReason);
		}
		
		// Locks are not applied on this event
		super.evtSessDisconnect(disconnectReason);
	}

	@Override
	public void evtSessDisconnect(DisconnectReason disconnectReason,
			List<MsrpMessageData> listMsgFailed) {

		if (supervisorListener != null) {
			supervisorListener.evtSessDisconnect(disconnectReason, listMsgFailed);
		}
		
		// Locks are not applied on this event
		super.evtSessDisconnect(disconnectReason, listMsgFailed);
	}

	@Override
	public void evtRcvMessage(final MsrpMessageData msrpContent, final boolean wasChunked) {
		
		logger.debug("START ref count {}", msrpContent.refCnt());
		
		try {
			
			if (supervisorListener != null) {
				// User has to release the Msg himself
				msrpContent.retain();

				supervisorListener.evtRcvMessage(msrpContent, wasChunked);
			}
			
			if ( lockOptions.isDiscardInput()) {
				logger.info("Discard input message ");
				logger.debug("Discarded input message content {}", msrpContent);
			}
			else if (! lockOptions.isLockInput()) {
				msrpContent.retain();

				super.evtRcvMessage(msrpContent, wasChunked);
			}
			else {
				// Add this Task in a Queue
				msrpContent.retain();
				
				TaskLockedListener task = new TaskLockedListener(this) {
					
					@Override
					public void run() {
						container.evtRcvMessage0(msrpContent, wasChunked);
					}
				};
				taskQueue.add(task);
			}
		}
		finally {
			
			msrpContent.release();
		}
		logger.debug("END, ref count {} ", msrpContent.refCnt());
		
	}

	protected void evtRcvMessage0(final MsrpMessageData msrpContent, final boolean wasChunked) {
		super.evtRcvMessage(msrpContent, wasChunked);
	}

	
	@Override
	public void evtRcvResponse(final MsrpResponseData respMsg) {
		if (supervisorListener != null) {
			supervisorListener.evtRcvResponse(respMsg);
		}
		
		if ( lockOptions.isDiscardInput()) {
			logger.info("Discard input response ");
			logger.debug("Discarded input response content {}", respMsg);
		}
		else if (! lockOptions.isLockInput()) {
			super.evtRcvResponse(respMsg);
		}
		else {
			
			TaskLockedListener task = new TaskLockedListener(this) {
				
				@Override
				public void run() {
					container.evtRcvResponse0(respMsg);
				}
			};
			taskQueue.add(task);
		}
	}

	protected void evtRcvResponse0(MsrpResponseData respMsg) {
		super.evtRcvResponse(respMsg);
	}
	

	@Override
	public void evtSendReportRcv(final MsrpReportData msrpContent) {
		if (supervisorListener != null) {
			supervisorListener.evtSendReportRcv(msrpContent);
		}
		
		if ( lockOptions.isDiscardInput()) {
			logger.info("Discard input report ");
			logger.debug("Discarded input report content {}", msrpContent);
		}
		else if (! lockOptions.isLockInput()) {
			super.evtSendReportRcv(msrpContent);
		}
		else {
			// Add this Task in a Queue
			TaskLockedListener task = new TaskLockedListener(this) {
				
				@Override
				public void run() {
					container.evtSendReportRcv0(msrpContent);
				}
			};
			taskQueue.add(task);
		}
	}
	
	protected void evtSendReportRcv0(MsrpReportData msrpContent) {
		super.evtSendReportRcv(msrpContent);
	}
	

	
	@Override
	public void evtSendMsgSuccess(final MsrpMessageData msrpContent) {
		if (supervisorListener != null) {
			supervisorListener.evtSendMsgSuccess(msrpContent);
		}

		// No lock on sending result
		super.evtSendMsgSuccess(msrpContent);
	}
	
	

	@Override
	public void evtSendMsgFailure(final MsrpMessageData msgSend) {
		if (supervisorListener != null) {
			supervisorListener.evtSendMsgFailure(msgSend);
		}
		
		// No lock on sending result
		super.evtSendMsgFailure(msgSend);
	}

	
	
	@Override
	public void evtRcvMsrpChunk(final MsrpChunkData msrpChunk) {

		// Protect from release while process msg
		logger.debug("First retain on supervised listener");
		
		try {
			if (supervisorListener != null) {
				// User has to release the Msg himself
				logger.debug("Retain for superviserListener");
				msrpChunk.retain();

				supervisorListener.evtRcvMsrpChunk(msrpChunk);
			}
			
			if ( lockOptions.isDiscardInput()) {
				logger.info("Discard input chunk ");
				logger.debug("Discarded input chunk content {}", msrpChunk);
			}
			else if (! lockOptions.isLockInput()) {
				logger.debug("Retain for superviserListener");
				msrpChunk.retain();

				super.evtRcvMsrpChunk(msrpChunk);
			}
			else {
				// Add this Task in a Queue
				logger.debug("Retain before create new task listener");
				msrpChunk.retain();
				
				TaskLockedListener task = new TaskLockedListener(this) {
					
					@Override
					public void run() {
						container.evtRcvMsrpChunk0(msrpChunk);
					}
				};
				taskQueue.add(task);
			}
		}
		finally {
			logger.debug("Last release on supervised listener");
			msrpChunk.release();
		}
	}
	
	
	protected void evtRcvMsrpChunk0(MsrpChunkData msrpChunk) {
		super.evtRcvMsrpChunk(msrpChunk);
	}

	@Override
	public void evtRcvChunckResponse(final MsrpResponseData respMsg) {
		if (supervisorListener != null) {
			supervisorListener.evtRcvChunckResponse(respMsg);
		}
		
		if ( lockOptions.isDiscardInput()) {
			logger.info("Discard input chunk response ");
			logger.debug("Discarded input chunk response content {}", respMsg);
		}
		else if (! lockOptions.isLockInput()) {
			super.evtRcvChunckResponse(respMsg);
		}
		else {

			TaskLockedListener task = new TaskLockedListener(this) {
				
				@Override
				public void run() {
					container.evtRcvChunckResponse0(respMsg);
				}
			};
			taskQueue.add(task);
		}
	}
	
	protected void evtRcvChunckResponse0(MsrpResponseData respMsg) {
		super.evtRcvChunckResponse(respMsg);
	}

	
	@Override
	public void evtRcvAbortMsrpChunck(final MsrpChunkData msrpChunk) {
		if (supervisorListener != null) {
			supervisorListener.evtRcvAbortMsrpChunck(msrpChunk);
		}

		// No lock on abort event
		super.evtRcvAbortMsrpChunck(msrpChunk);
	}

	@Override
	public void evtSendChunkedMsgFailure(final MsrpChunkData msrpChunk) {
		if (supervisorListener != null) {
			supervisorListener.evtSendChunkedMsgFailure(msrpChunk);
		}
		
		// No lock on send failure event
		super.evtSendChunkedMsgFailure(msrpChunk);
	}
	
}
