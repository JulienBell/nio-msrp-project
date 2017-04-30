package org.msrpenabler.server.api;

import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.LinkedList;

import org.msrpenabler.server.cnx.MsrpAddrServer;
import org.msrpenabler.server.exception.SessionHdException;
import org.msrpenabler.server.exception.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Manage lock :
 *  - a list of received message in lock mode
 *  - a list of msg block in sending queue if in lock mode
 *  - next listener are not call until unlock 
 *
 *  Allow to supervise event in a dedicate listener even if lock is set :
 *  - super listener is always call if active even if lock is Set  
 *  
 * 
 * @author Julien
 *
 */
public class SupervisedMsrpSession extends DefaultMsrpSession {

    private static final Logger logger =
        LoggerFactory.getLogger(SupervisedMsrpSession.class);

    private LockOptions lockOptions;

	private final SupervisedMultiListeners listenersSet;
	
	
	public SupervisedMsrpSession(MsrpSessionsFactory sessMgnt, MsrpAddrServer addrSrv,
			MsrpSessionListener sessListener, MsrpSessionListener superListener,
			int inactivityTO, LockOptions lockOptions)
	throws URISyntaxException, SessionHdException, UnknownHostException 
	{
		
		super(sessMgnt, addrSrv, null, false, inactivityTO);
		
		this.lockOptions = lockOptions;
		
		listenersSet  = new SupervisedMultiListeners(lockOptions);
		
		listenersSet.setSupervisorListener(superListener);
		listenersSet.addListener(sessListener);

		listenersSet.attachSessionHd(this);
	}

	public SupervisedMsrpSession(MsrpSessionsFactory sessMgnt, MsrpAddrServer addrSrv,
			MsrpSessionListener sessListener, MsrpSessionListener superListener,
			int inactivityTO, boolean lockInput, boolean lockOutput,
			boolean discardInput, boolean discardOutput)
	throws URISyntaxException, SessionHdException, UnknownHostException 
	{
		
		super(sessMgnt, addrSrv, null, false, inactivityTO);
		
		lockOptions = new LockOptions(lockInput, lockOutput, discardInput, discardOutput);
		 
		listenersSet  = new SupervisedMultiListeners(lockOptions);
		
		listenersSet.setSupervisorListener(superListener);
		listenersSet.addListener(sessListener);

		listenersSet.attachSessionHd(this);
	}

	public SupervisedMsrpSession(MsrpSessionsFactory sessMgnt, MsrpAddrServer addrSrv,
			int inactivityTO, boolean lockInput, boolean lockOutput,
			boolean discardInput, boolean discardOutput)
	throws URISyntaxException, SessionHdException, UnknownHostException 
	{
		
		this(sessMgnt, addrSrv, null, null, inactivityTO, lockInput, lockOutput, discardInput, discardOutput);
	}

	public SupervisedMsrpSession(MsrpSessionsFactory sessMgnt, MsrpAddrServer addrSrv, int inactivityTO) 
	throws UnknownHostException, URISyntaxException, SessionHdException
	{
		this(sessMgnt,addrSrv, inactivityTO, false, false,false,false);
	}
	
	public void setSupervisorListener(MsrpSessionListener superListener) {
		listenersSet.setSupervisorListener(superListener);
	}

	public SupervisedMultiListeners getSupervisedSessListener() {
		return listenersSet;
	}

	@Override
	public MultiListeners getSessListener() {
		return listenersSet;
	}

	@Override
	public void addSessListener(MsrpSessionListener sessListener) {
		listenersSet.addListener( sessListener );
	}

	@Override
	public void removeSessListener(MsrpSessionListener sessListener) {
		listenersSet.removeListener(sessListener);
	}
	
	public MsrpSessionListener getSupervisorListener() {
		return listenersSet.getSupervisorListener();
	}
	
	public void updateLockOutput(boolean isActive) {
		lockOptions.setLockOutput(isActive);

		if (!isActive) {
			executeTaskQueue();
		}

	}

	public void updateLockInput(boolean isActive) {
		lockOptions.setLockInput(isActive);
		
		if (!isActive) {
			SupervisedMultiListeners listenerSet = getSupervisedSessListener();

			listenerSet.executeTaskQueue();
		}
	}

	public void updateDiscardInput(boolean discardInput) {
		lockOptions.setDiscardInput(discardInput);
	}

	public void updateDiscardOutput(boolean discardOutput) {
		lockOptions.setDiscardOutput(discardOutput);
	}
	
	
	abstract class TaskLockedOutput extends TaskLocked<SupervisedMsrpSession> {

		public TaskLockedOutput(SupervisedMsrpSession container) {
			super(container);
		}
	}

	private LinkedList<TaskLockedOutput> taskQueue = new LinkedList<TaskLockedOutput>();
	
	/**
	 * Call on unblock the session
	 */
	public void executeTaskQueue() {
	
		TaskLockedOutput task;
		do {
            task = taskQueue.poll();

            if (task != null)  {
            	
            	//TODO
            	logger.warn("call a task on {}", this.getLocalPath());
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
	 * 
	 * @param msrpMessage : message to send
	 * @throws SessionHdException 
	 * @throws TransactionException 
	 */
	@Override
	public void sendMsrpMessage(MsrpMessageData msrpMessage) throws SessionHdException, TransactionException {
	
		// Check output lock 
		// 		if discard out not set and lockout set or cnx is not yet connected store the msg
		//		else send the message 
		
		final MsrpMessageData msg = msrpMessage;
		
		boolean isCon = isConnected();
				
		logger.info("Msg to send with lockout {}, discardout {}, isCon {}", lockOptions.isLockOutput(), lockOptions.isDiscardOutput(), isCon);

		if (!lockOptions.isLockOutput() && !lockOptions.isDiscardOutput() && isCon ) {
			super.sendMsrpMessage(msrpMessage);
		}
		else if (lockOptions.isDiscardOutput()) {
			msrpMessage.release();
			return;
		}
		else {
			
			if (!isCon) {
				logger.info("Msg delayed as sess not connected, msg {}", msrpMessage.getMessageId());
			}
			else {
				logger.info("Put task msg to send in queue, msg {}", msrpMessage.getMessageId());
			}
			
			// Add this Task in a Queue

			// Not necessary here 
			//msrpMessage.retain();
			
			TaskLockedOutput task = new TaskLockedOutput(this) {
				
				@Override
				public void run() {

					logger.info("Execute task send id {}", msg.getMessageId());
					container.sendMsrpMessage0(msg);
				}
			};
			taskQueue.add(task);
		}
	}	

	/**
	 * 
	 * @param msrpChunk : chunck to send
	 * @throws SessionHdException 
	 * @throws TransactionException 
	 */
	@Override
	public MsrpChunkAggregator sendMsrpChunk(MsrpChunkData msrpChunk, boolean saveChunks) throws SessionHdException, TransactionException {
	
		// Check output lock 
		// 		if discard out not set and lockout set or cnx is not yet connected store the msg
		//		else send the message 
		
		final MsrpChunkData chunk = msrpChunk;
		MsrpChunkAggregator chunkAgg=null;
		
		boolean isCon = isConnected();
		
		if (!lockOptions.isLockOutput() && !lockOptions.isDiscardOutput() && isCon ) {
			chunkAgg = super.sendMsrpChunk(msrpChunk, saveChunks);
		}
		else if (lockOptions.isDiscardOutput()) {
			msrpChunk.release();
			return chunkAgg;
		}
		else {
			
			if (saveChunks) {
				logger.warn("Save chunck is not supported on asynchronous send");
			}
			
			if (!isCon) {
				logger.warn("Chunck delayed as sess not connected, msg {}", msrpChunk.getContentByte());
			}
			else {
				logger.info("Put task chunck to send in queue, msg {}, start range {}", msrpChunk.getMessageId(), msrpChunk.getByteStartRange());
			}
			
			// Add this Task in a Queue
			TaskLockedOutput task = new TaskLockedOutput(this) {
				
				@Override
				public void run() {
					logger.info("Execute delayed send chunk start range {}, id {}", chunk.getByteStartRange(), chunk.getMessageId());
					container.sendMsrChunk0(chunk);
				}
			};
			taskQueue.add(task);
		}
		return chunkAgg;
	}	

	protected void sendMsrChunk0(MsrpChunkData msrpChunk) {
		// TODO -- voir comment remonter exception echec envoi
		try {
			super.sendMsrpChunk(msrpChunk, false);
		} catch (SessionHdException e) {
			logger.error("Fail background sendMsrpMessage {} ", e);
		} catch (TransactionException e) {
			logger.error("Fail background sendMsrpMessage {} ", e);
		}
	}

	protected void sendMsrpMessage0(MsrpMessageData msg) {
		
		// TODO -- voir comment remonter exception echec envoi
		try {
			super.sendMsrpMessage(msg);
		} catch (SessionHdException e) {
			logger.error("Fail background sendMsrpMessage {} ", e);
		} catch (TransactionException e) {
			logger.error("Fail background sendMsrpMessage {} ", e);
		}
	}


	/**
	 * 
	 * @param msrpMessage : : message to send even if lock is active
	 * @throws SessionHdException 
	 * @throws TransactionException 
	 */
	public void sendMsrpMessageNoLock(MsrpMessageData msrpMessage) throws SessionHdException, TransactionException {
		
		super.sendMsrpMessage(msrpMessage);
	}
	
	
	
	
}
