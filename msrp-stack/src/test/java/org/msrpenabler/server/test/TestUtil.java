package org.msrpenabler.server.test;

import io.netty.handler.codec.http.QueryStringEncoder;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.msrpenabler.server.util.GenerateIds;
import org.msrpenabler.server.util.MsrpSyntaxRegexp;

import junit.framework.TestCase;

public class TestUtil extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}


	public void testRegexp() throws Exception {

		
		
		QueryStringEncoder uriEncoded = new QueryStringEncoder("/msrp/updatesess/?toto=rr&sess=msrp://10.26.9.23:2855/z+XEW6S;tcp;param=3&titi&tata=rt");
		System.out.println("encoded Uri :" + uriEncoded.toString());

		String url = " http://FakeHostForSwCompliance"+uriEncoded.toString();
		URL urlPath;
		String[] queryParams = null;
		try {
			urlPath = new URL(url);
			queryParams = urlPath.getQuery().split("&");
			
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
			queryParams = new String[0];
		}
		
		Map<String, List<String>> params = new HashMap<String, List<String>>();
		for (String param: queryParams) {
			String[] keyValue = param.split("=");
			List<String> list = new ArrayList<String>();
			
			if (keyValue.length > 1) {
				list.add(keyValue[1]);
			}
			params.put(keyValue[0], list);
		}
		
		
		String sess = params.get("sess").get(0);
		System.out.println("param sess: " + sess);
		
		String sessId = null;
		try {
	
//			sessId = MsrpSyntaxRegexp.getSessionId("msrp://10.26.9.23:2855/zXEW6S");
//			sessId = MsrpSyntaxRegexp.getSessionId("msrp://10.26.9.23:2855/zXEW6S;tcp");
			//sessId = MsrpSyntaxRegexp.getSessionId("msrp://10.26.9.23:2855/z XEW6S;tcp;param=3");
			sessId = MsrpSyntaxRegexp.getSessionId(sess);
		}
		catch (Exception e) {
			System.out.println("failed to get sessId :" + "msrp://10.26.9.23:2855/zXEW6S");
			throw e;
		}
		System.out.println("sessId : " + sessId);
		
		
		
		Matcher ma = MsrpSyntaxRegexp.pattRespLine.matcher("MSRP dkei38gd 200 OK");
		
		System.out.println("matche Resp Line: "+ma.matches());
		assert ma.matches();
		
		System.out.println("Group 1 "+ma.group(1));
		System.out.println("Group 2 "+ma.group(2));
		System.out.println("Group 3 "+ma.group(3));
		
		ma = MsrpSyntaxRegexp.patt_ByteRange.matcher("Byte-Range: 8-9/16");

		System.out.println("matche Byte Range: "+ma.matches());

		if (ma != null && ma.matches()) {
			System.out.println("Group 1 "+ma.group(1));
			System.out.println("Group 2 "+ma.group(2));
			System.out.println("Group 3 "+ma.group(3));
		}

	}	
	
	public void testGenerateId() throws Exception {

		//session-id: [{Alnum}-._~+=/]
		// ALPHA: %x41-5A / %x61-7A
		// NUM: %x30-39

		String str = GenerateIds.createTokenList('A', 'Z');
		
		str+= GenerateIds.createTokenList('a','z');
		str+= GenerateIds.createTokenList('0','9');
		str+="/+=-_~";

		System.out.println("tokenList : " + str );

		String resStr = GenerateIds.generateId(str, 1, 8);
		
		System.out.println("result Id : " + resStr );
		
		resStr = GenerateIds.generateId(str, 2, 10);
		
		System.out.println("result Id : " + resStr );
		
		
		
	}	
	
}
