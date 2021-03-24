package boomflow.log;

import java.math.BigInteger;

import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;

import boomflow.common.Utils;
import conflux.web3j.response.Log;
import conflux.web3j.types.CfxAddress;

public class EventLogData {
	private CfxAddress contractAddress;
	private String txHash;
	
	protected EventLogData(Log log) {
		this.contractAddress = new CfxAddress(log.getAddress().getAddress());
		this.txHash = log.getTransactionHash().orElse("");
	}
	
	public CfxAddress getContractAddress() {
		return contractAddress;
	}
	
	public String getTxHash() {
		return txHash;
	}
	
	protected static String parseHexAddress(String encoded) {
		return ((Address) FunctionReturnDecoder.decodeIndexedValue(encoded, TypeReference.create(Address.class))).getValue();
	}
	
	protected CfxAddress parseCfxAddress(String encoded) {
		return new CfxAddress(parseHexAddress(encoded), this.contractAddress.getNetworkId());
	}
	
	protected static BigInteger parseUint256(String encoded) {
		return ((Uint256) FunctionReturnDecoder.decodeIndexedValue(encoded, TypeReference.create(Uint256.class))).getValue();
	}
	
	@Override
	public String toString() {
		return Utils.toJson(this);
	}
}
