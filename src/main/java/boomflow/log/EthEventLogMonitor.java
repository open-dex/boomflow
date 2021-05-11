package boomflow.log;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;

import boomflow.common.Address;
import boomflow.common.EthWeb3Wrapper;

public class EthEventLogMonitor extends EventLogMonitor {
	
	private EthWeb3Wrapper web3j;
	private BigInteger confirmBlocks;

	public EthEventLogMonitor(EthWeb3Wrapper web3j, BaseEventLogHandler handler, int confirmBlocks) {
		super(handler);
		
		this.web3j = web3j;
		this.confirmBlocks = BigInteger.valueOf(confirmBlocks);
	}

	@Override
	protected BigInteger getLatestConfirmedBlock() {
		BigInteger blockNumber = this.web3j.getBlockNumber();
		return this.confirmBlocks.compareTo(blockNumber) >= 0
				? BigInteger.ZERO
				: blockNumber.subtract(this.confirmBlocks);
	}

	@Override
	protected List<EventLogData> pollEventLogs(BigInteger from, BigInteger to, List<Address> contracts, List<String> topics) {
		EthFilter filter = new EthFilter(
				DefaultBlockParameter.valueOf(from),
				DefaultBlockParameter.valueOf(to),
				contracts.stream().map(Address::toString).collect(Collectors.toList()));
		filter.addOptionalTopics(topics.toArray(new String[topics.size()]));
		
		List<Log> logs = this.web3j.getLogs(filter);
		
		return logs.stream()
				.filter(l -> !l.isRemoved())
				.map(l -> this.handler.parseLog(l))
				.collect(Collectors.toList());
	}
	
}
