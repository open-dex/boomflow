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

public class TypedOrder extends StaticStruct implements TypedData {
	
	private static final String PRIMARY_TYPE = "Order";
	
	private static final List<Entry> SCHEMA = Arrays.asList(
			new Entry("userAddress", "address"),
			new Entry("quantity", "uint256"),
			new Entry("price", "uint256"),
			new Entry("orderType", "uint256"),
			new Entry("side", "int256"),
			new Entry("salt", "uint256"),
			new Entry("contractId", "uint256"),
			new Entry("positionEffect", "uint256"),
			new Entry("marginType", "uint256"),
			new Entry("marginRate", "uint256"),
			new Entry("posiId", "uint256"));
	
	private static final Map<String, List<Entry>> SCHEMAS = Collections.singletonMap(PRIMARY_TYPE, SCHEMA);
	
	public String userAddress;
	public BigInteger quantity;
	public BigInteger price;
	public long orderType;
	public long side;
	public long salt;
	public long contractId;
	public long positionEffect;
	public long marginType;
	public long marginRate;
	public long posiId;
	
	private Address signer;
	private String signature;
	
	public TypedOrder(Address userAddress, BigInteger quantity, BigInteger price, long orderType, long side, long salt, long contractId,
			long positionEffect, long marginType, long marginRate, long posiId, String signature) {
		super(userAddress.toABI(), new Uint256(quantity), new Uint256(price), new Uint256(orderType), new Int256(side),
				new Uint256(salt), new Uint256(contractId), new Uint256(positionEffect), new Uint256(marginType),
				new Uint256(marginRate), new Uint256(posiId));
		
		this.userAddress = userAddress.toHex();
		this.quantity = quantity;
		this.price = price;
		this.orderType = orderType;
		this.side = side;
		this.salt = salt;
		this.contractId = contractId;
		this.positionEffect = positionEffect;
		this.marginType = marginType;
		this.marginRate = marginRate;
		this.posiId = posiId;
		
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
