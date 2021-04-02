package boomflow.log;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog.LogObject;
import org.web3j.protocol.core.methods.response.EthLog.LogResult;

import boomflow.common.Address;
import conflux.web3j.RpcException;

public class EthEventLogMonitor extends EventLogMonitor {
	
	private Web3j web3j;

	public EthEventLogMonitor(Web3j web3j, EventLogHandler handler) {
		super(handler);
		
		this.web3j = web3j;
	}

	@Override
	protected BigInteger getLatestConfirmedBlock() {
		try {
			return this.web3j.ethBlockNumber().send().getBlockNumber();
		} catch (IOException e) {
			throw RpcException.sendFailure(e);
		}
	}

	@Override
	protected void pollEventLogs(BigInteger from, BigInteger to, List<Address> contracts, 
			List<DepositData> deposits, List<ScheduleWithdrawRequest> withdraws) {
		EthFilter filter = new EthFilter(
				DefaultBlockParameter.valueOf(from),
				DefaultBlockParameter.valueOf(to),
				contracts.stream().map(Address::toString).collect(Collectors.toList()));
		filter.addOptionalTopics(DepositData.EVENT_HASH, ScheduleWithdrawRequest.EVENT_HASH);
		
		@SuppressWarnings("rawtypes")
		List<LogResult> logObjects;
		
		try {
			logObjects = this.web3j.ethGetLogs(filter).send().getLogs();
		} catch (IOException e) {
			throw RpcException.sendFailure(e);
		}
		
		if (logObjects.isEmpty()) {
			return;
		}
		
		if (!(logObjects.get(0) instanceof LogObject)) {
			logger.error("unexpected log type retrieved: {}", logObjects.get(0).getClass());
			return;
		}
		
		for (Object o : logObjects) {
			LogObject log = (LogObject) o;
			
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
