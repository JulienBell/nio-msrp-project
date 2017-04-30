package org.msrpenabler.server.util;

import java.util.Random;

public class GenerateIds {

	private static final Random rand = new Random();
	
//	public static byte[] generateId(byte[] tokenToUse, int minLength, int maxLength) {
//
//						
//		byte[] result; 
//		
//		if (maxLength < 1 || minLength < 1 || maxLength < minLength ) {
//			throw new IllegalArgumentException("Invalid length values min: "+minLength+", max: "+maxLength);
//		}
//		if ( tokenToUse == null || tokenToUse.length <= 1) {
//			throw new IllegalArgumentException("Invalid token List byte array should be greater than 1");
//		}
//		
//		int rLgth;
//		if (maxLength == minLength) {
//			rLgth = maxLength;
//		}
//		else {
//			rLgth = rand.nextInt(maxLength-minLength+1)+minLength;;
//		}
//		result=new byte[rLgth];
//		
//		int tokLen = tokenToUse.length;
//		
//		while (rLgth >= 1) {
//			result[rLgth-1] = tokenToUse[rand.nextInt(tokLen)];
//			rLgth--;
//		}
//		
//		return result;
//		
//	}
	
	public static String generateId(String tokenToUse, int minLength, int maxLength) {

		
		String result; 
		
		if (maxLength < 1 || minLength < 1 || maxLength < minLength ) {
			throw new IllegalArgumentException("Invalid length values min: "+minLength+", max: "+maxLength);
		}
		if ( tokenToUse == null || tokenToUse.length() <= 1) {
			throw new IllegalArgumentException("Invalid token List byte array should be greater than 1");
		}
		
		int rLgth;
		if (maxLength == minLength) {
			rLgth = maxLength;
		}
		else {
			rLgth = rand.nextInt(maxLength-minLength+1)+minLength;;
		}
		result=new String();
		
		int tokLen = tokenToUse.length();
		
		for (int i=0; i < rLgth; i++) {
			result += tokenToUse.charAt(rand.nextInt(tokLen));
		}
		
		return result;
		
	}
	
	public static String createTokenList(char startVal, char endVal) {

		String res = new String();
		
		for (char b = startVal; b<=endVal; b++) {
			res+=b;
		}
		return res;
	}

	
	public static String createMessageId() {
		
		//	 ident = ALPHANUM  3*31ident-char
		//	 ident-char = ALPHANUM / "." / "-" / "+" / "%" / "="
		String token = GenerateIds.createTokenList('A', 'Z');
		
		token+= GenerateIds.createTokenList('a','z');
		token+= GenerateIds.createTokenList('0','9');
		
		// Avoid to use these parameters that are leading to parsing errors in URL encoding
		//token+="+=-%";

		String msgId = generateId(token, 6, 10);
		
		return msgId;
	}

	
}
