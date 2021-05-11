package boomflow.common;

import java.math.BigInteger;

import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import conflux.web3j.types.AddressException;
import conflux.web3j.types.AddressType;
import conflux.web3j.types.CfxAddress;

public interface Address {
	
	String toHex();
	
	org.web3j.abi.datatypes.Address toABI();
	
	boolean equals(BigInteger pubKey);
	
	static Address createCfxAddress(String base32Address) throws AddressException {
		return new ConfluxAddress(base32Address);
	}
	
	static Address createEthAddress(String hex40Address) {
		return new EthAddress(hex40Address);
	}

}

class ConfluxAddress implements Address {
	
	private CfxAddress address;
	
	public ConfluxAddress(String base32Address) throws AddressException {
		this.address = new CfxAddress(base32Address);
	}
	
	@Override
	public String toHex() {
		return this.address.getHexAddress();
	}
	
	@Override
	public org.web3j.abi.datatypes.Address toABI() {
		return this.address.getABIAddress();
	}

	@Override
	public boolean equals(BigInteger pubKey) {
		String recoveredAddress = AddressType.User.normalize(Keys.getAddress(pubKey));
		return this.address.getHexAddress().equalsIgnoreCase(recoveredAddress);
	}
	
	@Override
	public String toString() {
		return this.address.getAddress();
	}
	
}

class EthAddress implements Address {
	
	private String address;
	
	public EthAddress(String hex40Address) {
		this.address = hex40Address;
	}
	
	@Override
	public String toHex() {
		return this.address;
	}
	
	@Override
	public org.web3j.abi.datatypes.Address toABI() {
		return new org.web3j.abi.datatypes.Address(this.address);
	}

	@Override
	public boolean equals(BigInteger pubKey) {
		String recoveredAddress = Keys.getAddress(pubKey);
		recoveredAddress = Numeric.prependHexPrefix(recoveredAddress);
		return this.address.equalsIgnoreCase(recoveredAddress);
	}

	@Override
	public String toString() {
		return this.address;
	}
	
}
