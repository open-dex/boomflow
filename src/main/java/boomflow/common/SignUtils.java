package boomflow.common;

import java.math.BigInteger;
import java.util.Arrays;

import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.utils.Numeric;

import conflux.web3j.types.Address;
import conflux.web3j.types.AddressException;
import conflux.web3j.types.AddressType;

/**
 * Utilities to validate signature.
 */
public class SignUtils {
	
	public static void validate(String signerAddress, String sigHex, byte[] message) throws AddressException, ValidationException {
		validate(signerAddress, sigHex, message, true);
	}
	
	public static void validate(String signerAddress, String sigHex, byte[] message, boolean needToHash) throws AddressException, ValidationException {
		signerAddress = Address.normalizeHexAddress(signerAddress);
		AddressType.validateHexAddress(signerAddress, AddressType.User);
		
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
		
		String recoveredAddress = AddressType.User.normalize(Keys.getAddress(pubkey));
		if (!signerAddress.equalsIgnoreCase(recoveredAddress)) {
			throw new ValidationException("invalid signature");
		}
	}

}
