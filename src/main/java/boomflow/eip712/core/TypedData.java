package boomflow.eip712.core;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.web3j.crypto.StructuredDataEncoder;
import org.web3j.utils.Numeric;

import boomflow.common.SignUtils;

/**
 * TypedData is implemented by any EIP712 object.
 */
public interface TypedData {
	
	String primaryType();
	
	Map<String, List<Entry>> schemas();
	
	Domain domain();
	
	String signer();
	String signature();
	
	/**
	 * Returns the hash of this typed data.
	 */
	default byte[] hash() {
		Template template = new Template(this);
		
		try {
			return new StructuredDataEncoder(template.toJson()).hashStructuredData();
		} catch (IOException e) {
			throw new RuntimeException("Failed to create StructuredDataEncoder", e);
		}
	}
	
	/**
	 * Returns the hash of this typed data in HEX format.
	 */
	default String hashHex() {
		return Numeric.toHexString(this.hash());
	}
	
	/**
	 * Validates this typed data with the signer address and signature.
	 * @return typed data hash in HEX format.
	 */
	default String validate() {
		byte[] hash = this.hash();
		SignUtils.validate(this.signer(), this.signature(), hash, false);
		return Numeric.toHexString(hash);
	}

}
