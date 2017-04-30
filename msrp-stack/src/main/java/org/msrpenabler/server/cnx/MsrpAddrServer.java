package org.msrpenabler.server.cnx;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.msrpenabler.server.net.NioMsrpSockServerBootStrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MsrpAddrServer {

	private static final Logger logger =
	        LoggerFactory.getLogger(MsrpAddrServer.class);
	
	public InetAddress inetAddr;
	public int port;
	
	public NioMsrpSockServerBootStrap bootStrapServ;
	
    public Map<String, MsrpConnexion> mapBindCnx = new ConcurrentHashMap<String, MsrpConnexion>(100);

    private String localURI ;
    
	public MsrpAddrServer(String scheme, String host, int port) throws UnknownHostException {

		InetAddress inet;
		try {
			inet = InetAddress.getByName(host);
		} catch (UnknownHostException e) {
			logger.error("Failed to retrieve inet Addr for host: {}", host);
			throw e;
		}
		
		this.inetAddr = inet;
		this.port = port;
		
		localURI = scheme + "://" + inetAddr.getHostAddress() + ":" + Integer.toString(port);
	}

	public MsrpAddrServer(String scheme, InetAddress inetAddr, int port) {
		this.inetAddr = inetAddr;
		this.port = port;
		
		localURI = scheme + "://" + inetAddr.getHostAddress() + ":" + Integer.toString(port);
	}

	// TODO Add capabilities to work with a list of localURI ?? 
	public String getLocalURI() {
		return localURI;
	}
	
	boolean equals(MsrpAddrServer keyAddr)  {
		return ( localURI.equals(keyAddr.getLocalURI()));
	}

	int hashcode() {
		return localURI.hashCode();
	}
	
	
	public SocketAddress getLocalSockAddr() {
		
		return new InetSocketAddress(inetAddr, port);
	}
}
