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

public class TypedAdjustMarginRateRequest extends StaticStruct implements TypedData {
	
	private static final String PRIMARY_TYPE = "AdjustMarginRateRequest";
	
	private static final Map<String, List<Entry>> SCHEMAS = Collections.singletonMap(PRIMARY_TYPE, Arrays.asList(
			new Entry("userAddress", "address"),
			new Entry("contractId", "uint256"),
			new Entry("marginType", "uint256"),
			new Entry("initMarginRate", "uint256"),
			new Entry("posiSide", "int256"),
			new Entry("posiId", "uint256"),
			new Entry("nonce", "uint256")));
	
	public String userAddress;
	public long contractId;
	public long marginType;
	public long initMarginRate;
	public long posiSide;
	public long posiId;
	public long nonce;
	
	private String signature;
	
	public TypedAdjustMarginRateRequest(CfxAddress userAddress, long contractId, long marginType, long initMarginRate, 
			long posiSide, long posiId, long nonce, String signature) {
		super(userAddress.getABIAddress(), new Uint256(contractId), new Uint256(marginType), new Uint256(initMarginRate),
				new Uint256(posiSide), new Uint256(posiId), new Uint256(nonce));
		
		this.userAddress = userAddress.getHexAddress();
		this.contractId = contractId;
		this.marginType = marginType;
		this.initMarginRate = initMarginRate;
		this.posiSide = posiSide;
		this.posiId = posiId;
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
