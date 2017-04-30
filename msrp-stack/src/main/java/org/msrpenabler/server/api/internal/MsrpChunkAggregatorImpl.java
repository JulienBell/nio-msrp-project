package org.msrpenabler.server.api.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.msrpenabler.server.api.MsrpChunkAggregator;
import org.msrpenabler.server.api.MsrpChunkData;
import org.msrpenabler.server.api.MsrpMessageData;
import org.msrpenabler.server.exception.SessionHdException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MsrpChunkAggregatorImpl implements MsrpChunkAggregator, MsrpMessageData {
	
    private static final Logger logger =
        LoggerFactory.getLogger(MsrpChunkAggregatorImpl.class);
	
	private final String messageId;
	private boolean isComplete = false;
	private boolean lastChunckReceived = false;
	
	private int waitingLength = -1;
	private int currentLength = 0;

	private int nbChunk = 0;
	private int nbChunkAck = 0;
	
	private boolean isAutomaticRechunckMsg = false;
	
	private CompositeByteBuf completedMsg = null;

	private TreeMap<Integer, MsrpChunkData> mapContentChunks = new TreeMap<Integer, MsrpChunkData>();
	

	public MsrpChunkAggregatorImpl(String msgId) {
		this.messageId = msgId;
	}


	public Iterator<MsrpChunkData> iterator() {
		
		if (! isComplete()) {
			logger.warn("Aggregator is not complete, return null");
			return null;
		}
		
		Collection<MsrpChunkData> collect = mapContentChunks.values();
		
		return collect.iterator();
	}
	
	public String getMessageId() {
		return messageId;
	}

	/**
	 * 
	 * @param contentChunk - chunk is retained when adding to the aggregator
	 * @throws SessionHdException
	 */
	public void addChunkedMsg(MsrpChunkData contentChunk) throws SessionHdException {

		Integer key = contentChunk.getByteStartRange();

		
		// Check if chunk has already been received
		MsrpChunkData prev = mapContentChunks.get(key);
		if (prev != null) {
			// Just a repetition ... ??
			if (prev.getContentLength() == contentChunk.getContentLength()) {
				return;
			}
			else {
				// suppress older chunck
				MsrpChunkData chk = mapContentChunks.remove(key);
				if (chk != null) chk.release();
				currentLength-=prev.getContentLength();
			}
		}
		
		
		mapContentChunks.put(key, contentChunk);
		contentChunk.retain();
		incNbChunk();
		logger.info("After retain on chunk {}", contentChunk.refCnt());

		currentLength += contentChunk.getContentLength();
		waitingLength = contentChunk.getByteEndRange();
				
		// Check if Start doesn't overlap an already received chunk   
		Entry<Integer, MsrpChunkData> prevChunk = mapContentChunks.floorEntry(key);

		int cutLength = 0;
		int prevEndIndex = 0;

		// Check previous chunck if its not me
		if (prevChunk != null && prevChunk.getKey() < key) {
			// index start with 1 and not zero
			prevEndIndex = prevChunk.getKey() + prevChunk.getValue().getContentLength() - 1;

			if (key <= prevEndIndex) {
				int readIndex = prevChunk.getValue().content().readerIndex();
				int writerIndex = prevChunk.getValue().content().writerIndex();

				int prevLength = writerIndex-readIndex;

				cutLength = prevEndIndex - key + 1;

				if (prevLength < cutLength) {
					throw new SessionHdException("Inconsistent chunck ordering or lentgh, cut: "
							+ cutLength +"  , prevLength: "+ prevLength 
							+", prevIndex: "+ prevChunk.getKey()
							+", currentIndex: "+ key);
				}

				if (prevLength == cutLength) {
					// Suppress previous Chunck
					MsrpChunkData chk = mapContentChunks.remove(prevChunk.getKey());
					if (chk != null) {
						chk.release();
						logger.info("After release on chunk {}", chk.refCnt());
					}
					decNbChunk();
				}
				else {
					// reduce its readable length
					writerIndex = prevChunk.getValue().content().writerIndex() - cutLength;
					prevChunk.getValue().content().writerIndex(writerIndex);
				}

				// recalculate Length correctly 
				currentLength -= cutLength;
			}
		}

		
		// Check next chunck 
		Entry<Integer, MsrpChunkData> nextChunk = mapContentChunks.ceilingEntry(key);

		cutLength = 0;
		int endIndex = 0;

		// Check previous chunck 
		if (nextChunk != null && nextChunk.getKey() > key) {
			// index start with 1 and not zero
			endIndex = key + contentChunk.getContentLength();

			if (nextChunk.getKey() <= endIndex) {
				int readIndex = contentChunk.content().readerIndex();
				int writerIndex = contentChunk.content().writerIndex();

				int currentLength = writerIndex-readIndex;

				cutLength = endIndex - nextChunk.getKey() + 1;

				if (currentLength < cutLength) {
					throw new SessionHdException("Inconsistent chunck ordering or lentgh, cut: "
							+ cutLength +"  , currentLength: "+ contentChunk.getContentLength() 
							+", endIndex: "+ key
							+", nextIndex: "+ nextChunk.getKey());
				}

				if (currentLength == cutLength) {
					// Suppress previous Chunck
					MsrpChunkData chk = mapContentChunks.remove(key);;
					if (chk != null) {
						chk.release();
						logger.info("After release on chunk {}", chk.refCnt());
					}
					decNbChunk();
				}
				else {
					// reduce its readable length
					writerIndex = contentChunk.content().writerIndex() - cutLength;
					contentChunk.content().writerIndex(writerIndex);
				}

				// recalculate Length correctly 
				currentLength -= cutLength;
			}
		}
 		
		
		// Check if message is complete
		logger.info("Continuation flag {}", contentChunk.getContinuationFlag());
		if (contentChunk.getContinuationFlag() == '$') {
			lastChunckReceived = true;
		}
		
		// check global length is reached if last chunck received
		if (lastChunckReceived) {
			
			logger.info("Curr length {}, waiting length {}", currentLength, waitingLength);
			if (currentLength >= waitingLength) {
				isComplete = true;
			}
			
			if (isComplete) {
				
				completedMsg = new CompositeByteBuf(PooledByteBufAllocator.DEFAULT, false, getNbChunk()+1);
				Iterator<Entry<Integer, MsrpChunkData>> iter = mapContentChunks.entrySet().iterator();
				
				while (iter.hasNext()) {
					Entry<Integer, MsrpChunkData> elt = iter.next();
					completedMsg.addComponent(elt.getValue().content());
					logger.info("After add component {}", elt.getValue().refCnt());
				}

				// Set the writer index
				completedMsg.capacity(currentLength);
				completedMsg.writerIndex(currentLength);
			}
		}
		
		return;
	}

	
	public TreeMap<Integer, MsrpChunkData> getListContentChunks() {
		return mapContentChunks;
	}

	
	/*
	 * An Aggregator could not be a Chunk Part: it is the whole msg
	 * (non-Javadoc)
	 * @see org.msrpenabler.server.api.MsrpMessageData#isChunkPartMsg()
	 */
	@Override
	public boolean isChunkPartMsg() {
		return false;
	}
	

	@Override
	public boolean isComplete() {
		return isComplete;
	}

	public void setWaitingLength(int waitingLength) {
		this.waitingLength = waitingLength;
	}

	public int getWaitingLength() {
		return waitingLength;
	}

	
	/**
	 * Methods overiding the MsrpMessageData
	 */
	
	@Override
	public String getContentType() {
		
		return mapContentChunks.firstEntry().getValue().getContentType();
	}

	@Override
	public byte[] getContentByte() {

		byte[] content = null;
		if (completedMsg != null) {
			content = new byte[waitingLength];
			completedMsg.getBytes(0, content, 0, waitingLength);
		}
		return content;
	}


	@Override
	public int getContentLength() {

		return currentLength;
	}

	@Override
	public String toString() {
		
		if (getContentLength() != 0) {

			if (content().unwrap() != null) {
				return content().unwrap().toString(Charset.forName("UTF-8"));
			}
			else {
				return content().toString(Charset.forName("UTF-8"));
			}
		}
		return "";
	}

	
	public int getNbChunk() {
		return nbChunk;
	}

	public void incNbChunk() {
		this.nbChunk++;
	}
	public void decNbChunk() {
		this.nbChunk++;
	}

	public int getNbChunkAck() {
		return nbChunkAck;
	}

	public void incNbChunkAck() {
		this.nbChunkAck++;
	}
	public void decNbChunkAck() {
		this.nbChunkAck++;
	}

	public boolean isAutomaticRechunckMsg() {
		return isAutomaticRechunckMsg;
	}

	public void setAutomaticRechunckMsg(boolean isAutomaticRechunckMsg) {
		this.isAutomaticRechunckMsg = isAutomaticRechunckMsg;
	}


	@Override
	public MsrpChunkAggregator duplicate() {
		
		logger.info("call duplicate on messageId {}, isComplete: {}", messageId, isComplete);

		MsrpChunkAggregatorImpl newAgg = new MsrpChunkAggregatorImpl(messageId);
		
		Iterator<MsrpChunkData> it = iterator();
		
		try {
			while (it.hasNext()) {
					newAgg.addChunkedMsg( (MsrpChunkData) ((MsrpMessageData) it.next()).duplicate());
			}
		} catch (SessionHdException e) {
			logger.error("Failed on duplicate ChunkAggregatorMsg ",e);
			return null;
		}
		
		logger.info("end  of duplicate on messageId {}, isComplete: {}", messageId, newAgg.isComplete);

		return newAgg;
	}


	@Override
	public CompositeByteBuf content() {
		return completedMsg;
	}

	@Override
	public MsrpChunkAggregator copy() {
		logger.info("call duplicate on messageId {}, isComplete: {}", messageId, isComplete);

		MsrpChunkAggregatorImpl newAgg = new MsrpChunkAggregatorImpl(messageId);
		
		Iterator<MsrpChunkData> it = iterator();
		
		try {
			while (it.hasNext()) {
					newAgg.addChunkedMsg( (MsrpChunkData) ((MsrpMessageData) it.next()).copy());
			}
		} catch (SessionHdException e) {
			logger.error("Failed on copy ChunkAggregatorMsg ",e);
			return null;
		}
		
		logger.info("end  of duplicate on messageId {}, isComplete: {}", messageId, newAgg.isComplete);

		return newAgg;
	}


	@Override
	public MsrpChunkAggregator retain() {
		return retain(1);
	}

	@Override
	public MsrpChunkAggregator retain(int increment) {
		if (completedMsg != null) {
			completedMsg.retain(increment);
		}
		logger.debug("After call retain msgId {}, {}", getMessageId(), refCnt());

		Iterator<ByteBuf> it = completedMsg.iterator();
		int id=0;
		while ( it.hasNext() ) {
			ByteBuf c = it.next();
			logger.debug("After call retain {} on index {}", c.refCnt(), id);
			id++;
		}
		
		return this;
	}


	@Override
	public int refCnt() {
		if (completedMsg == null) return 0;
		return completedMsg.refCnt();
	}


	@Override
	public boolean release() {
		return release(1);
	}

	@Override
	public boolean release(int decrement) {
		boolean ret = false;
		if (completedMsg != null) {
			ret = completedMsg.release(decrement);
			
			Iterator<ByteBuf> it = completedMsg.iterator();
			int id=0;
			while ( it.hasNext() ) {
				ByteBuf c = it.next();
				logger.debug("After call release {} on chunck index {}", c.refCnt(), id);
				id++;
			}
			
			if (completedMsg.refCnt() == 0) {
				completedMsg = null;
				mapContentChunks = null;
			}
		}
		logger.debug("After call release msgId {}, count {}", getMessageId(), refCnt());

		return ret;
	}

	
}
