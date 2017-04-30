/**
 * 
 */
package org.msrpenabler.mcu.start;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.enabler.restlib.RestHttpException;
import org.msrpenabler.mcu.restsrv.McuHttpServlet;
import org.msrpenabler.mcu.restsrv.SwMSRPCompliantHttpServlet;
import org.msrpenabler.server.api.MsrpSessionsFactory;
import org.msrpenabler.server.cnx.MsrpAddrServer;
import org.rest.stub.sply.server.SplyRestSrvFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * @author Julien Bellanger
 *
 */
public class McuStart {

	private static final Logger logger = LoggerFactory.getLogger(McuStart.class);

	private static SplyRestSrvFactory srvHttpFactory = SplyRestSrvFactory.getInstance();
	private static MsrpSessionsFactory srvMsrpFactory = MsrpSessionsFactory.getDefaultInstance();

	

	/**
	 * @param args
	 */
	public static void main(String[] args) {

        System.out.format("McuStart - logback property %s%n", 
        		System.getProperty("logback.configurationFile") );
		
        String cp = System.getProperty("java.class.path");

        System.out.format("McuStart - Classpath property %s%n", cp);

        System.out.format("McuStart - current dir %s%n", System.getProperty("user.dir") );
        
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        
//        try {
          JoranConfigurator configurator = new JoranConfigurator();
          configurator.setContext(context);
          
          Map<String, String> mapProps = context.getCopyOfPropertyMap();
          
          for (Entry<String, String> entry : mapProps.entrySet()) {
        	  System.out.format("McuStart - property %s=%s%n",
        			  entry.getKey(), entry.getValue());
          }
          
          // Call context.reset() to clear any previous configuration, e.g. default 
          // configuration. For multi-step configuration, omit calling context.reset().
//          context.reset(); 
//          configurator.doConfigure(args[0]);
//        } catch (JoranException je) {
//          // StatusPrinter will handle this
//        }
          
        StatusPrinter.print(context);
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
        
		
		// TODO Load configuration properties from file passed as arg or default properties if undef
		String filePath;
		if (args.length < 2) {
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
		
//		Map<String, String> env = System.getenv();
//        for (String envName : env.keySet()) {
//        	logger.debug("env {}={}", envName,  env.get(envName));
//        }
        
        // Start an RMI registry on config port .
        //
        Integer rmiPort = config.getInteger("rmi.port", 3000);
        logger.info("Create RMI registry on port {}", rmiPort);
        try {
			LocateRegistry.createRegistry(3000);
		} catch (RemoteException e2) {
			logger.error("Exception on RMI register on port {}", rmiPort, e2);
		}

        
		// JMX Monitor MBean
		McuMonitor monitor = new McuMonitor();
		
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer(); 
        ObjectName name;
		try {
			name = new ObjectName("org.msrpenabler.mcu.start:type=McuMonitor");

			mbs.registerMBean(monitor, name);
		} catch (MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException
				| NotCompliantMBeanException e1) {

			logger.error("Failed to register JMX Monitor ", e1);;
		} 
        

        // Create an RMI connector server.
        //
        // As specified in the JMXServiceURL the RMIServer stub will be
        // registered in the RMI registry running in the local host on
        // port 3000 with the name "jmxrmi". This is the same name the
        // out-of-the-box management agent uses to register the RMIServer
        // stub too.
        //
        logger.info("Create an RMI connector server");
        JMXServiceURL url;
        JMXConnectorServer cs=null;
		try {
			url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://:"+rmiPort+"/jmxrmi");
			cs = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);

			//	Start the RMI connector server.
			logger.info("Start the RMI connector server");
			cs.start();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			logger.error("Start the RMI connector server ", e1);
		}
 		
		
		logger.debug("McuStart - Classpath property {}", cp);
        
		/**
		 *  Bind MSRP servers on configured ip port
		 */
		Integer nbServ = config.getInteger("msrp.nbSrvBind", 1);
		
		String host;
		int port;

		Configuration subconfig;
		MsrpAddrServer addrServ = null;
		
		logger.warn("McuStart - nb msrp bind {}", nbServ);

		for (Integer i=1; i<=nbServ; i++) {

			subconfig = config.subset("msrp."+i);

			host = subconfig.getString("host");
			port = Integer.valueOf(subconfig.getString("port"));

			
			try {
				addrServ = new MsrpAddrServer("msrp", host, port);

				logger.warn("McuStart - start {}", subconfig);
				srvMsrpFactory.startServer(addrServ);

				logger.debug("After McuStart - start {}", subconfig);
				
			} catch (Exception e) {
				logger.error("Failed on initialized addr server Msrp {}:", addrServ, e);
			}
		}

		
		/**
		 *  Bind HTTP server on configured ip port
		 */
		logger.debug("McuStart - start http interface command");

		String httpMCUCmdURI = "http://"+config.getString("http.host")
								+":"+config.getString("http.port");
		
		McuHttpServlet servlet = new McuHttpServlet();
		
		logger.warn("Start http notif Server on {}", httpMCUCmdURI);
		try {
			srvHttpFactory.getSrvBindConnector(httpMCUCmdURI, servlet);
		} catch (RestHttpException e) {
			logger.error("Failed on bind on URI {}:", httpMCUCmdURI, e);
		}
		

		logger.debug("McuStart - start http  Switch MSRP compliant interface command");

		String httpSwMSRPCmdURI = "http://"+config.getString("http.sw-old.host")
								+":"+config.getString("http.sw-old.port");
		
		SwMSRPCompliantHttpServlet servletSwMSRP = new SwMSRPCompliantHttpServlet();
		
		logger.warn("Start http notif Server on {}", httpSwMSRPCmdURI);
		try {
			srvHttpFactory.getSrvBindConnector(httpSwMSRPCmdURI, servletSwMSRP);
		} catch (RestHttpException e) {
			logger.error("Failed on bind on URI {}:", httpSwMSRPCmdURI, e);
		}
		
		
		
		/**
		 *  Wait Interupt signal
		 */
		logger.debug("McuStart - wait ");


		try {
			synchronized (monitor.lockStop) {
				monitor.lockStop.wait();
			}		
		} catch (InterruptedException e) {
			logger.error("End of process", e);
		}
				
		// Stop Gracefully all servers
		logger.warn("End on addr server Msrp...");
		srvMsrpFactory.cnxFactory.shutdownAllServer();
		
		srvHttpFactory.shutdown();

		logger.warn("Bye !");
		System.exit(0);
		
	}


}
