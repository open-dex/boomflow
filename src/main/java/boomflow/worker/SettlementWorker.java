package boomflow.worker;

import java.util.concurrent.ScheduledExecutorService;

import boomflow.common.worker.BatchWorker;
import boomflow.common.worker.PendingException;
import boomflow.event.Handler;
import boomflow.worker.settle.Settleable;
import boomflow.worker.settle.SettlementStatus;
import conflux.web3j.RpcException;

/**
 * Asynchronous worker to settle data on blockchain.
 * 
 * When service restarted, application should reload off chain settled data 
 * and submit all of them to this worker to continue settlement.
 */
public abstract class SettlementWorker extends BatchWorker<Settleable> {
	
	// Following constants could be adjusted according to contract gas fee and settlement TPS.
	private static final int DEFAULT_BATCH_SIZE = 30;
	private static final int DEFAULT_WAIT_COUNTDOWN = 3;
	private static final int DEFAULT_WAIT_INTERVAL_MILLIS = 1000;
	
	protected SettlementHandler handler;
	protected TransactionConfirmationMonitor monitor;
	private TransactionRelayer txRelayer;
	
	protected SettlementWorker(ScheduledExecutorService executor,
			SettlementHandler handler,
			TransactionConfirmationMonitor monitor) {
		this(executor, handler, monitor, DEFAULT_BATCH_SIZE, DEFAULT_WAIT_COUNTDOWN, DEFAULT_WAIT_INTERVAL_MILLIS);
	}
	
	protected SettlementWorker(ScheduledExecutorService executor,
			SettlementHandler handler,
			TransactionConfirmationMonitor monitor,
			int batchSize, int waitCountdown, long waitIntervalMillis) {
		super(executor, batchSize, waitCountdown, waitIntervalMillis);
		
		this.handler = handler;
		this.monitor = monitor;
		
		this.monitor.onTxLongUnexecuted.addHandler(new Handler<Settleable>() {
			
			@Override
			public void handle(Settleable data) {
				SettlementWorker.this.submit(data, true);
				SettlementWorker.this.handler.onTransactionLongUnexecuted(data);
			}
			
		});
		this.monitor.onTxFailed.addHandler(new Handler<Settleable>() {

			@Override
			public void handle(Settleable data) {
				SettlementWorker.this.handler.onTransactionFailure(data);
				SettlementWorker.this.setPaused(true);
			}
			
		});
	}
	
	@Override
	public void setPaused(boolean paused) {
		super.setPaused(paused);
		
		this.monitor.setPaused(paused);
	}
	
	public TransactionConfirmationMonitor getMonitor() {
		return monitor;
	}
	
	public void setTxRelayer(TransactionRelayer txRelayer) {
		this.txRelayer = txRelayer;
	}
	
	protected void relayTx(String signedTx) {
		if (this.txRelayer != null) {
			this.txRelayer.submit(signedTx);
		}
	}
	
	/**
	 * Ensure not too many pending transactions in txpool to avoid <code>NONCE_TOO_FUTURE</code> error.
	 * 
	 * @throws PendingException if too many pending transactions.
	 */
	protected abstract void validatePendingNonce() throws PendingException, RpcException;
	
	/**
	 * Check if the specified data is settled on chain.
	 * @return true if data already settled on chain, false otherwise.
	 */
	protected abstract boolean isSettledOnChain(Settleable settleable) throws RpcException;
	
	/**
	 * Send transaction to full node, including updating the settlement status in database.
	 * 
	 * There are x cases to settle data on chain:
	 * 1) Settle data for the first time.
	 * 2) Settle incomplete data when service restarted.
	 * 3) Re-settle data when failed to send raw transaction.
	 * 4) Re-settle data if transaction not executed for a long time, e.g. price too low.
	 */
	protected abstract void sendTransaction(Settleable data) throws Exception;
	
	@Override
	protected void doWork(Settleable data) throws Exception {
		this.validatePendingNonce();
		
		if (!this.isSettledOnChain(data)) {
			this.sendTransaction(data);
		}
		
		data.updateSettlement(SettlementStatus.OnChainSettled);
		
		this.monitor.add(data);
	}

	@Override
	protected void onFailure(Settleable data, Exception e) {
		this.handler.onException(data, e);
	}

}
