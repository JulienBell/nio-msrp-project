package org.msrpenabler.server.cnx;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.msrpenabler.server.net.NioMsrpSockServerBootStrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MsrpConnexionsFactory {


	private static final Logger logger =
        LoggerFactory.getLogger(MsrpConnexionsFactory.class);
	
	private EventLoopGroup workerGroup;
	
    public EventLoopGroup getWorkerGroup() {
		return workerGroup;
	}


	private Map<String, MsrpAddrServer> serverList = new HashMap<String, MsrpAddrServer>();

	
	
    public MsrpConnexionsFactory() throws Exception  {
    	
    	workerGroup = new NioEventLoopGroup();
    	
	}

    
    /**
	 * Select on any server addr with default msrp scheme
	 * @return
	 * @throws UnknownHostException 
	 */
    static Integer last=0;

    public MsrpAddrServer selectLocalServer() throws UnknownHostException {

    	Set<String> srvSet = getServerList().keySet();
    	
        String[] servList = srvSet.toArray(new String[srvSet.size()]);
        
        int sz = servList.length;
        
        int id;
        
		synchronized(last) {
        	if (last >= sz) last = 0;
        	id = last;
        }
        
		return getServerList().get(servList[id]);
		
	}
    public String selectServerLocalURI() throws UnknownHostException {

    	return selectLocalServer().getLocalURI();
	}

	
	/**
	 * Select any server with default msrp scheme and same inetAddr
	 * @param inet
	 * @return
	 */
	public String selectServerLocalURI(InetAddress inet) {

		return selectServerLocalURI("msrp", inet);
	}
	
	/**
	 * Select on any server with default msrp scheme and same host addr
	 * @param host
	 * @return
	 * @throws UnknownHostException 
	 */
	public String selectServerLocalURI(String host) throws UnknownHostException {

		InetAddress inet;
		logger.debug("host : {}", host);
		try {
			inet = InetAddress.getByName(host);
		} catch (UnknownHostException e) {
			logger.error("Failed to retrieve inet Addr for host: {}", host);

			throw e;
		}
		logger.debug("inet : {}", inet);
		
		return selectServerLocalURI(inet); 
	}

	/**
	 * Select a server on a given scheme and inetAddr
	 * 
	 * @param scheme
	 * @param inet
	 * @return
	 */
	public String selectServerLocalURI(String scheme, InetAddress inet) {

        MsrpAddrServer mAddr;

        for (Entry<String, MsrpAddrServer> addrServerEntry : getServerList().entrySet()) {
            mAddr = addrServerEntry.getValue();
            if (mAddr.inetAddr.equals(inet)) {
                return mAddr.getLocalURI();
            }
        }

        return null;
	}

	/**
	 * Select a server on a given scheme and inetAddr
	 * 
	 * @param scheme
	 * @param inet
	 * @return
	 */
	public MsrpAddrServer selectServerLocalAddr(InetAddress inet) {

        MsrpAddrServer mAddr;

        for (Entry<String, MsrpAddrServer> addrServerEntry : getServerList().entrySet()) {
            mAddr = addrServerEntry.getValue();
            if (mAddr.inetAddr.equals(inet)) {
                return mAddr;
            }
        }

        return null;
	}
    
    	
    // start server on a given ip port addr
    public void startServer(MsrpAddrServer addrServ) throws Exception {
    	
    	addrServ.bootStrapServ = new NioMsrpSockServerBootStrap(addrServ);
    	
    	addrServ.bootStrapServ.run();
    	
    	getServerList().put(addrServ.getLocalURI(), addrServ);
    	
    }
    
    public void shutdownServer(MsrpAddrServer addrServ) {
    	try {
			addrServ.bootStrapServ.shutDown();
		} catch (IOException e) {
			logger.error("Failed to shutdown server: ", addrServ);
		}
    }
    
    public void shutdownAllServer() {

        for (Entry<String, MsrpAddrServer> addrServerEntry : getServerList().entrySet()) {
            shutdownServer(addrServerEntry.getValue());
        }
    	
    	// TODO - may the list be used later ?
    	getServerList().clear();
    }

	
	public Map<String, MsrpAddrServer> getServerList() {
		return serverList;
	}
	

}
