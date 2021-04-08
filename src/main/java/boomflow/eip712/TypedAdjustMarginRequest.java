package boomflow.eip712;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.web3j.abi.datatypes.StaticStruct;
import org.web3j.abi.datatypes.generated.Int256;
import org.web3j.abi.datatypes.generated.Uint256;

import boomflow.common.Address;
import boomflow.eip712.core.Domain;
import boomflow.eip712.core.Entry;
import boomflow.eip712.core.TypedData;

public class TypedAdjustMarginRequest extends StaticStruct implements TypedData {
	
	private static final String PRIMARY_TYPE = "AdjustMarginRequest";
	
	private static final Map<String, List<Entry>> SCHEMAS = Collections.singletonMap(PRIMARY_TYPE, Arrays.asList(
			new Entry("userAddress", "address"),
			new Entry("contractId", "uint256"),
			new Entry("margin", "int256"),
			new Entry("posiSide", "int256"),
			new Entry("nonce", "uint256")));
	
	public String userAddress;
	public long contractId;
	public BigInteger margin;
	public long posiSide;
	public long nonce;
	
	private Address signer;
	private String signature;
	
	public TypedAdjustMarginRequest(Address userAddress, long contractId, BigInteger margin, long posiSide, long nonce, String signature) {
		super(userAddress.toABI(), new Uint256(contractId), new Int256(margin), new Int256(posiSide), new Uint256(nonce));
		
		this.userAddress = userAddress.toHex();
		this.contractId = contractId;
		this.margin = margin;
		this.posiSide = posiSide;
		this.nonce = nonce;
		
		this.signer = userAddress;
		this.signature = signature;
	}

	@Override
	public String primaryType() {
		return PRIMARY_TYPE;
	}

	@Override
	public Map<String, List<Entry>> schemas() {
		return SCHEMAS;
	}

	@Override
	public Domain domain() {
		return Domain.boomflow();
	}

	@Override
	public Address signer() {
		return this.signer;
	}

	@Override
	public String signature() {
		return this.signature;
	}

}
