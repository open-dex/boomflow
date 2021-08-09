package boomflow.eip712;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicStruct;
import org.web3j.abi.datatypes.generated.Uint256;

import boomflow.common.Address;
import boomflow.common.EncodeUtils;
import boomflow.eip712.core.Domain;
import boomflow.eip712.core.Entry;
import boomflow.eip712.core.TypedData;

public class TypedWithdrawUser extends DynamicStruct implements TypedData {
	
	static final String PRIMARY_TYPE = "WithdrawRequest";
	
	static final List<Entry> SCHEMA = Arrays.asList(
			new Entry("userAddress", "address"),
			new Entry("amount", "uint256"),
			new Entry("recipient", "address"),
			new Entry("burn", "bool"),
			new Entry("nonce", "uint256"));
	
	private static final Map<String, List<Entry>> SCHEMAS = Collections.singletonMap(PRIMARY_TYPE, SCHEMA);
	
	public String userAddress;
	public BigInteger amount;
	public String recipient;
	public boolean burn;
	public long nonce;
	
	private Address contractAddress;
	private Address signer;
	private String signature;
	
	public TypedWithdrawUser(Address userAddress, BigInteger amount, Address recipient, boolean burn, long nonce, Address contractAddress, String signature) {
		super(userAddress.toABI(), new Uint256(amount), recipient.toABI(), new Bool(burn), new Uint256(nonce), EncodeUtils.hex2Bytes(signature));
		
		this.userAddress = userAddress.toHex();
		this.amount = amount;
		this.recipient = recipient.toHex();
		this.burn = burn;
		this.nonce = nonce;
		
		this.contractAddress = contractAddress;
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
		return Domain.getCRCL(this.contractAddress);
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
