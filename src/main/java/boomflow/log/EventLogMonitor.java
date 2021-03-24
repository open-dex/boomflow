package boomflow.log;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import boomflow.common.Utils;
import conflux.web3j.Cfx;
import conflux.web3j.RpcException;
import conflux.web3j.request.Epoch;
import conflux.web3j.request.LogFilter;
import conflux.web3j.response.Log;
import conflux.web3j.types.Address;

/**
 * EventLogMonitor is used to monitor event logs of all CRC-L contracts, including
 * deposit and withdraw event logs.
 */
public class EventLogMonitor implements Runnable {
	
	private static final List<List<String>> FILTER_TOPCIS = Arrays.asList(
			Arrays.asList(DepositData.EVENT_HASH, ScheduleWithdrawRequest.EVENT_HASH),
			null, null, null);
	
	private Cfx cfx;
	private EventLogHandler handler;
	private Logger logger = LoggerFactory.getLogger(EventLogMonitor.class);
	
	public EventLogMonitor(Cfx cfx, EventLogHandler handler) {
		this.cfx = cfx;
		this.handler = handler;
	}
	
	/**
	 * Poll event logs for confirmed epochs.
	 * @return <code>true</code> if there are more epochs to poll. Otherwise, false.
	 */
	public boolean pollOnce() throws RpcException {
		// only poll confirmed event logs
		BigInteger confirmedEpoch = this.cfx.getEpochNumber(Epoch.latestConfirmed()).sendAndGet();
		BigInteger lastPollEpoch = this.handler.getLastPollEpoch();
		if (confirmedEpoch.compareTo(lastPollEpoch) <= 0) {
			logger.trace("wait for more epochs to poll event logs from confirmed epoch");
			return false;
		}
		
		// poll logs from the latest confirmed epoch if address not specified
		List<Address> pollAddresses = this.handler.getPollAddresses();
        if (pollAddresses.isEmpty()) {
			this.handler.handleEventLogs(Collections.emptyList(), Collections.emptyList(), confirmedEpoch);
			logger.trace("address not specified and just move forward the last polled epoch");
			return false;
		}
        
        // limit the number of polled epochs to avoid RPC timeout
        BigInteger pollEpochTo = this.handler.getMaxPollEpochs().add(lastPollEpoch).min(confirmedEpoch);
        
        // poll logs
        BigInteger pollEpochFrom = lastPollEpoch.add(BigInteger.ONE);
		List<Log> logs = this.getLogs(pollEpochFrom, pollEpochTo, pollAddresses);
		
		
		List<DepositData> deposits = new LinkedList<DepositData>();
		List<ScheduleWithdrawRequest> withdraws = new LinkedList<ScheduleWithdrawRequest>();
		
		for (Log log : logs) {
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
		
		if (!deposits.isEmpty() || !withdraws.isEmpty()) {
			logger.info("polled event logs, epochFrom = {}, epochTo = {}, deposits = {}, withdraws = {}",
					pollEpochFrom, pollEpochTo, deposits.size(), withdraws.size());
		}
		
		this.handler.handleEventLogs(deposits, withdraws, pollEpochTo);
        
        return confirmedEpoch.compareTo(pollEpochTo) > 0;
	}
	
	private List<Log> getLogs(BigInteger from, BigInteger to, List<Address> addresses) throws RpcException {
		LogFilter filter = new LogFilter();
		
        filter.setFromEpoch(Epoch.numberOf(from));
		filter.setToEpoch(Epoch.numberOf(to));
		filter.setAddress(addresses);
		filter.setTopics(FILTER_TOPCIS);
		
		return this.cfx.getLogs(filter).sendAndGet();
	}
	
	/**
	 * Schedule job to poll event logs every 5000 milliseconds.
	 */
	public void schedule(ScheduledExecutorService executor) {
		this.schedule(executor, 5000);
	}

	/**
	 * Schedule job to poll event logs for specified interval.
	 */
	public void schedule(ScheduledExecutorService executor, long delayMillis) {
		executor.scheduleWithFixedDelay(this, delayMillis, delayMillis, TimeUnit.MILLISECONDS);
	}

	@Override
	public void run() {
		logger.trace("poll event logs started");
		
		try {
			while (this.pollOnce()) {
				logger.trace("continue to poll event logs");
			}
		} catch (RpcException e) {
			if (Utils.isRpcError(e)) {
				logger.error("failed to poll event logs", e);
			} else {
				logger.debug("failed to poll event logs: {}", e.getMessage());
			}
		} catch (Exception e) {
			logger.error("failed to poll event logs", e);
		}
		
		logger.trace("poll event logs ended");
	}

}
