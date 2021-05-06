package boomflow.worker;

import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import org.web3j.crypto.Hash;

import boomflow.common.worker.PendingException;
import boomflow.worker.settle.Settleable;
import boomflow.worker.settle.SettlementStatus;
import boomflow.worker.settle.TransactionRecorder;
import conflux.web3j.Account;
import conflux.web3j.Cfx;
import conflux.web3j.CfxUnit;
import conflux.web3j.RpcException;
import conflux.web3j.types.RawTransaction;
import conflux.web3j.types.SendTransactionResult;

public class CfxSettlementWorker extends SettlementWorker {
	
	private static final BigInteger CHECK_NONCE_INTERVAL = BigInteger.valueOf(100);
	private static final BigInteger CHECK_NONCE_FUTURE_THRESHOLD = BigInteger.valueOf(1000);
	
	private Account admin;
	
	public CfxSettlementWorker(ScheduledExecutorService executor, Account admin, SettlementHandler handler) {
		super(executor, handler, new CfxTransactionConfirmationMonitor(admin.getCfx()));
		
		this.admin = admin;
		
		new CfxNonceSyncer(admin).sync(handler);
	}
	
	@Override
	public CfxTransactionConfirmationMonitor getMonitor() {
		return (CfxTransactionConfirmationMonitor) this.monitor;
	}

	@Override
	protected void validatePendingNonce() throws PendingException, RpcException {
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

	@Override
	protected boolean isSettledOnChain(Settleable settleable) throws RpcException {
		TransactionRecorder recorder = settleable.getRecorder();
		
		// not settled on chain yet
		if (recorder == null) {
			return false;
		}
		
		// need to re-send transaction if not executed for a long time.
		if (recorder.getLast().isLongUnexecuted()) {
			return false;
		}
		
		// When service restarted, the last item may not be settled on chain yet.
		// E.g. program crashed before transaction sent to full node.
		return recorder.isTxExists(this.admin.getCfx());
	}

	@Override
	protected void sendTransaction(Settleable data) throws Exception {
		TransactionRecorder recorder = data.getRecorder();
		boolean resend = recorder != null && recorder.getLast().isLongUnexecuted();
		
		BigInteger nonce = resend ? recorder.getNonce() : this.admin.getNonce();
		
		Cfx cfx = this.admin.getCfx();
		BigInteger epoch = cfx.getEpochNumber().sendAndGet();
		RawTransaction tx = data.getSettlementContext().buildCfxTx(nonce, epoch);
		
		// increase gas price if re-send transaction.
		if (resend) {
			this.increaseGasPriceOnResend(tx, recorder);
		}
		
		String signedTx = this.admin.sign(tx);
		String txHash = Hash.sha3(signedTx);
		
		if (!resend) {
			this.handler.persistTxHashAndNonce(txHash, nonce);
		}
		
		data.updateSettlement(SettlementStatus.OffChainSettled, txHash, tx);
		
		SendTransactionResult result = resend
				? cfx.sendRawTransactionAndGet(signedTx)
				: this.admin.send(signedTx);	// nonce++ if succeeded
		
		// succeeded to send transaction
		if (result.getRawError() == null) {
			this.relayTx(signedTx);
			return;
		}
		
		// any error occurred
		switch (result.getErrorType()) {
		case TxAlreadyExists:
		case InvalidNonceAlreadyUsed:
		case InvalidNonceTooStale:
			// Sometimes, transaction will be re-sent due to temporary IO error,
			// and cause TxAlreadyExists error. Just go ahead in this case.
			if (resend && recorder.getReceipt(cfx).isPresent()) {
				// Failed to re-send transaction due to previous sent transaction already executed.
				// In this case, just ignore the error, and go ahead.
			} else {
				this.relayTx(signedTx);
			}
			
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
		case InvalidNonceTooFuture:
		case InvalidSignature:
		case Internal:
		case Unknown:
			// If extreme unrecoverable case happened, pause worker and notify administrator to involve.
			this.setPaused(true);
			this.handler.onUnexpectedTransactionError(data, result.getRawError());
			throw new PendingException(this.getPauseIntervalMillis(), "unexpected error occurred: %s", result.getErrorType());
			
		default:
			this.setPaused(true);
			this.handler.onUnexpectedTransactionError(data, result.getRawError());
			throw new PendingException(this.getPauseIntervalMillis(), "unknown error type found, result = %s", result);
		}
	}
	
	private void increaseGasPriceOnResend(RawTransaction tx, TransactionRecorder recorder) {
		Optional<BigInteger> maybePrevGasPrice = recorder.getLast().getGasPrice();
		if (!maybePrevGasPrice.isPresent()) {
			return;
		}
		
		BigInteger prevGasPrice = maybePrevGasPrice.get();
		BigInteger newGasPrice = prevGasPrice.longValue() <= 1_000_000
				? prevGasPrice.multiply(BigInteger.valueOf(1000))
				: prevGasPrice.add(CfxUnit.GDRIP_ONE);
		
		tx.setGasPrice(newGasPrice);
	}

}
