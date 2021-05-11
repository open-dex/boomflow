package boomflow.log;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import conflux.web3j.response.Log;

/**
 * EventLogHandler is a log handler for boomflow events.
 */
public interface EventLogHandler extends BaseEventLogHandler {
	
	@Override
	default List<String> getPollTopics() {
		return Arrays.asList(DepositData.EVENT_HASH, ScheduleWithdrawRequest.EVENT_HASH);
	}
	
	@Override
	default EventLogData parseLog(Log log) {
		String eventHash = log.getTopics().get(0);
		
		if (DepositData.EVENT_HASH.equalsIgnoreCase(eventHash)) {
			return new DepositData(log);
		}
		
		if (ScheduleWithdrawRequest.EVENT_HASH.equalsIgnoreCase(eventHash)) {
			return new ScheduleWithdrawRequest(log);
		}
		
		return new EventLogData(log);
	}
	
	@Override
	default EventLogData parseLog(org.web3j.protocol.core.methods.response.Log log) {
		String eventHash = log.getTopics().get(0);
		
		if (DepositData.EVENT_HASH.equalsIgnoreCase(eventHash)) {
			return new DepositData(log);
		}
		
		if (ScheduleWithdrawRequest.EVENT_HASH.equalsIgnoreCase(eventHash)) {
			return new ScheduleWithdrawRequest(log);
		}
		
		return new EventLogData(log);
	}
	
	@Override
	default void handleEventLogs(List<EventLogData> logs, BigInteger lastPollBlockNumber) {
		List<DepositData> deposits = new LinkedList<DepositData>();
		List<ScheduleWithdrawRequest> withdraws = new LinkedList<ScheduleWithdrawRequest>();
		
		for (EventLogData log : logs) {
			if (log instanceof DepositData) {
				deposits.add((DepositData) log);
			}
			
			if (log instanceof ScheduleWithdrawRequest) {
				withdraws.add((ScheduleWithdrawRequest) log);
			}
		}
		
		this.handleEventLogs(deposits, withdraws, lastPollBlockNumber);
	}
	
	/**
	 * Handle the polled event logs, including deposits and withdraw requests, and also
	 * update the last polled pivot block number. Generally, the implementation should update 
	 * off-chain database in a transaction in case of program crashed or power off.
	 * @param deposits polled <code>deposit</code> event logs.
	 * @param withdraws polled <code>withdraw</code> event logs.
	 * @param lastPollBlock last polled pivot block number.
	 */
	void handleEventLogs(List<DepositData> deposits, List<ScheduleWithdrawRequest> withdraws, BigInteger lastPollBlockNumber);
	
}
