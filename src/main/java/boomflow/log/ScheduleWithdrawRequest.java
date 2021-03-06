package boomflow.log;

import java.math.BigInteger;
import java.util.Arrays;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.generated.Uint256;

import boomflow.common.Address;
import conflux.web3j.response.Log;

public class ScheduleWithdrawRequest extends EventLogData {
	// event ScheduleWithdraw(address indexed sender, uint256 time);
	// 0x0ebe7b96b8d0566030cee68cd5153d3af3eb238d56092c9493a18e6d0b568369
	public static final String EVENT_HASH = EventEncoder.encode(new Event("ScheduleWithdraw", Arrays.asList(
			TypeReference.create(org.web3j.abi.datatypes.Address.class, true),
			TypeReference.create(Uint256.class))));
	
	private Address senderAddress;
	private BigInteger time;
	
	public ScheduleWithdrawRequest(Log log) {
		super(log);
		
		this.senderAddress = this.parseCfxAddress(log.getTopics().get(1));
		this.time = parseUint256(log.getData());
	}
	
	public ScheduleWithdrawRequest(org.web3j.protocol.core.methods.response.Log log) {
		super(log);
		
		this.senderAddress = parseEthAddress(log.getTopics().get(1));
		this.time = parseUint256(log.getData());
	}
	
	public Address getSenderAddress() {
		return senderAddress;
	}

	/**
	 * Return <code>block.timestamp</code> on blockchain if user request to withdraw.
	 * Otherwise, return 0 when force withdraw completed.
	 */
	public BigInteger getTime() {
		return time;
	}
}
