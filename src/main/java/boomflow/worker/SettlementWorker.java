package boomflow.worker;

import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import org.web3j.crypto.Hash;

import boomflow.common.worker.BatchWorker;
import boomflow.common.worker.PendingException;
import boomflow.event.Handler;
import boomflow.worker.settle.Settleable;
import boomflow.worker.settle.SettlementStatus;
import boomflow.worker.settle.TransactionRecorder;
import conflux.web3j.Account;
import conflux.web3j.Cfx;
import conflux.web3j.types.RawTransaction;
import conflux.web3j.types.SendTransactionResult;

/**
 * Asynchronous worker to settle data on blockchain.
 * 
 * When service restarted, application should reload off chain settled data 
 * and submit all of them to this worker to continue settlement.
 */
public class SettlementWorker extends BatchWorker<Settleable> {
	
	// Following constants could be adjusted according to contract gas fee and settlement TPS.
	private static final int DEFAULT_BATCH_SIZE = 30;
	private static final int DEFAULT_WAIT_COUNTDOWN = 3;
	private static final int DEFAULT_WAIT_INTERVAL_MILLIS = 1000;
	
	private static final BigInteger CHECK_NONCE_INTERVAL = BigInteger.valueOf(100);
	private static final BigInteger CHECK_NONCE_FUTURE_THRESHOLD = BigInteger.valueOf(1000);
	
	private static final BigInteger DEFAULT_GAS_PRICE_INCREMENT = BigInteger.ONE;
	
	private Account admin;
	private SettlementHandler handler;
	private TransactionConfirmationMonitor monitor;
	private TransactionRelayer txRelayer;
	
	public SettlementWorker(ScheduledExecutorService executor, Account admin, SettlementHandler handler) {
		this(executor, admin, handler, null);
	}
	
	public SettlementWorker(ScheduledExecutorService executor, Account admin, SettlementHandler handler, TransactionRelayer txRelayer) {
		this(executor, admin, handler, txRelayer, DEFAULT_BATCH_SIZE, DEFAULT_WAIT_COUNTDOWN, DEFAULT_WAIT_INTERVAL_MILLIS);
	}
	
	public SettlementWorker(ScheduledExecutorService executor, Account admin, SettlementHandler handler, TransactionRelayer txRelayer, int batchSize, int waitCountdown, long waitIntervalMillis) {
		super(executor, batchSize, waitCountdown, waitIntervalMillis);
		
		this.admin = admin;
		this.handler = handler;
		this.txRelayer = txRelayer;
		
		this.monitor = new TransactionConfirmationMonitor(admin.getCfx());
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
		
		NonceSyncer nonceSyncer = new CfxNonceSyncer(admin);
		nonceSyncer.sync(handler);
	}
	
	@Override
	public void setPaused(boolean paused) {
		super.setPaused(paused);
		
		this.monitor.setPaused(paused);
	}
	
	public TransactionConfirmationMonitor getMonitor() {
		return monitor;
	}
	
	private void relayTx(String signedTx) {
		if (this.txRelayer != null) {
			this.txRelayer.submit(signedTx);
		}
	}
	
	@Override
	protected void doWork(Settleable data) throws Exception {
		this.validatePendingNonce();
		
		TransactionRecorder recorder = data.getRecorder();
		
		if (recorder == null) {
			// settle for the first time
			this.sendTransaction(data);
		} else if (recorder.getLast().isLongUnexecuted()) {
			// re-send transaction on any error
			this.sendTransaction(data);
		} else if (recorder.isTxExists(this.admin.getCfx())) {
			// already settled on chain in case of service restarted and continue to settle
		} else {
			// service restarted and last item not settled on chain yet.
			// e.g. program crash before transaction sent to full node.
			this.sendTransaction(data);
		}
		
		data.updateSettlement(SettlementStatus.OnChainSettled);
		
		this.monitor.add(data);
	}
	
	/**
	 * Ensure not too many pending transactions in txpool to avoid <code>NONCE_TOO_FUTURE</code> error.
	 * 
	 * @throws PendingException if too many pending transactions.
	 */
	private void validatePendingNonce() throws PendingException {
		// check once every N settlements
		BigInteger offChainNonce = this.admin.getNonce();
		if (offChainNonce.divideAndRemainder(CHECK_NONCE_INTERVAL)[1].compareTo(BigInteger.ZERO) != 0) {
			return;
		}
		
		// not reached the too future threshold
		BigInteger onChainNonce = this.admin.getCfx().getNonce(admin.getAddress()).sendAndGet();
		if (onChainNonce.add(CHECK_NONCE_FUTURE_THRESHOLD).compareTo(offChainNonce) >= 0) {
			return;
		}
		
		this.handler.onNonceTooFuture(offChainNonce, onChainNonce);
		
		// wait for a while until pending transactions packed on chain
		throw new PendingException(this.getPauseIntervalMillis(), "too many pending transactions");
	}
	
	private void sendTransaction(Settleable data) throws Exception {
		TransactionRecorder recorder = data.getRecorder();
		boolean resendOnError = recorder != null && recorder.getLast().isLongUnexecuted();
		
		// For discarded case, use the original tx nonce.
		// Otherwise, use the latest tx nonce of DEX admin.
		BigInteger nonce = resendOnError ? recorder.getNonce() : this.admin.getNonce();
		
		Cfx cfx = this.admin.getCfx();
		BigInteger epoch = cfx.getEpochNumber().sendAndGet();
		RawTransaction tx = data.getSettlementContext().buildTx(nonce, epoch);
		
		// increase gas price if re-send transaction.
		if (resendOnError) {
			Optional<BigInteger> prevGasPrice = recorder.getLast().getGasPrice();
			if (prevGasPrice.isPresent()) {
				BigInteger newGasPrice = prevGasPrice.get().add(DEFAULT_GAS_PRICE_INCREMENT);
				tx.setGasPrice(newGasPrice);
			}
		}
		
		String signedTx = this.admin.sign(tx);
		String txHash = Hash.sha3(signedTx);
		
		if (!resendOnError) {
			this.handler.persistTxHashAndNonce(txHash, nonce);
		}
		
		data.updateSettlement(SettlementStatus.OffChainSettled, txHash, tx);
		
		SendTransactionResult result = resendOnError
				? cfx.sendRawTransactionAndGet(signedTx)
				: this.admin.send(signedTx);	// nonce++ if succeeded
				
		if (result.getRawError() == null) {
			this.relayTx(signedTx);
			return;
		}
		
		switch (result.getErrorType()) {
		case TxAlreadyExists:
		case InvalidNonceAlreadyUsed:
			// Sometimes, transaction will be re-sent due to temporary IO error,
			// and cause such kind of errors. Just go ahead in this case.
			this.relayTx(signedTx);
			break;
			
		case TxPoolFull:
			// In this case, client have to wait for a while and re-send transaction again.
			this.handler.onTransactionPoolFull(data);
			throw new PendingException(this.getPauseIntervalMillis(), "txpool is full");
			
		case Rlp:
		case InvalidEpochHeight:
		case InvalidChainId:
		case InvalidGasLimitExceedsMax:
		case InvalidGasLimitLessThanIntrinsic:
		case InvalidGasPriceTooSmall:
		case InvalidNonceTooStale:
		case InvalidNonceTooFuture:
		case InvalidSignature:
		case Internal:
		case Unknown:
			if (resendOnError && recorder.getReceipt(cfx).isPresent()) {
				// Failed to re-send transaction due to previous sent transaction already executed.
				// In this case, just ignore the error, and go ahead.
				break;
			} else {
				// If extreme unrecoverable case happened, pause worker and notify administrator to involve.
				this.setPaused(true);
				this.handler.onUnexpectedTransactionError(data, result);
				throw new PendingException(this.getPauseIntervalMillis(), "unexpected error occurred: %s", result.getErrorType());
			}
			
		default:
			this.setPaused(true);
			this.handler.onUnexpectedTransactionError(data, result);
			throw new PendingException(this.getPauseIntervalMillis(), "unknown error type found, result = %s", result);
		}
	}

	@Override
	protected void onFailure(Settleable data, Exception e) {
		this.handler.onException(data, e);
	}

}
