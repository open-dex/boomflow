package boomflow.log;

import java.math.BigInteger;

import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.generated.Uint256;

import boomflow.common.Address;
import boomflow.common.Utils;
import conflux.web3j.response.Log;
import conflux.web3j.types.CfxAddress;

public class EventLogData {
	
	private static final TypeReference<org.web3j.abi.datatypes.Address> addressTypeReference =
			TypeReference.create(org.web3j.abi.datatypes.Address.class);
	private static final TypeReference<Uint256> uintTypeReference = TypeReference.create(Uint256.class);
	
	private Address contractAddress;
	private String txHash;
	private int networkId;
	
	protected EventLogData(Log log) {
		this.contractAddress = Address.createCfxAddress(log.getAddress().getAddress());
		this.txHash = log.getTransactionHash().orElse("");
		this.networkId = log.getAddress().getNetworkId();
	}
	
	protected EventLogData(org.web3j.protocol.core.methods.response.Log log) {
		this.contractAddress = Address.createEthAddress(log.getAddress());
		this.txHash = log.getTransactionHash();
	}
	
	public Address getContractAddress() {
		return contractAddress;
	}
	
	public String getTxHash() {
		return txHash;
	}
	
	protected static Address parseEthAddress(String encoded) {
		org.web3j.abi.datatypes.Address address = (org.web3j.abi.datatypes.Address) FunctionReturnDecoder.decodeIndexedValue(encoded, addressTypeReference);
		return Address.createEthAddress(address.getValue());
	}
	
	protected Address parseCfxAddress(String encoded) {
		String hexAddress = parseEthAddress(encoded).toHex();
		String base32Address = CfxAddress.encode(hexAddress, this.networkId);
		return Address.createCfxAddress(base32Address);
	}
	
	protected static BigInteger parseUint256(String encoded) {
		return ((Uint256) FunctionReturnDecoder.decodeIndexedValue(encoded, uintTypeReference)).getValue();
	}
	
	@Override
	public String toString() {
		return Utils.toJson(this);
	}
}
