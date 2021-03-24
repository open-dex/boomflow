package boomflow.log;

import java.math.BigInteger;
import java.util.Arrays;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.generated.Uint256;

import conflux.web3j.response.Log;
import conflux.web3j.types.CfxAddress;

public class DepositData extends EventLogData {
	// event Deposit(address indexed sender, address indexed recipient, uint256 value)
	// 0x5548c837ab068cf56a2c2479df0882a4922fd203edb7517321831d95078c5f62
	public static final String EVENT_HASH = EventEncoder.encode(new Event("Deposit", Arrays.asList(
			TypeReference.create(Address.class, true),
			TypeReference.create(Address.class, true),
			TypeReference.create(Uint256.class))));
	
	private CfxAddress senderAddress;
	private CfxAddress recipientAddress;
	private BigInteger amount;
	
	public DepositData(Log log) {
		super(log);
		
		this.senderAddress = this.parseCfxAddress(log.getTopics().get(1));
		this.recipientAddress = this.parseCfxAddress(log.getTopics().get(2));
		this.amount = parseUint256(log.getData());
	}
	
	public CfxAddress getSenderAddress() {
		return senderAddress;
	}

	public CfxAddress getRecipientAddress() {
		return recipientAddress;
	}

	public BigInteger getAmount() {
		return amount;
	}
}
