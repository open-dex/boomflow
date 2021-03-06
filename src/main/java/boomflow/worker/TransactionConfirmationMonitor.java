package boomflow.worker;

import java.math.BigInteger;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.web3j.protocol.core.methods.response.TransactionReceipt;

import boomflow.common.EthWeb3Wrapper;
import boomflow.event.Event;
import boomflow.worker.settle.Settleable;
import boomflow.worker.settle.SettlementStatus;
import boomflow.worker.settle.TransactionRecorder;
import conflux.web3j.Cfx;
import conflux.web3j.RpcException;
import conflux.web3j.request.Epoch;
import conflux.web3j.response.Receipt;

/**
 * Monitor the confirmation status of transactions sent on chain.
 * 
 * When service restarted, application should reload on chain settled data 
 * and add all of them to queue to continue confiramtion status monitor.
 */
public abstract class TransactionConfirmationMonitor {
	
	private AtomicBoolean paused = new AtomicBoolean();
	
	/**
	 * Maximum number of pivot blocks since transaction sent to confirm a transaction.
	 * Once exceeded, transaction should be re-send with higher gas price.
	 */
	private AtomicReference<BigInteger> confirmBlocksThreshold = new AtomicReference<BigInteger>();
	
	/**
	 * Pending data (nonce => Settleable) to confirm transactions on chain.
	 */
	private ConcurrentNavigableMap<BigInteger, Settleable> items = new ConcurrentSkipListMap<BigInteger, Settleable>();
	
	/**
	 * Fired when transaction not executed for a long time.
	 */
	Event<Settleable> onTxLongUnexecuted = new Event<Settleable>();
	
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
	Event<Settleable> onTxFailed = new Event<Settleable>();
	
	protected TransactionConfirmationMonitor(long confirmBlocksThreshold) {
		this.setConfirmBlocksThreshold(confirmBlocksThreshold);
	}
	
	/**
	 * Indicates whether the monitor is enabled to work. When any transaction
	 * execution failed, the monitor should be paused to prevent more failures.
	 */
	public boolean isPaused() {
		return this.paused.get();
	}
	
	/**
	 * Pause or unpause the monitor.
	 */
	public void setPaused(boolean paused) {
		this.paused.set(paused);
	}
	
	/**
	 * Returns the maximum number of pivot blocks since transaction sent to confirm a transaction.
	 */
	public long getConfirmBlocksThreshold() {
		return this.confirmBlocksThreshold.get().longValueExact();
	}
	
	/**
	 * Sets the maximum number of pivot blocks since transaction sent to confirm a transaction.
	 */
	public void setConfirmBlocksThreshold(long confirmBlocksThreshold) {
		this.confirmBlocksThreshold.set(BigInteger.valueOf(confirmBlocksThreshold));
	}
	
	/**
	 * Returns the pending data by transaction nonce.
	 */
	public Settleable get(BigInteger nonce) {
		return this.items.get(nonce);
	}
	
	/**
	 * Returns all pending data.
	 */
	public NavigableMap<BigInteger, Settleable> getItems() {
		return new TreeMap<BigInteger, Settleable>(this.items);
	}
	
	/**
	 * Returns the number of pending data.
	 */
	public int getPendingCount() {
		return this.items.size();
	}
	
	/**
	 * Removes a pending data by transaction nonce.
	 */
	public Settleable remove(BigInteger nonce) {
		return this.items.remove(nonce);
	}
	
	protected abstract BigInteger getBlockNumber() throws RpcException;
	protected abstract BigInteger getConfirmedBlockNumber() throws RpcException;
	protected abstract CheckConfirmationResult checkConfirmation(Settleable settleable, BigInteger confirmedBlock) throws RpcException;
	
	/**
	 * Append data in queue to check transaction confirmation status.
	 */
	public void add(Settleable item) throws RpcException {
		TransactionRecorder recorder = item.getRecorder();
		if (recorder == null) {
			return;
		}
		
		if (!recorder.getLast().getBlockNumber().isPresent()) {
			BigInteger blockNumber = this.getBlockNumber();
			recorder.getLast().setBlockNumber(blockNumber);
		}
		
		this.items.put(recorder.getNonce(), item);
	}
	
	/**
	 * Update the transaction confirmation status in queue. Once confirmed, remove from queue.
	 * Otherwise, update for the next time.
	 * 
	 * @return the number of transactions that already confirmed on chain.
	 */
	public int update() throws RpcException {
		if (this.isPaused() || this.items.isEmpty()) {
			return 0;
		}
		
		BigInteger confirmedBlock = this.getConfirmedBlockNumber();
		CheckConfirmationResult result = CheckConfirmationResult.Confirmed;
		int numConfirmed = 0;
		
		while (!this.isPaused() && !this.items.isEmpty() && result == CheckConfirmationResult.Confirmed) {
			Settleable settleable = this.items.firstEntry().getValue();
			
			// break out if not confirmed yet
			BigInteger sentBlock = settleable.getRecorder().getLast().getBlockNumber().get();
			if (sentBlock.compareTo(confirmedBlock) > 0) {
				break;
			}
			
			result = this.checkConfirmation(settleable, confirmedBlock);
			boolean removeMonitorItem = true;
			
			switch (result) {
			case NotExecuted:
				removeMonitorItem = this.onTxNotExecuted(settleable, sentBlock, confirmedBlock);
				break;
			case ReceiptValidationFailed:
				this.onTxValidationFailed(settleable);
				break;
			case ExecutionFailed:
				this.onTxFailed(settleable);
				break;
			case NotConfirmed:
				removeMonitorItem = false;
				break;
			case Confirmed:
				settleable.updateSettlement(SettlementStatus.OnChainConfirmed);
				numConfirmed++;
				break;
			default:
				break;
			}
			
			if (removeMonitorItem) {
				this.items.pollFirstEntry();
			}
		}
		
		return numConfirmed;
	}
	
	/*
	 * There are several cases that transaction not packed in recent confirmed epoch:
	 * 
	 * 1) RPC server received transaction, but not propagate it to mining node timely.
	 * 2) RPC server received transaction, but not propagate it to mining node due to
	 * bug or temporary network issue.
	 * 3) Any unexpected issue in transaction pool of full node.
	 * 4) Gas price is too low in case of too many pending transactions.
	 * 
	 * Now, there is no way to judge the reason, so just wait for a longer time,
	 * e.g. 5 minutes or N blocks, and check the transaction again. If transaction still
	 * unpacked, then DEX need to re-send the transaction with original nonce and higher
	 * gas price.
	 * 
	 * Return true if requires to remove the data from queue. Otherwise, false.
	 */
	private boolean onTxNotExecuted(Settleable settleable, BigInteger sentBlock, BigInteger confirmedBlock) {
		BigInteger elapsedBlocks = confirmedBlock.subtract(sentBlock);
		
		// continue to wait for transaction execution
		if (this.confirmBlocksThreshold.get().compareTo(elapsedBlocks) > 0) {
			return false;
		}
		
		// try to re-send the unpacked transaction
		settleable.getRecorder().getLast().setLongUnexecuted(true);		
		this.onTxLongUnexecuted.fire(settleable);
		
		return true;
	}
	
	private void onTxValidationFailed(Settleable settleable) {
		settleable.updateSettlement(SettlementStatus.OnChainReceiptValidationFailed);
		this.setPaused(true);
		this.onTxFailed.fire(settleable);
	}
	
	private void onTxFailed(Settleable settleable) {
		settleable.updateSettlement(SettlementStatus.OnChainFailed);
		
		if (!settleable.suppressOnChainFailure()) {
			this.setPaused(true);
			this.onTxFailed.fire(settleable);
		}
	}
	
	public CheckConfirmationResult checkConfirmation(BigInteger nonce) throws RpcException {
		Settleable settleable = this.items.get(nonce);
		if (settleable == null) {
			return null;
		}
		
		BigInteger confirmedBlock = this.getConfirmedBlockNumber();
		
		return this.checkConfirmation(settleable, confirmedBlock);
	}
	
	enum CheckConfirmationResult {
		NotExecuted,
		ExecutionFailed,
		ReceiptValidationFailed,
		NotConfirmed,
		Confirmed,
	}
}

class CfxTransactionConfirmationMonitor extends TransactionConfirmationMonitor {
	
	private static final long DEFAULT_CONFIRM_EPOCHS_THRESHOLD = 200;
	
	private Cfx cfx;
	
	/**
	 * Extra number of epochs before confirmed epoch to check transaction confirmation.
	 */
	private AtomicReference<BigInteger> extraConfirmEpochs = new AtomicReference<BigInteger>(BigInteger.ZERO);

	public CfxTransactionConfirmationMonitor(Cfx cfx) {
		super(DEFAULT_CONFIRM_EPOCHS_THRESHOLD);
		
		this.cfx = cfx;
	}
	
	/**
	 * Returns the extra number of epochs before confirmed epoch to check transaction confirmation.
	 */
	public long getExtraConfirmEpochs() {
		return this.extraConfirmEpochs.get().longValueExact();
	}
	
	/**
	 * Sets the extra number of epochs before confirmed epoch to check transaction confirmation.
	 */
	public void setExtraConfirmEpochs(long extraConfirmEpochs) {
		this.extraConfirmEpochs.set(BigInteger.valueOf(extraConfirmEpochs));
	}
	
	@Override
	protected BigInteger getBlockNumber() throws RpcException {
		return this.cfx.getEpochNumber().sendAndGet();
	}

	@Override
	protected BigInteger getConfirmedBlockNumber() throws RpcException {
		BigInteger epoch = this.cfx.getEpochNumber(Epoch.latestConfirmed()).sendAndGet();
		return this.extraConfirmEpochs.get().add(epoch);
	}

	@Override
	protected CheckConfirmationResult checkConfirmation(Settleable settleable, BigInteger confirmedBlock) throws RpcException {
		Optional<Receipt> maybeReceipt = settleable.getRecorder().getReceipt(this.cfx);
		
		// transaction not executed yet
		if (!maybeReceipt.isPresent()) {
			return CheckConfirmationResult.NotExecuted;
		}
		
		Receipt receipt = maybeReceipt.get();
		
		// Multiple transactions sent, but not the last one packed.
		// In this case, need to update the txHash in database.
		String packedTxHash = receipt.getTransactionHash();
		if (!settleable.getRecorder().getLast().getTxHash().equalsIgnoreCase(packedTxHash)) {
			settleable.updateSettlement(packedTxHash);
		}

		if (receipt.getOutcomeStatus() != 0) {
			return CheckConfirmationResult.ExecutionFailed;
		}
		
		if (!settleable.matches(receipt)) {
			return CheckConfirmationResult.ReceiptValidationFailed;
		}
		
		return receipt.getEpochNumber().compareTo(confirmedBlock) <= 0
				? CheckConfirmationResult.Confirmed
				: CheckConfirmationResult.NotConfirmed;
	}
	
}

class EthTransactionConfirmationMonitor extends TransactionConfirmationMonitor {
	
	private EthWeb3Wrapper web3j;
	private BigInteger confirmBlocks;

	public EthTransactionConfirmationMonitor(EthWeb3Wrapper web3j, int confirmThreshold, int confirmBlocks) {
		super(confirmThreshold);
		
		this.web3j = web3j;
		this.confirmBlocks = BigInteger.valueOf(confirmBlocks);
	}
	
	public static EthTransactionConfirmationMonitor createBSC(EthWeb3Wrapper web3j) {
		return new EthTransactionConfirmationMonitor(web3j, 50, 15);
	}

	@Override
	protected BigInteger getBlockNumber() throws RpcException {
		return this.web3j.getBlockNumber();
	}

	@Override
	protected BigInteger getConfirmedBlockNumber() throws RpcException {
		BigInteger blockNumber = this.getBlockNumber();
		return this.confirmBlocks.compareTo(blockNumber) < 0
				? blockNumber.subtract(this.confirmBlocks)
				: BigInteger.ZERO;
	}

	@Override
	protected CheckConfirmationResult checkConfirmation(Settleable settleable, BigInteger confirmedBlock) throws RpcException {
		Optional<TransactionReceipt> maybeReceipt = settleable.getRecorder().getReceipt(this.web3j);
		
		// transaction not executed yet
		if (!maybeReceipt.isPresent()) {
			return CheckConfirmationResult.NotExecuted;
		}
		
		TransactionReceipt receipt = maybeReceipt.get();
		
		// Multiple transactions sent, but not the last one packed.
		// In this case, need to update the txHash in database.
		String packedTxHash = receipt.getTransactionHash();
		if (!settleable.getRecorder().getLast().getTxHash().equalsIgnoreCase(packedTxHash)) {
			settleable.updateSettlement(packedTxHash);
		}

		if (!receipt.isStatusOK()) {
			return CheckConfirmationResult.ExecutionFailed;
		}
		
		if (!settleable.matches(receipt)) {
			return CheckConfirmationResult.ReceiptValidationFailed;
		}
		
		return receipt.getBlockNumber().compareTo(confirmedBlock) <= 0
				? CheckConfirmationResult.Confirmed
				: CheckConfirmationResult.NotConfirmed;
	}
	
}
