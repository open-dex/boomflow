package boomflow.eip712;

import java.math.BigInteger;
import java.util.ArrayList;
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

/**
 * Withdraw request that supports to validate against EIP712 signature.
 */
public class TypedWithdraw extends DynamicStruct implements TypedData {
	
	private static final Map<String, List<Entry>> SCHEMAS;
	
	static {
		List<Entry> types = new ArrayList<Entry>(TypedWithdrawUser.SCHEMA);
		types.add(new Entry("gasFeeToken", "address"));
		types.add(new Entry("gasFeeAmount", "uint256"));
		types.add(new Entry("gasFeeRecipient", "address"));
		SCHEMAS = Collections.singletonMap(TypedWithdrawUser.PRIMARY_TYPE, types);
	}
	
	public String userAddress;
	public BigInteger amount;
	public String recipient;
	public boolean burn;
	public long nonce;
	public String gasFeeToken;
	public BigInteger gasFeeAmount;
	public String gasFeeRecipient;
	
	private Address contractAddress;
	private Address signer;
	private String signature;
	
	public TypedWithdraw(Address userAddress, BigInteger amount, Address recipient, boolean burn, long userNonce, String userSignature,
			Address gasFeeToken, BigInteger gasFeeAmount, Address gasFeeRecipient, long signerNonce,
			Address contractAddress, Address signer, String signature) {
		super(userAddress.toABI(), new Uint256(amount), recipient.toABI(), new Bool(burn), new Uint256(userNonce), EncodeUtils.hex2Bytes(userSignature),
				gasFeeToken.toABI(), new Uint256(gasFeeAmount), gasFeeRecipient.toABI(), new Uint256(signerNonce),
				signer.toABI(), EncodeUtils.hex2Bytes(signature));
		
		this.userAddress = userAddress.toHex();
		this.amount = amount;
		this.recipient = recipient.toHex();
		this.burn = burn;
		this.nonce = signerNonce;
		
		this.gasFeeToken = gasFeeToken.toHex();
		this.gasFeeAmount = gasFeeAmount;
		this.gasFeeRecipient = gasFeeRecipient.toHex();
		
		this.contractAddress = contractAddress;
		this.signer = signer;
		this.signature = signature;
	}

	@Override
	public String primaryType() {
		return TypedWithdrawUser.PRIMARY_TYPE;
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
