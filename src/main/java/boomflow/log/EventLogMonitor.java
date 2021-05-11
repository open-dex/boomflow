package boomflow.log;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import boomflow.common.Address;
import boomflow.common.Utils;
import conflux.web3j.RpcException;

/**
 * EventLogMonitor is used to monitor event logs of all CRC-L contracts, including
 * deposit and withdraw event logs.
 */
public abstract class EventLogMonitor implements Runnable {
	
	protected BaseEventLogHandler handler;
	protected Logger logger = LoggerFactory.getLogger(EventLogMonitor.class);
	
	protected EventLogMonitor(BaseEventLogHandler handler) {
		this.handler = handler;
	}
	
	protected abstract BigInteger getLatestConfirmedBlock();
	protected abstract List<EventLogData> pollEventLogs(BigInteger from, BigInteger to, List<Address> contracts, List<String> topics);
	
	/**
	 * Poll event logs for confirmed blocks.
	 * @return <code>true</code> if there are more blocks to poll. Otherwise, false.
	 */
	public boolean pollOnce() throws RpcException {
		// only poll event logs from confirmed blocks.
		BigInteger confirmed = this.getLatestConfirmedBlock();
		BigInteger lastPolled = this.handler.getLastPollBlockNumber();
		if (confirmed.compareTo(lastPolled) <= 0) {
			logger.trace("wait for more blocks to poll event logs");
			return false;
		}
		
		// poll logs from the latest confirmed block if address not specified
		List<Address> pollAddresses = this.handler.getPollAddresses();
        if (pollAddresses.isEmpty()) {
			this.handler.handleEventLogs(Collections.emptyList(), confirmed);
			logger.trace("address not specified and just move forward the last polled block");
			return false;
		}
        
        // limit the number of polled blocks to avoid RPC timeout
        BigInteger pollTo = this.handler.getMaxPollBlocks().add(lastPolled).min(confirmed);
        
        // poll logs
        BigInteger pollFrom = lastPolled.add(BigInteger.ONE);
		
		List<EventLogData> logs = this.pollEventLogs(pollFrom, pollTo, pollAddresses, this.handler.getPollTopics());
		this.handler.handleEventLogs(logs, pollTo);
        
        return confirmed.compareTo(pollTo) > 0;
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
