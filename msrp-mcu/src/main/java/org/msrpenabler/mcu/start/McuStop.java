/**
 * 
 */
package org.msrpenabler.mcu.start;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Julien Bellanger
 *
 */
public class McuStop {

	private static final Logger logger = LoggerFactory.getLogger(McuStop.class);

	public static void stop(String[] args) {

		// Load configuration properties from file passed as arg or default properties if undef
		String filePath;
		if (args == null || args.length < 2) {
			filePath = "mcu_default.properties";
		}
		else {
			filePath = args[1];
		}

		Configuration config;
		try {
			logger.warn("Load config file : {}", filePath);
			config = ConfigurationService.getInstance().loadConfigFile(filePath);
		} catch (ConfigurationException e) {
			logger.error("Failed to load configuration file : {}", filePath);
			logger.error("Starting MCU failure !! ");
			return;
		}
		
        // Get Rmi Port
        Integer rmiPort = config.getInteger("rmi.port", 3000);
        logger.info("RMI port {}", rmiPort);
        
		// 		
        String connectorAddress = "service:jmx:rmi:///jndi/rmi://:"+rmiPort+"/jmxrmi" ;
		System.out.printf("Connecting to jmx server with connectorAddress : %s%n",connectorAddress );
 
        // establish connection to connector server
        JMXServiceURL url;
		try {
			url = new JMXServiceURL(connectorAddress);
			JMXConnector connector = JMXConnectorFactory.connect(url);

			MBeanServerConnection mbsc = connector.getMBeanServerConnection();

			RuntimeMXBean runtime = ManagementFactory.newPlatformMXBeanProxy(
					mbsc, ManagementFactory.RUNTIME_MXBEAN_NAME, RuntimeMXBean.class);

			System.out.printf("Extracted getName : %s%n",runtime.getName());
 
		
			ObjectName mbeanName = new ObjectName("org.msrpenabler.mcu.start:type=McuMonitor");
			McuMonitorMBean mbeanProxy = JMX.newMBeanProxy(mbsc, mbeanName, McuMonitorMBean.class, true);
			
			// Call Stop on remote process
			logger.warn("Call stop on remote MsrpMcuServer {}", runtime.getName());
			mbeanProxy.stop();

			logger.warn("Stop McuServer has been called");
			
		} catch ( IOException | MalformedObjectNameException e) {
			// TODO Auto-generated catch block
			logger.error("Failed on get JMX connector",e);
		}

		
	}

	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

        System.out.format("McuStop - logback property %s%n", 
        		System.getProperty("logback.configurationFile") );

        // Call stop
        stop(args);
			
		logger.warn("Bye !");
		System.exit(0);
		
	}
	

}
