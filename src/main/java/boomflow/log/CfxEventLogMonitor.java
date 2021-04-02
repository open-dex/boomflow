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
	
	private static final List<List<String>> FILTER_TOPCIS = Arrays.asList(
			Arrays.asList(DepositData.EVENT_HASH, ScheduleWithdrawRequest.EVENT_HASH),
			null, null, null);
	
	private Cfx cfx;

	public CfxEventLogMonitor(Cfx cfx, EventLogHandler handler) {
		super(handler);
		
		this.cfx = cfx;
	}

	@Override
	protected BigInteger getLatestConfirmedBlock() {
		return this.cfx.getEpochNumber(Epoch.latestConfirmed()).sendAndGet();
	}

	@Override
	protected void pollEventLogs(BigInteger from, BigInteger to, List<Address> contracts, 
			List<DepositData> deposits, List<ScheduleWithdrawRequest> withdraws) {
		LogFilter filter = new LogFilter();
		
        filter.setFromEpoch(Epoch.numberOf(from));
		filter.setToEpoch(Epoch.numberOf(to));
		filter.setAddress(contracts.stream().map(addr -> new CfxAddress(addr.toString())).collect(Collectors.toList()));
		filter.setTopics(FILTER_TOPCIS);
		
		List<Log> logs = this.cfx.getLogs(filter).sendAndGet();
		
		for (Log log : logs) {
			if (!log.getTransactionHash().isPresent()) {
				continue;
			}
			
			// topics[0] is event hash, and should not be empty
			String eventHash = log.getTopics().get(0);
			
			if (DepositData.EVENT_HASH.equalsIgnoreCase(eventHash)) {
				DepositData data = new DepositData(log);
				logger.trace("polled deposit event log, sender = {}, recipient = {}, amount = {}", data.getSenderAddress(), data.getRecipientAddress(), data.getAmount());
				deposits.add(data);
			}
			
			if (ScheduleWithdrawRequest.EVENT_HASH.equalsIgnoreCase(eventHash)) {
				ScheduleWithdrawRequest data = new ScheduleWithdrawRequest(log);
				logger.trace("polled withdraw event log, sender = {}, time = {}", data.getSenderAddress(), data.getTime());
				withdraws.add(data);
			}
		}
	}
	
}
