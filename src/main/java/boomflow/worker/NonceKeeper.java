package boomflow.worker;

import java.math.BigInteger;

import org.web3j.utils.Strings;

import conflux.web3j.Account;

/**
 * NonceKeeper tracks the last transaction hash and nonce sent to full node, so that
 * program could easily sync up with the nonce when service restarted unexpectedly,
 * e.g. powered off and recovered in a very short time, or killed by OS unexpectedly.
 */
public interface NonceKeeper {
	
	/**
	 * Persist transaction hash and nonce before transaction sent to full node. Note,
	 * 2 values should be persisted in a transaction.
	 * 
	 * In rare case, program maybe crashed before or after transaction sent to full node.
	 * When service restarted immediately, program should sync up with the nonce between
	 * on-chain and off-chain.
	 * 
	 * In practice, application could persist the transaction hash and nonce before transaction
	 * sent to full node. Once service restarted, system could adjust the nonce based on the
	 * status of last sent transaction.
	 * 
	 * @param txHash last transaction hash sent to full node.
	 * @param nonce last transaction nonce sent to full node.
	 */
	void persistTxHashAndNonce(String txHash, BigInteger nonce);
	
	String getLastTxHash();
	BigInteger getLastNonce();

}

abstract class NonceSyncer {
	
	protected abstract long getOnChainNonce();
	protected abstract void onNonceMismatch(String lastTxHash, BigInteger lastNonce);
	
	public void sync(NonceKeeper keeper) {
		String lastTxHash = keeper.getLastTxHash();
		if (Strings.isEmpty(lastTxHash)) {
			return;
		}
		
		BigInteger lastNonce = keeper.getLastNonce();
		if (lastNonce == null) {
			return;
		}
		
		long nonceOffChain = lastNonce.longValueExact();
		long nonceOnChain = this.getOnChainNonce();
		
		// account has been used to send transaction outside.
		if (nonceOnChain > nonceOffChain + 1) {
			return;
		}
		
		// last transaction sent to full node successfully.
		if (nonceOnChain == nonceOffChain + 1) {
			return;
		}
		
		this.onNonceMismatch(lastTxHash, lastNonce);
	}

}

class CfxNonceSyncer extends NonceSyncer {
	
	private Account admin;
	
	public CfxNonceSyncer(Account admin) {
		this.admin = admin;
	}

	@Override
	protected long getOnChainNonce() {
		return this.admin.getNonce().longValueExact();
	}

	@Override
	protected void onNonceMismatch(String lastTxHash, BigInteger lastNonce) {
		boolean lastTxSent = this.admin.getCfx().getTransactionByHash(lastTxHash).sendAndGet().isPresent();
		
		if (lastTxSent) {
			this.admin.setNonce(lastNonce.add(BigInteger.ONE));
		} else {
			this.admin.setNonce(lastNonce);
		}
	}
	
}
