package boomflow.log;

import java.math.BigInteger;
import java.util.List;

import conflux.web3j.types.Address;

/**
 * EventLogHandler is implemented by any object that consume the polled event logs.
 */
public interface EventLogHandler {
	
	/**
	 * Default maximum number of epochs to poll at a time.
	 */
	static BigInteger DEFAULT_POLL_EPOCHS = BigInteger.valueOf(10);
	
	/**
	 * Returns the latest polled epoch number. Generally, the value is
	 * loaded from database or configuration service when initialized,
	 * and updated via method <code>handleEventLogs</code>.
	 */
	BigInteger getLastPollEpoch();
	
	/**
	 * Returns the contract addresses to poll event logs.
	 */
	List<Address> getPollAddresses();
	
	/**
	 * Maximum number of epochs to poll at a time.
	 */
	default BigInteger getMaxPollEpochs() {
		return DEFAULT_POLL_EPOCHS;
	}
	
	/**
	 * Handle the polled event logs, including deposits and withdraw requests, and also
	 * update the last polled epoch number. Generally, the implementation should update 
	 * off-chain database in a transaction in case of program crashed or power off.
	 * @param deposits polled <code>deposit</code> event logs.
	 * @param withdraws polled <code>withdraw</code> event logs.
	 * @param lastPollEpoch last polled epoch number.
	 */
	void handleEventLogs(List<DepositData> deposits, List<ScheduleWithdrawRequest> withdraws, BigInteger lastPollEpoch);

}
