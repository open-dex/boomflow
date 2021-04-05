package boomflow.worker.settle;

import java.math.BigInteger;

import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Strings;

import boomflow.common.Utils;
import boomflow.common.worker.BatchWorker.Batchable;
import conflux.web3j.response.Receipt;
import conflux.web3j.types.RawTransaction;

/**
 * Inherited from any object that could be settled on chain.
 */
public abstract class Settleable implements Batchable {
	
	static final BigInteger DEFAULT_STORAGE_LIMIT = BigInteger.valueOf(2048);
	
	private SettlementStatus status = SettlementStatus.OffChainSettled;
	private TransactionRecorder recorder;
	private String executedTxHash;
	
	protected Settleable(String txHash, BigInteger nonce) {
		if (!Strings.isEmpty(txHash) && nonce != null && nonce.compareTo(BigInteger.ZERO) >= 0) {
			this.recorder = new TransactionRecorder(txHash, nonce);
		}
	}
	
	public SettlementStatus getStatus() {
		return status;
	}
	
	public TransactionRecorder getRecorder() {
		return recorder;
	}
	
	public abstract SettlementContext getSettlementContext() throws Exception;
	
	protected abstract void update(SettlementStatus status);
	protected abstract void update(SettlementStatus status, String txHash, BigInteger nonce);

	public void updateSettlement(SettlementStatus status) {
		if (this.status == status) {
			return;
		}
		
		this.update(status);
		
		this.status = status;
	}
	
	public void updateSettlement(String txHash) {
		if (this.executedTxHash != null && this.executedTxHash.equalsIgnoreCase(txHash)) {
			return;
		}
		
		this.update(this.status, txHash, this.recorder.getNonce());
		
		this.executedTxHash = txHash;
	}
	
	public void updateSettlement(SettlementStatus status, String txHash, RawTransaction tx) {
		this.update(status, txHash, tx.getNonce());
		
		this.status = status;
		
		if (this.recorder == null) {
			this.recorder = new TransactionRecorder(txHash, tx);
		} else {
			this.recorder.addRecord(txHash, tx);
		}
	}
	
	/**
	 * Indicates whether the specified transaction receipt matches the off-chain values.
	 */
	public boolean matches(Receipt receipt) {
		// check nothing by default
		return true;
	}
	
	public boolean matches(TransactionReceipt receipt) {
		// check nothing by default
		return true;
	}
	
	/**
	 * Indicates whether to suppress the transaction execution failure.
	 * 
	 * E.g. cross-chain withdraw failed due to fee increased during transaction execution.
	 */
	public boolean suppressOnChainFailure() {
		return false;
	}
	
	@Override
	public String toString() {
		return Utils.toJson(this);
	}

}
