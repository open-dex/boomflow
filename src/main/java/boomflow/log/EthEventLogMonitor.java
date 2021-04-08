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

	public EthEventLogMonitor(EthWeb3Wrapper web3j, EventLogHandler handler, int confirmBlocks) {
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
	protected void pollEventLogs(BigInteger from, BigInteger to, List<Address> contracts, 
			List<DepositData> deposits, List<ScheduleWithdrawRequest> withdraws) {
		EthFilter filter = new EthFilter(
				DefaultBlockParameter.valueOf(from),
				DefaultBlockParameter.valueOf(to),
				contracts.stream().map(Address::toString).collect(Collectors.toList()));
		filter.addOptionalTopics(DepositData.EVENT_HASH, ScheduleWithdrawRequest.EVENT_HASH);
		
		List<Log> logs = this.web3j.getLogs(filter);
		
		for (Log log : logs) {
			if (log.isRemoved()) {
				logger.debug("log removed: {}", log);
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
