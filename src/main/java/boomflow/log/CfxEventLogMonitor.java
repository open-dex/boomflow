package boomflow.log;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import boomflow.common.Address;
import conflux.web3j.Cfx;
import conflux.web3j.request.Epoch;
import conflux.web3j.request.LogFilter;
import conflux.web3j.response.Log;
import conflux.web3j.types.CfxAddress;

public class CfxEventLogMonitor extends EventLogMonitor {
	
	private Cfx cfx;

	public CfxEventLogMonitor(Cfx cfx, BaseEventLogHandler handler) {
		super(handler);
		
		this.cfx = cfx;
	}

	@Override
	protected BigInteger getLatestConfirmedBlock() {
		return this.cfx.getEpochNumber(Epoch.latestConfirmed()).sendAndGet();
	}

	@Override
	protected List<EventLogData> pollEventLogs(BigInteger from, BigInteger to, List<Address> contracts, List<String> topics) {
		LogFilter filter = new LogFilter();
		
        filter.setFromEpoch(Epoch.numberOf(from));
		filter.setToEpoch(Epoch.numberOf(to));
		filter.setAddress(contracts.stream().map(addr -> new CfxAddress(addr.toString())).collect(Collectors.toList()));
		filter.setTopics(Arrays.asList(topics));
		
		List<Log> logs = this.cfx.getLogs(filter).sendAndGet();
		
		return logs.stream()
				.filter(l -> l.getTransactionHash().isPresent())
				.map(l -> this.handler.parseLog(l))
				.collect(Collectors.toList());
	}
	
}
