package org.msrpenabler.server.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.msrpenabler.server.exception.SessionHdException;

public class MsrpSyntaxRegexp {

	
    //	token = 1*(%x21 / %x23-27 / %x2A-2B / %x2D-2E
    //          / %x30-39 / %x41-5A / %x5E-7E)
    //          ; token is compared case-insensitive

    //private static final String token_car = "[\\x21|\\x23-\\x27|\\x2A-\\x2B|\\x2D-\\x2E|\\x30-\\x39|\\x41-\\x5A|\\x5E-\\x7E]+";

	//    
    //    private final String unreserved  = "[\\p{Alnum}-\\._~]";
    //    private final String pathSessionId = "[\\p{Alnum}-\\._~+=/]+";
    	
	//    MSRP-URI = msrp-scheme "://" authority
	//    	       ["/" session-id] ";" transport *( ";" URI-parameter)
	//    	                        ; authority as defined in RFC3986
    // 8 groups
    private static final String msrp_uri = "(msrps?)://((.*@)*([^@:]*)(:\\d*)?)(/([\\p{Alnum}-\\._~\\+=/&&[^;]]+))(;([\\p{Alnum}&&[^;]]+))*(;.*)*";
	
    private static final String req_line = "^MSRP (\\p{Alnum}{1}[\\p{Alnum}\\.-=%\\+]{3,31}) ([\\p{Upper}]+)$";
    private static final String resp_line = "^MSRP (\\p{Alnum}{1}[\\p{Alnum}\\.-=%\\+]{3,31}) (\\d{3})( .+)*$";
    
    // Path Headers
    private static final String to_path = "^To-Path: ("+msrp_uri+")( "+msrp_uri+")?$";
    private static final String from_path = "^From-Path: ("+msrp_uri+")( "+msrp_uri+")?$";
    
    // Headers
    //    Message-ID = "Message-ID:" SP ident
    //    Success-Report = "Success-Report:" SP ("yes" / "no" )
    //    Failure-Report = "Failure-Report:" SP ("yes" / "no" / "partial" )
    //    Byte-Range = "Byte-Range:" SP range-start "-" range-end "/" total
    //    	range-start = 1*DIGIT
    //    	range-end   = 1*DIGIT / "*"
    //    	total       = 1*DIGIT / "*"
    private static final String header_line = "(.*): (.*)$" ;

    private static final String content_type_hd = "^Content-Type: (.*)$" ;
    
    private static final String byte_range_hd = "^Byte-Range: (\\p{Digit})-([\\p{Digit}\\*]+)/([\\p{Digit}\\*]+)$" ;
    
    //   content-stuff = *(Other-Mime-header CRLF)
    //   Content-Type 2CRLF data CRLF
    private static final String message_id_hd = "^Message-ID: (.*)$" ;

    //	Content-Type = "Content-Type:" SP media-type
    //	media-type = type "/" subtype *( ";" gen-param )
    //	type = token
    //	subtype = token
    //
    //	gen-param = pname [ "=" pval ]
    //	pname = token
    //	pval  = token / quoted-string
    //

    // end-line
    private static final String end_line = "^-------(\\p{Alnum}{1}[\\p{Alnum}\\.-=%\\+]{3,31})([\\+\\$#])$" ;
    

    public static Pattern pattReqLine = Pattern.compile(req_line);
    public static Pattern pattRespLine = Pattern.compile(resp_line);
	
    public static Pattern patt_ToPath = Pattern.compile(to_path);
    public static Pattern patt_FromPath = Pattern.compile(from_path);

    public static Pattern patt_ByteRange = Pattern.compile(byte_range_hd);
	
    public static Pattern patt_ContentType = Pattern.compile(content_type_hd);
    public static Pattern patt_HeaderGen = Pattern.compile(header_line);
	
    public static Pattern patt_EndLine = Pattern.compile(end_line);

	public static Pattern patt_msrpUri = Pattern.compile(msrp_uri);
	
	public static Pattern patt_MsgId = Pattern.compile(message_id_hd);

	
	public static int getPortAddress(String path) throws SessionHdException {
		Matcher matchLine = MsrpSyntaxRegexp.patt_msrpUri.matcher(path);

		if (! matchLine.matches() ) {
			throw new SessionHdException("Path invalid msrp_uri syntax: " + path);
		}
		String portStr = matchLine.group(5);

		int port = new Integer(portStr.substring(1)).intValue();
		return port;
	}

	public static InetAddress getInetAddress(String path) throws UnknownHostException, SessionHdException {
		Matcher matchLine = MsrpSyntaxRegexp.patt_msrpUri.matcher(path);

		if (! matchLine.matches() ) {
			throw new SessionHdException("Path invalid msrp_uri syntax: " + path);
		}
		
		String host = matchLine.group(4);
		
		InetAddress inetAddr = InetAddress.getByName(host);
		return inetAddr;
	}
	
	public static String getSessionId(String path) throws SessionHdException {
		Matcher matchLine = MsrpSyntaxRegexp.patt_msrpUri.matcher(path);

		if (! matchLine.matches() ) {
			throw new SessionHdException("Path invalid msrp_uri syntax: " + path);
		}
		String sessionId = matchLine.group(6).substring(1);

		return sessionId;
	}
	
}
