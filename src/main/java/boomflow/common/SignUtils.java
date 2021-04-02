package boomflow.common;

import java.math.BigInteger;
import java.util.Arrays;

import org.web3j.crypto.Sign;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.utils.Numeric;

/**
 * Utilities to validate signature.
 */
public class SignUtils {
	
	public static void validate(Address signerAddress, String sigHex, byte[] message) throws ValidationException {
		validate(signerAddress, sigHex, message, true);
	}
	
	public static void validate(Address signerAddress, String sigHex, byte[] message, boolean needToHash) throws ValidationException {
		Validators.validateSignature(sigHex);
		
		byte[] sigBytes = Numeric.hexStringToByteArray(sigHex);
		SignatureData signatureData = new SignatureData(
				sigBytes[64],							// V
				Arrays.copyOfRange(sigBytes, 0, 32),	// R
				Arrays.copyOfRange(sigBytes, 32, 64));	// S
		
		BigInteger pubkey;
		try {
			pubkey = needToHash
					? Sign.signedMessageToKey(message, signatureData)
					: Sign.signedMessageHashToKey(message, signatureData);
		} catch (Exception e) {
			throw new ValidationException("invalid signature");
		}
		
		if (!signerAddress.equals(pubkey)) {
			throw new ValidationException("invalid signature");
		}
	}

}
