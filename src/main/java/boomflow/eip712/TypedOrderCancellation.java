package boomflow.eip712;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.web3j.abi.datatypes.StaticStruct;
import org.web3j.abi.datatypes.generated.Uint256;

import boomflow.eip712.core.Domain;
import boomflow.eip712.core.Entry;
import boomflow.eip712.core.TypedData;
import conflux.web3j.types.CfxAddress;

public class TypedOrderCancellation extends StaticStruct implements TypedData {
	
	private static final String PRIMARY_TYPE = "CancelRequest";
	
	private static final Map<String, List<Entry>> SCHEMAS = Collections.singletonMap(PRIMARY_TYPE, Arrays.asList(
			new Entry("userAddress", "address"),
			new Entry("contractId", "uint256"),
			new Entry("originalOrderId", "uint256"),
			new Entry("nonce", "uint256")));
	
	public String userAddress;
	public long contractId;
	public long originalOrderId;
	public long nonce;
	
	private String signature;
	
	public TypedOrderCancellation(CfxAddress userAddress, long contractId, long orderId, long nonce, String signature) {
		super(userAddress.getABIAddress(), new Uint256(contractId), new Uint256(orderId), new Uint256(nonce));
		
		this.userAddress = userAddress.getHexAddress();
		this.contractId = contractId;
		this.originalOrderId = orderId;
		this.nonce = nonce;
		
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
	public String signer() {
		return this.userAddress;
	}

	@Override
	public String signature() {
		return this.signature;
	}

}
