package boomflow.worker.settle;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import conflux.web3j.Cfx;
import conflux.web3j.RpcException;
import conflux.web3j.response.Receipt;
import conflux.web3j.response.Transaction;
import conflux.web3j.types.RawTransaction;

/**
 * TransactionRecorder records transactions for a single settlement.
 * 
 * When transaction discarded or unpacked for a long time, DEX will re-send
 * transaction to RPC server with same nonce and higher gas price.
 * 
 * Not sure which transaction will be packed by miner, so TransactionRecorder
 * will track all transactions for a single settlement.
 */
public class TransactionRecorder {
	
	// use same nonce to re-send transactions
	private BigInteger nonce = BigInteger.ZERO;
	
	// track all re-sent transactions
	private List<Record> records = new ArrayList<Record>();
	
	public TransactionRecorder(String txHash, RawTransaction tx) {
		this.addRecord(txHash, tx);
	}

	// in case of service restarted, and database only records
	// the tx hash and nonce.
	public TransactionRecorder(String txHash, BigInteger nonce) {
		this.nonce = nonce;
		this.records.add(new Record(txHash));
	}
	
	public BigInteger getNonce() {
		return nonce;
	}
	
	public void addRecord(String txHash, RawTransaction tx) {
		// in case of service restarted and admin nonce changed since last settlement
		if (this.nonce.compareTo(tx.getNonce()) < 0) {
			this.nonce = tx.getNonce();
		}
		
		this.records.add(new Record(txHash, tx));
	}
	
	public Record getLast() {
		return this.records.get(this.records.size() - 1);
	}
	
	/**
	 * Try to get receipt for all sent transactions.
	 */
	public Optional<Receipt> getReceipt(Cfx cfx) throws RpcException {
		for (Record record : this.records) {
			Optional<Receipt> receipt = cfx.getTransactionReceipt(record.getTxHash()).sendAndGet();
			if (receipt.isPresent()) {
				return receipt;
			}
		}
		
		return Optional.empty();
	}
	
	public Optional<Transaction> getTransaction(Cfx cfx) throws RpcException {
		int len = this.records.size();
		for (int i = len - 1; i >= 0; i--) {
			String txHash = this.records.get(i).getTxHash();
			Optional<Transaction> tx = cfx.getTransactionByHash(txHash).sendAndGet();
			if (tx.isPresent()) {
				return tx;
			}
		}
		
		return Optional.empty();
	}

	public static class Record {
		private String txHash;
		private Optional<BigInteger> gasPrice = Optional.empty();
		private Optional<BigInteger> epoch = Optional.empty();
		private boolean longUnexecuted = false;
		
		public Record(String txHash) {
			this.txHash = txHash;
		}
		
		public Record(String txHash, RawTransaction tx) {
			this.txHash = txHash;
			this.gasPrice = Optional.of(tx.getGasPrice());
			this.epoch = Optional.of(tx.getEpochHeight());
		}
		
		public String getTxHash() {
			return txHash;
		}
		
		public Optional<BigInteger> getGasPrice() {
			return gasPrice;
		}
		
		public Optional<BigInteger> getEpoch() {
			return epoch;
		}
		
		public void setEpoch(BigInteger epoch) {
			this.epoch = Optional.of(epoch);
		}
		
		public boolean isLongUnexecuted() {
			return longUnexecuted;
		}
		
		public void setLongUnexecuted(boolean longUnexecuted) {
			this.longUnexecuted = longUnexecuted;
		}
	}

}
