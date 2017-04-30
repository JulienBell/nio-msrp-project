package org.msrpenabler.server.api;

import java.net.UnknownHostException;

import org.msrpenabler.server.exception.SessionHdException;
import org.msrpenabler.server.exception.TransactionException;


public interface MsrpSessionHd {
	
	/**
	 * Add a new listener to the session 
	 * @param listener
	 */
	public void addSessListener(MsrpSessionListener listener);
	
	/**
	 * 
	 * @param ctx
	 */
	public void setUserContext(Object ctx);

	/**
	 * 
	 * @return
	 */
	public Object getUserContext();
	
	/**
	 * Set remote path of the peer session to connect or bind with
	 * @param remotePath
	 * @throws SessionHdException
	 * @throws RemotePathAlreadySetException 
	 */
	public void setRemotePath(String remotePath) throws SessionHdException;
	
	/**
	 *  Active mode connection with remotePath
	 * @throws SessionHdException
	 * @throws UnknownHostException 
	 * @throws Exception 
	 * @throws BadCnxStatusException
	 */
	public MsrpSessionHdFuture connect(int connectTOsec) throws SessionHdException, UnknownHostException, Exception;
	
	/**
	 * Passive mode waiting connection from remote Path
	 * @throws SessionHdException
	 * @throws TransactionException 
	 * @throws BadCnxStatusException
	 */
	public MsrpSessionHdFuture bind() throws SessionHdException, TransactionException;

	
	public String getRemotePath() throws SessionHdException;

	public String getLocalPath()  throws SessionHdException;

	public String getSessionId() ;

	public String getSessionIdNoChk();

	public boolean isReusable()  throws SessionHdException;

	public boolean isRef();
	public boolean isUnRef();
	
	/**
	 * 
	 * @param msrpMessage
	 * @throws TransactionException 
	 */
	public void sendMsrpMessage(MsrpMessageData msrpMessage)  throws SessionHdException, TransactionException;
	
	/**
	 * 
	 * @param msrpMessage
	 */
	public void sendExplicitResponse(MsrpMessageData receivedMessage)  throws SessionHdException;

	
	/*
	 * Chunk Management
	 */


	/**
	 * 
	 */
	public MsrpChunkAggregator createMsrpChunkedMsg(MsrpMessageData msrpMsgToChunck, int chunckLength)
	 				throws SessionHdException;
	
	/**
	 * 
	 * @param msrpAggChunk
	 * @throws TransactionException 
	 */
	// TODO -- 
	public void sendMsrpChunkedMsg(MsrpChunkAggregator msrpAggChunk)  throws SessionHdException, TransactionException;


	/**
	 * Return an ChunkAggregator if requested
	 * 
	 * @param msrpChunk
	 * @param saveChunks
	 * @return
	 * @throws SessionHdException
	 * @throws TransactionException
	 */
	public MsrpChunkAggregator sendMsrpChunk(MsrpChunkData msrpChunk, boolean saveChunks)	throws SessionHdException, TransactionException;


	// TODO -- 
	public void sendAbortMsrpChunck(MsrpChunkAggregator msrpAggChunk)
	 								throws SessionHdException;

	/**
	 * @throws TransactionException 
	 * @throws SessionHdException 
	 * 
	 */
	void close() throws TransactionException, SessionHdException;

	/**
	 * 
	 * @param option
	 */
	public void setOption(EnumSessionOptions option);


	public void removeSessListener(MsrpSessionListener sessListener);

	/**
	 * 
	 */
	public void setAsUnref();

	
}
