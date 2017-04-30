package org.msrpenabler.mculib.cnf;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.msrpenabler.server.util.GenerateIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;

/**
 * 
 * @author Julien Bellanger
 *
 */
public class ConferencesFactory {

	private static final Logger logger = LoggerFactory.getLogger(ConferencesFactory.class);
	
	private Map<String, ConferenceUnit> mapConf = new ConcurrentHashMap<String, ConferenceUnit>();
	
	private final static MetricRegistry metricsFactory = new MetricRegistry();
	
	private static Counter nbConf = metricsFactory.counter(MetricRegistry.name(ConferencesFactory.class, "nbConfMcu"));
	
	final JmxReporter reporter;

	private ConferencesFactory() {
		reporter = JmxReporter.forRegistry(metricsFactory).build();
		reporter.start();
	}

	
	private static class SingletonLoader {
		private final static ConferencesFactory defaultInstance = new ConferencesFactory();
	}

	public static ConferencesFactory getDefaultInstance() {
		return SingletonLoader.defaultInstance;
	}
	
	public ConferenceUnit createConference() {
		
		ConferenceUnit conf = new ConferenceUnit();
		createId(conf);
		
		return conf;
	}

	private String createId(ConferenceUnit conf) {
		// Has to be thread safe between genSessionId 

		String tokenToUse = GenerateIds.createTokenList('A', 'Z');

		tokenToUse+= GenerateIds.createTokenList('a','z');
		tokenToUse+= GenerateIds.createTokenList('0','9');
		// I avoid to include char / and ~ as is too ugly to read network trace
		tokenToUse+="+-";

		String confId;
		boolean toRegenerate;

		do {
			confId = GenerateIds.generateId(tokenToUse, 6, 6);

			// Non thread safe operations...
			toRegenerate = mapConf.containsKey(confId);
			if (!toRegenerate) {
				ConferenceUnit previousConf = mapConf.put(confId, conf);
				if (previousConf != null) {
					logger.warn(" Non thread safe operation detected: last conference {} has been overwrited by {}  ", previousConf, conf );
					logger.warn(" Non thread safe operation detected: we replace the overwirtten conference in the map {}  ", previousConf );
					ConferenceUnit lastConf = mapConf.put(confId, previousConf);
					if (lastConf != conf) {
						// Very unlucky !!! Should never happen
						logger.error(" Unresolvable thread unsafe operation detected: last conference {} has been overwrite twice by {} ", previousConf, lastConf );
						logger.error(" Unresolvable thread unsafe operation detected: the conference has been lost {} ", lastConf );
					}
					toRegenerate = true;
				}
				else {
					// OK this is really a new conf in the map
					nbConf.inc();
				}
			}
			
		} while (toRegenerate);

		conf.setConfId(confId);

		return confId;
	}

	public ConferenceUnit unrefConfMsrp(ConferenceUnit conf) {

		
		ConferenceUnit delConf = mapConf.remove(conf.getConfId());
		
		if (delConf != null) {
			nbConf.dec();
		}
		
		return delConf;
	}

	/**
	 * 
	 * @param sessconfId
	 * @return
	 */
	public ConferenceUnit getMsrpConfById(String confId) {
		
		return mapConf.get(confId);
	}

	public static long getCurrentConferenceNumber() {
		return nbConf.getCount();
	}
	
}
