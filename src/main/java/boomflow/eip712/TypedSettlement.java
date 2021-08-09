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

class TypedSettlement extends StaticStruct implements TypedData {
	
	static final String PRIMARY_TYPE = "Settlement";
	
	static final List<Entry> SCHEMA = Arrays.asList(
			new Entry("opType", "uint256"),
			new Entry("deltaMatchAmt", "uint256"),
			new Entry("deltaMatchQty", "uint256"),
			new Entry("deltaMargin", "int256"),
			new Entry("totalMargin", "uint256"),
			new Entry("deltaAmount", "int256"),
			new Entry("fee", "uint256"),
			new Entry("gasFeeToken", "address"),
			new Entry("gasFee", "uint256"));
	
	private static final Map<String, List<Entry>> SCHEMAS = Collections.singletonMap(PRIMARY_TYPE, SCHEMA);
	
	public long opType;
	public BigInteger deltaMatchAmt;
	public BigInteger deltaMatchQty;
	public BigInteger deltaMargin;
	public BigInteger totalMargin;
	public BigInteger deltaAmount;
	public BigInteger fee;
	public String gasFeeToken;
	public BigInteger gasFee;
	
	public TypedSettlement(long opType,
			BigInteger deltaMatchAmt, BigInteger deltaMatchQty,
			BigInteger deltaMargin, BigInteger totalMargin,
			BigInteger deltaAmount,
			BigInteger fee, Address gasFeeToken, BigInteger gasFee) {
		super(new Uint256(opType), 
				new Uint256(deltaMatchAmt), new Uint256(deltaMatchQty),
				new Int256(deltaMargin), new Uint256(totalMargin),
				new Int256(deltaAmount),
				new Uint256(fee), gasFeeToken.toABI(), new Uint256(gasFee));
		
		this.opType = opType;
		this.deltaMatchAmt = deltaMatchAmt;
		this.deltaMatchQty = deltaMatchQty;
		this.deltaMargin = deltaMargin;
		this.totalMargin = totalMargin;
		this.deltaAmount = deltaAmount;
		this.fee = fee;
		this.gasFeeToken = gasFeeToken.toHex();
		this.gasFee = gasFee;
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
		throw new RuntimeException("unsupported to validate signature");
	}

	@Override
	public String signature() {
		throw new RuntimeException("unsupported to validate signature");
	}

}
