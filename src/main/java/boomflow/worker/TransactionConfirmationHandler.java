package boomflow.worker;

import boomflow.worker.settle.Settleable;

public interface TransactionConfirmationHandler {
	
	/**
	 * Fired when transaction not executed for a long time.
	 */
	default void onTransactionLongUnexecuted(Settleable data) {}
	
	/**
	 * Fired when transaction execution failed or receipt validation failed.
	 * 
	 * Generally, the <code>TransactionConfirmationMonitor</code> will be paused
	 * automatically to prevent more failures, and administrator must be involved
	 * to check the root cause and mitigate the issue in time.
	 * 
	 * Once the failed transaction resolved, administrator have to resume in manual
	 * via <code>unpause</code> method.
	 */
	void onTransactionFailure(Settleable data);

}
