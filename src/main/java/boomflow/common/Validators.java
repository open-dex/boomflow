package boomflow.common;

import java.math.BigInteger;

public class Validators {
	
	private static final BigInteger SIG_MAX_S = new BigInteger("7FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF5D576E7357A4501DDFE92F46681B20A0", 16);
	
	public static void validateHexWithPrefix(String prefixedHex) throws ValidationException {
		if (prefixedHex == null || prefixedHex.length() < 2) {
			throw new ValidationException("prefix 0x missed");
		}
		
		if (prefixedHex.charAt(0) != '0' || prefixedHex.charAt(1) != 'x') {
			throw new ValidationException("prefix 0x missed");
		}
		
		if (prefixedHex.length() % 2 == 1) {
			throw new ValidationException("length of HEX should be even");
		}
		
		for (int i = 2, len = prefixedHex.length(); i < len; i++) {
			char ch = prefixedHex.charAt(i);
			if (ch < '0' || (ch > '9' && ch < 'A') || (ch > 'Z' && ch < 'a') || ch > 'z') {
				throw new ValidationException("invalid HEX character");
			}
		}
	}
	
	public static void validateSignature(String signature) throws ValidationException {
		if (signature == null || signature.isEmpty()) {
			throw new ValidationException("signature not specified");
		}
		
		if (signature.length() != 132) {
			throw new ValidationException("invalid signature length, expect 132 with 0x prefix in HEX format");
		}
		
		validateHexWithPrefix(signature);
		
		// V is 27 (1B) or 28 (1C)
		if (signature.charAt(130) != '1') {
			throw new ValidationException("invalid V in signature");
		}
		
		switch (signature.charAt(131)) {
		case 'b':
		case 'B':
		case 'C':
		case 'c':
			break;
		default:
			throw new ValidationException("invalid V in signature");
		}
		
		String s = signature.substring(66, 130);
		if (new BigInteger(s, 16).compareTo(SIG_MAX_S) > 0) {
			throw new ValidationException("invalid S in signature");
		}
	}

}
