package boomflow.worker;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.tx.RawTransactionManager;
import org.web3j.utils.Strings;

import boomflow.common.EthWeb3Wrapper;
import boomflow.common.Utils;
import boomflow.common.worker.PendingException;
import boomflow.worker.settle.Settleable;
import boomflow.worker.settle.SettlementStatus;
import boomflow.worker.settle.TransactionRecorder;
import conflux.web3j.RpcException;

public class EthSettlementWorker extends SettlementWorker {
	
	private static final BigInteger CHECK_NONCE_INTERVAL = BigInteger.valueOf(100);
	private static final BigInteger CHECK_NONCE_FUTURE_THRESHOLD = BigInteger.valueOf(500);
	
	private static final BigDecimal DEFAULT_GAS_PRICE_BUMPUP = BigDecimal.valueOf(1.1); // 10%
	
	private static Logger logger = LoggerFactory.getLogger(EthSettlementWorker.class);
	
	private EthWeb3Wrapper web3j;
	private RawTransactionManager manager;

	public EthSettlementWorker(ScheduledExecutorService executor, EthWeb3Wrapper web3j, Credentials admin, SettlementHandler handler) {
		super(executor, handler, EthTransactionConfirmationMonitor.createBSC(web3j));
		
		this.web3j = web3j;
		this.manager = new RawTransactionManager(web3j.getWeb3j(), admin);
	}
	
	@Override
	public EthTransactionConfirmationMonitor getMonitor() {
		return (EthTransactionConfirmationMonitor) this.monitor;
	}
	
	private BigInteger getAdminNonce() throws RpcException {
		return this.web3j.getNonce(this.manager.getFromAddress(), DefaultBlockParameterName.PENDING);
	}

	@Override
	protected void validatePendingNonce() throws PendingException, RpcException {
		// check once every N settlements
		BigInteger offChainNonce = this.getAdminNonce();
		if (offChainNonce.divideAndRemainder(CHECK_NONCE_INTERVAL)[1].compareTo(BigInteger.ZERO) != 0) {
			return;
		}
		
		// not reached the too future threshold
		BigInteger onChainNonce = this.web3j.getNonce(this.manager.getFromAddress(), DefaultBlockParameterName.LATEST);
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
		return recorder.isTxExists(this.web3j);
	}

	@Override
	protected void sendTransaction(Settleable data) throws Exception {
		TransactionRecorder recorder = data.getRecorder();
		
		// If sent before, always use the same nonce to send again.
		// Otherwise, retrieve the pending nonce to send for the first time.
		BigInteger nonce = recorder == null ? this.getAdminNonce() : recorder.getNonce();
		
		BigInteger price = this.getBestGasPrice(recorder);
		
		RawTransaction tx = data.getSettlementContext().buildEthTx(nonce, price);
		String signedTx = this.manager.sign(tx);
		String txHash = Hash.sha3(signedTx);
	
		data.updateSettlement(SettlementStatus.OffChainSettled, txHash, tx);
		
		try {
			this.web3j.sendRawTransaction(signedTx);
		} catch (RpcException e) {
			// possible temp IO error
			if (!Utils.isRpcError(e)) {
				// It's up to heartbeat service to monitor the health of full node.
				throw new PendingException(1000, "failed to send raw transaction due to IO error");
			}
			
			String msg = e.getError().getMessage();
			if (Strings.isEmpty(msg)) {
				throw new Exception("RPC exception occurred, but the error message is null or empty", e);
			}
			
			switch (msg) {
			case "already known":
				// usually caused by re-send on temp IO error, just go ahead.
				break;
				
			case "transaction underpriced":
			case "replacement transaction underpriced":
				// mark as long unexecuted and throw again to re-send with higher gas price.
				data.getRecorder().getLast().setLongUnexecuted(true);
				throw new PendingException(300, "gas price too low");
				
			case "insufficient funds for gas * price + value":
				throw new Exception("Balance not enough to send transaction", e);
				
			case "nonce too low":
				// Transaction with same nonce already executed. In this case,
				// 1) If receipt found, just ignore this error and do not relay transaction anymore.
				// 2) Otherwise, it is the case that service restarted and previous sent information missed.
				if (!recorder.getReceipt(this.web3j).isPresent()) {
					logger.info("Transaction already executed, but cannot find the historical record");
				}
				
				return;
				
			default:
				logger.error("Unexpected RPC exception occurred: {}", e.getMessage());
				throw new Exception("Unexpected RPC exception occurred: " + msg, e);
			}
		}
		
		// Relay transactions to multiple full nodes for faster propagation.
		this.relayTx(signedTx);
	}
	
	/**
	 * Returns a best practical gas price to send transaction for the specifed data.
	 * 1) For the first time, use average gas price from blockchain.
	 * 2) When service restarted (last price info missed), use average gas price from blockchain.
	 * 3) When any error occurred, just re-send transaction with same gas price.
	 * 4) When transaction not executed for a long time, increase gas price for acceleration.
	 */
	private BigInteger getBestGasPrice(TransactionRecorder recorder) {
		// send for the first time
		if (recorder == null) {
			return this.web3j.getGasPrice();
		}
		
		Optional<BigInteger> prevGasPrice = recorder.getLast().getGasPrice();
		
		// service restarted
		if (!prevGasPrice.isPresent()) {
			return this.web3j.getGasPrice();
		}
		
		// re-send on any error
		if (!recorder.getLast().isLongUnexecuted()) {
			return prevGasPrice.get();
		}
		
		// re-send with higher gas price for acceleration
		BigInteger bumpupPrice = new BigDecimal(prevGasPrice.get()).multiply(DEFAULT_GAS_PRICE_BUMPUP).toBigInteger();
		return this.web3j.getGasPrice().max(bumpupPrice);
	}

}
