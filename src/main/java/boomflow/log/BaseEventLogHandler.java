package boomflow.log;

import java.math.BigInteger;
import java.util.List;

import boomflow.common.Address;
import conflux.web3j.response.Log;

/**
 * BaseEventLogHandler is implemented by any object that consume the polled event logs.
 * 
 * Note, the block number is the canonical block height on BSC/ETH, or epoch number
 * on Conflux network.
 */
public interface BaseEventLogHandler {
	
	/**
	 * Default maximum number of pivot blocks to poll at a time.
	 */
	static BigInteger DEFAULT_POLL_BLOCKS = BigInteger.valueOf(10);
	
	/**
	 * Returns the latest polled pivot block number. Generally, the value
	 * is loaded from database or configuration service when initialized,
	 * and updated via method <code>handleEventLogs</code>.
	 */
	BigInteger getLastPollBlockNumber();
	
	/**
	 * Returns the contract addresses to poll event logs.
	 */
	List<Address> getPollAddresses();
	
	/**
	 * Returns the topics to poll event logs.
	 */
	List<String> getPollTopics();
	
	/**
	 * Maximum number of pivot blocks to poll at a time.
	 */
	default BigInteger getMaxPollBlocks() {
		return DEFAULT_POLL_BLOCKS;
	}
	
	EventLogData parseLog(Log log);
	EventLogData parseLog(org.web3j.protocol.core.methods.response.Log log);
	
	/**
	 * Handle the polled event logs and also update the last polled pivot block number.
	 * Generally, the implementation should update off-chain database in a transaction 
	 * in case of program crashed or power off.
	 * @param logs polled event logs.
	 * @param lastPollBlock last polled pivot block number.
	 */
	void handleEventLogs(List<EventLogData> logs, BigInteger lastPollBlockNumber);

}
