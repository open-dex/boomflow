package boomflow.worker.settle;

public enum SettlementStatus {
	/**
	 * Settled and updated off chain (e.g. database).
	 */
	OffChainSettled,
	/**
	 * Transaction sent on blockchain.
	 */
	OnChainSettled,
	/**
	 * Transaction confirmed on blockchain.
	 */
	OnChainConfirmed,
	/**
	 * Transaction execution failed on blockchain.
	 */
	OnChainFailed,
	/**
	 * Transaction execution succeeded on blockchain, but failed to 
	 * validate against event logs in receipt.
	 */
	OnChainReceiptValidationFailed,
}
