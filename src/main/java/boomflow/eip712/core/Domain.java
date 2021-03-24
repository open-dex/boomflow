package boomflow.eip712.core;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.fasterxml.jackson.annotation.JsonIgnore;

import boomflow.common.Utils;
import conflux.web3j.types.CfxAddress;

public class Domain {
	
	static final String PRIMARY_TYPE = "EIP712Domain";
	
	public static String defaultName = "CRCL";
	public static String defaultVersion = "1.0";
	public static long defaultChainId;
	public static CfxAddress boomflowAddress;
	public static String boomflowName = "Boomflow";
	
	private static ConcurrentMap<String, Domain> domains = new ConcurrentHashMap<String, Domain>();
	
	static final List<Entry> SCHEMA = Arrays.asList(
			new Entry("name", "string"),
			new Entry("version", "string"),
			new Entry("chainId", "uint256"),
			new Entry("verifyingContract", "address"));
	
	private String name;
	private String version;
	private long chainId;
	private CfxAddress verifyingContractAddress;
	
	public Domain(String name, CfxAddress contract) {
		this(name, defaultVersion, defaultChainId, contract);
	}

	public Domain(String name, String version, long chainId, CfxAddress contract) {
		this.name = name;
		this.version = version;
		this.chainId = chainId;
		this.verifyingContractAddress = contract;
	}
	
	public String getName() {
		return name;
	}
	
	public String getVersion() {
		return version;
	}
	
	public long getChainId() {
		return chainId;
	}
	
	public String getVerifyingContract() {
		return this.verifyingContractAddress.getHexAddress();
	}
	
	@JsonIgnore
	public CfxAddress getVerifyingContractAddress() {
		return verifyingContractAddress;
	}
	
	public static Domain boomflow() {
		if (boomflowAddress == null) {
			throw new NullPointerException("boomflow address not initialized");
		}
		
		return domains.computeIfAbsent("Boomflow", bf -> {
			return new Domain(boomflowName, boomflowAddress);
		});
	}
	
	public static Domain getCRCL(CfxAddress crclAddress) {
		return domains.computeIfAbsent(crclAddress.getAddress(), address -> {
			return new Domain(defaultName, crclAddress);
		});
	}
	
	@Override
	public String toString() {
		return Utils.toJson(this);
	}

}
