package boomflow.eip712;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.web3j.abi.datatypes.DynamicStruct;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;

import boomflow.common.Address;
import boomflow.common.EncodeUtils;
import boomflow.eip712.core.Domain;
import boomflow.eip712.core.Entry;
import boomflow.eip712.core.TypedData;

public class TypedPosition extends DynamicStruct implements TypedData {
	
	private static final String PRIMARY_TYPE = "Position";
	
	private static final Map<String, List<Entry>> SCHEMAS = new HashMap<String, List<Entry>>();
	
	static {
		SCHEMAS.put(PRIMARY_TYPE, Arrays.asList(
				new Entry("nonce", "uint256"),
				new Entry("userAddress", "address"),
				new Entry("clearAccountAddress", "address"),
				new Entry("tokenAddress", "address"),
				new Entry("settlement", TypedSettlement.PRIMARY_TYPE),
				new Entry("symbol", "string")));
		SCHEMAS.put(TypedSettlement.PRIMARY_TYPE, TypedSettlement.SCHEMA);
	}
	
	public long nonce;
	public String userAddress;
	public String clearAccountAddress;
	public String tokenAddress;
	public TypedSettlement settlement;
	public String symbol;
	
	private Address signer;
	private String signature;
	
	public TypedPosition(long nonce, long opType, 
			Address userAddress, Address clearAccountAddress, Address tokenAddress,
			BigInteger deltaMatchAmt, BigInteger deltaMatchQty,
			BigInteger deltaMargin, BigInteger totalMargin,
			BigInteger deltaAmount, String symbol,
			BigInteger fee, Address gasFeeToken, BigInteger gasFee, Address signer, String signature) {
		super(new Uint256(nonce),
				userAddress.toABI(), clearAccountAddress.toABI(), tokenAddress.toABI(),
				new TypedSettlement(opType, deltaMatchAmt, deltaMatchQty, deltaMargin, totalMargin, deltaAmount, fee, gasFeeToken, gasFee),
				new Utf8String(symbol), signer.toABI(), EncodeUtils.hex2Bytes(signature));
		
		this.nonce = nonce;
		this.userAddress = userAddress.toHex();
		this.clearAccountAddress = clearAccountAddress.toHex();
		this.tokenAddress = tokenAddress.toHex();
		this.settlement = new TypedSettlement(opType, deltaMatchAmt, deltaMatchQty, deltaMargin, totalMargin, deltaAmount, fee, gasFeeToken, gasFee);
		this.symbol = symbol;
		
		this.signer = signer;
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
