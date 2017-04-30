package org.msrpenabler.server.api;

import io.netty.buffer.ByteBuf;

import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.msrpenabler.server.api.internal.MsrpChunkDataImpl;
import org.msrpenabler.server.api.internal.MsrpMessageDataImpl;
import org.msrpenabler.server.cnx.MsrpAddrServer;
import org.msrpenabler.server.cnx.MsrpConnexionsFactory;
import org.msrpenabler.server.exception.SessionHdException;
import org.msrpenabler.server.util.GenerateIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;

public class MsrpSessionsFactory  {

    private static final Logger logger =
        LoggerFactory.getLogger(MsrpSessionsFactory.class);

	private static class SingletonLoader {
		private final static MsrpSessionsFactory defaultInstance = new MsrpSessionsFactory();
	}

	public static MsrpSessionsFactory getDefaultInstance() {
		return SingletonLoader.defaultInstance;
	}

	private final static MetricRegistry metricsSessionFactory = new MetricRegistry();
	
	private static Counter nbSession = metricsSessionFactory.counter(MetricRegistry.name(MsrpSessionsFactory.class, "nbSession"));
	

	final JmxReporter reporter;
	
	public final MsrpConnexionsFactory cnxFactory;
	
	private MsrpSessionsFactory() {

		MsrpConnexionsFactory fact;
		try {
			fact = new MsrpConnexionsFactory();
		} catch (Exception e) {
			logger.error("Failed to initialize MsrpConnexionsFactory ",e);
			fact = null;
		}
		cnxFactory = fact;
		
		

		metricsSessionFactory.register(MetricRegistry.name(MsrpSessionsFactory.class, "nbSession2"),
				new Gauge<Long>() {
			@Override
			public Long getValue() {
				return nbSession.getCount();
			}
		});

		reporter = JmxReporter.forRegistry(metricsSessionFactory).build();
		reporter.start();
	}

	
	private Map<String, MsrpSessionHd> mapSessions = new ConcurrentHashMap<String, MsrpSessionHd>(1000);
	
    public void startServer(MsrpAddrServer addrServ) throws Exception {
    	cnxFactory.startServer(addrServ);
    }

	// Has to be thread safe between genSessionId 
	String getNewMsrpSessionId(MsrpSessionHd msrpSess) {
		
		String tokenToUse = GenerateIds.createTokenList('A', 'Z');
		
		tokenToUse+= GenerateIds.createTokenList('a','z');
		tokenToUse+= GenerateIds.createTokenList('0','9');
		// I avoid to include char / and ~ as is too ugly to read network trace
		// Avoir to use these tokens as they are leading to parsing error in http URL decoding
		//tokenToUse+="+=-_";
		
		String sessId;
		boolean toRegenerate;

		do {
			sessId = GenerateIds.generateId(tokenToUse, 6, 8);
		
			// Thread safe op
			synchronized(mapSessions) {
				toRegenerate = mapSessions.containsKey(sessId);
				if (!toRegenerate) mapSessions.put(sessId, msrpSess);
				nbSession.inc();
			}
		} while (toRegenerate);
			
		return sessId;
	}

	/**
	 * 
	 * @param sessId
	 * @return
	 */
	public MsrpSessionHd getMsrpSessionById(String sessId) {
		
		return mapSessions.get(sessId);
	}
	
	public void unrefMsrpSession(MsrpSessionHd sessHd) {

		MsrpSessionHd delSess = mapSessions.remove(sessHd.getSessionIdNoChk());
		if (delSess != null) {
			// TODO - set unref is the del session object
			delSess.setAsUnref();
			nbSession.dec();
		}
		return;
	}

	// Simple MSRP sessions
	public MsrpSessionHd createSimpleSession(InetAddress inet, MsrpSessionListener sessListener, boolean reusable, int inactivityTO) throws URISyntaxException, UnknownHostException, SessionHdException {
		return new DefaultMsrpSession(this, inet, sessListener, reusable, inactivityTO);
	}

	public MsrpSessionHd createSimpleSession(String host, MsrpSessionListener sessListener, boolean reusable, int inactivityTO) throws URISyntaxException, UnknownHostException, SessionHdException {
		return new DefaultMsrpSession(this, host, sessListener, reusable, inactivityTO);
	}
	
	public MsrpSessionHd createSimpleSession(MsrpAddrServer addrSrv, MsrpSessionListener sessListener, boolean reusable, int inactivityTO) throws URISyntaxException, SessionHdException {
		return new DefaultMsrpSession(this, addrSrv, sessListener, reusable, inactivityTO);
	}

	// Supervised MSRP sessions
	public MsrpSessionHd createSession(MsrpAddrServer addrSrv,
			MsrpSessionListener sessListener,
			MsrpSessionListener supervListener, int inactivityTO,
			LockOptions lockOptions) throws URISyntaxException, UnknownHostException, SessionHdException {
		return new SupervisedMsrpSession(this, addrSrv, sessListener, supervListener, inactivityTO, lockOptions);
	}
	
	public MsrpSessionHd createSession(MsrpAddrServer addrSrv, int inactivityTO) throws URISyntaxException, UnknownHostException, SessionHdException {
		return new SupervisedMsrpSession(this, addrSrv, inactivityTO);
	}

	public MsrpSessionHd createSession(MsrpAddrServer addrSrv, MsrpSessionListener sessListener, MsrpSessionListener superListener, 
				int inactivityTO,
				boolean lockInput, boolean lockOutput,
				boolean discardInput, boolean discardOutput) throws URISyntaxException, UnknownHostException, SessionHdException {
		return new SupervisedMsrpSession(this, addrSrv, sessListener, superListener, inactivityTO, lockInput, lockOutput, discardInput, discardOutput);
	}
	
	// MsrpMessages
	public static MsrpMessageData createMsg(String contentType,	byte[] rawContent) {
		return (MsrpMessageData) new MsrpMessageDataImpl(contentType, rawContent);
	}

	public static MsrpMessageData createMsg(String contentType,	ByteBuf rawContent) {
		return (MsrpMessageData) new MsrpMessageDataImpl(contentType, rawContent);
	}

	public static MsrpChunkData createChunkMsg(String messageId, String contentType, ByteBuf rawContent, int startByteRange, int length, int endByteRange) {
		return (MsrpChunkData) new MsrpChunkDataImpl(messageId, contentType, rawContent, startByteRange, length, endByteRange);
	}

	/**
	 * Create a new Message-Id, its unicity is not ensured
	 * @return
	 */
	public static String createMessageId() {
		
		return GenerateIds.createMessageId();
	}

	/**
	 * @return
	 */
	public static long getCurrentSessionNumber() {

		return nbSession.getCount();
	}

	/**
	 * @return
	 */
	public static MetricRegistry getMetrics() {

		return metricsSessionFactory;
	}
	
}
