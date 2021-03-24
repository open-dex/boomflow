package boomflow.eip712;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.StaticStruct;
import org.web3j.abi.datatypes.generated.Uint256;

import boomflow.eip712.core.Domain;
import boomflow.eip712.core.Entry;
import boomflow.eip712.core.TypedData;
import conflux.web3j.types.CfxAddress;

/**
 * Withdraw request that supports to validate against EIP712 signature.
 */
public class TypedWithdraw extends StaticStruct implements TypedData {
	
	private static final String PRIMARY_TYPE = "WithdrawRequest";
	
	private static final Map<String, List<Entry>> SCHEMAS = Collections.singletonMap(PRIMARY_TYPE, Arrays.asList(
			new Entry("userAddress", "address"),
			new Entry("amount", "uint256"),
			new Entry("recipient", "address"),
			new Entry("burn", "bool"),
			new Entry("nonce", "uint256")));
	
	public String userAddress;
	public BigInteger amount;
	public String recipient;
	public boolean burn;
	public long nonce;
	
	private CfxAddress contractAddress;
	private String signature;
	
	public TypedWithdraw(CfxAddress userAddress, BigInteger amount, CfxAddress recipient, boolean burn, long nonce, CfxAddress contractAddress, String signature) {
		super(userAddress.getABIAddress(), new Uint256(amount), recipient.getABIAddress(), new Bool(burn), new Uint256(nonce));
		
		this.userAddress = userAddress.getHexAddress();
		this.amount = amount;
		this.recipient = recipient.getHexAddress();
		this.burn = burn;
		this.nonce = nonce;
		
		this.contractAddress = contractAddress;
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
	public String signer() {
		return this.userAddress;
	}
	
	@Override
	public String signature() {
		return this.signature;
	}

}
