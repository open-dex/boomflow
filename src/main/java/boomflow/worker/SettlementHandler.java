package boomflow.worker;

import java.math.BigInteger;

import org.web3j.protocol.core.Response.Error;

import boomflow.worker.settle.Settleable;

/**
 * SettlementHandler is used to handle important events that fired in <code>SettlementWorker</code>.
 */
public interface SettlementHandler extends NonceKeeper, TransactionConfirmationHandler {
	
	/**
	 * Fired when too many pending transactions pooled on full node.
	 */
	void onNonceTooFuture(BigInteger offChainNonce, BigInteger onChainNonce);
	
	/**
	 * Fired when failed to send transaction to full node due to transaction pool is full.
	 * 
	 * Once happened, administrator should be involved to check whether something wrong
	 * with the full node, or the blockchain network is really congested.
	 * 
	 * Generally, client should listen to such event, and notify administrator timely.
	 * On the other hand, <code>SettlementWorker</code> will stop working for a while
	 * until the full node recovered. 
	 */
	void onTransactionPoolFull(Settleable data);
	
	/**
	 * Fired when failed to send transaction to full node due to unexpected error.
	 * 
	 * Once happened, administrator should be involved to check the root cause timely.
	 * On the other hand, <code>SettlementWorker</code> will be paused to avoid more
	 * errors, and requires administrator to resume in manual.
	 */
	void onUnexpectedTransactionError(Settleable data, Error error);
	
	/**
	 * Fired when any unexpected exception occurred, excluding the PendingException.
	 * E.g. temporary network issue to full node, or database service unavailable.
	 * 
	 * Once happened, worker will continue to re-settle the data again. Client could
	 * pause the worker in manual if the failure not recovered in a reasonable period
	 * of time.
	 */
	void onException(Settleable data, Exception e);

}
