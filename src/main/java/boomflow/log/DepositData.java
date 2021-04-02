package boomflow.log;

import java.math.BigInteger;
import java.util.Arrays;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.generated.Uint256;

import boomflow.common.Address;
import conflux.web3j.response.Log;

public class DepositData extends EventLogData {
	// event Deposit(address indexed sender, address indexed recipient, uint256 value)
	// 0x5548c837ab068cf56a2c2479df0882a4922fd203edb7517321831d95078c5f62
	public static final String EVENT_HASH = EventEncoder.encode(new Event("Deposit", Arrays.asList(
			TypeReference.create(org.web3j.abi.datatypes.Address.class, true),
			TypeReference.create(org.web3j.abi.datatypes.Address.class, true),
			TypeReference.create(Uint256.class))));
	
	private Address senderAddress;
	private Address recipientAddress;
	private BigInteger amount;
	
	public DepositData(Log log) {
		super(log);
		
		this.senderAddress = this.parseCfxAddress(log.getTopics().get(1));
		this.recipientAddress = this.parseCfxAddress(log.getTopics().get(2));
		this.amount = parseUint256(log.getData());
	}
	
	public DepositData(org.web3j.protocol.core.methods.response.Log log) {
		super(log);
		
		this.senderAddress = parseEthAddress(log.getTopics().get(1));
		this.recipientAddress = parseEthAddress(log.getTopics().get(2));
		this.amount = parseUint256(log.getData());
	}
	
	public Address getSenderAddress() {
		return senderAddress;
	}

	public Address getRecipientAddress() {
		return recipientAddress;
	}

	public BigInteger getAmount() {
		return amount;
	}
}
