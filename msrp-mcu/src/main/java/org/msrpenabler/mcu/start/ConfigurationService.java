/**
 * 
 */
package org.msrpenabler.mcu.start;

import java.util.Iterator;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Julien Bellanger
 *
 */
public class ConfigurationService {

	private static final Logger logger = LoggerFactory.getLogger(ConfigurationService.class);
	
	private Configuration config = null;

	
	private static class ChargeurService {
		private static final ConfigurationService instance = new ConfigurationService();
	}

	public static ConfigurationService getInstance() {
		return ChargeurService.instance;
	}

	private ConfigurationService() {
		
	}
	
	
	public Configuration loadConfigFile(String filepath) throws ConfigurationException {

		logger.debug("loadConfigFile() filepath = "+filepath);

		config = new PropertiesConfiguration(filepath);

		logger.debug("loadConfigFile() config = "+config);

		if (logger.isDebugEnabled()) {
			// Print out the configuration parameters
			Iterator<?> en = config.getKeys();

			logger.debug("********** System configuration **********");
			while (en.hasNext()) {
				String key = (String) en.next();
				logger.debug(key + " => " + config.getString(key));
			}
		}
		return config;
	}
	
}
