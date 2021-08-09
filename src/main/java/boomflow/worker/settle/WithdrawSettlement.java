package boomflow.worker.settle;

import java.math.BigInteger;

import boomflow.common.EncodeUtils;
import boomflow.common.worker.BatchWorker.Batchable;
import boomflow.eip712.TypedWithdraw;

/**
 * Settlement for withdraw request.
 */
public abstract class WithdrawSettlement extends Settleable {
	
	private static final String FUNCTION_NAME = "withdraw";
	public static BigInteger defaultGasLimit = BigInteger.valueOf(200000);

	protected WithdrawSettlement(String txHash, BigInteger nonce) {
		super(txHash, nonce);
	}

	@Override
	public boolean batchWith(Batchable other) {
		return false;
	}

	@Override
	public int size() {
		return 1;
	}
	
	protected abstract TypedWithdraw toTypedData();

	@Override
	public SettlementContext getSettlementContext() throws Exception {
		TypedWithdraw withdraw = this.toTypedData();
		String data = EncodeUtils.encode(FUNCTION_NAME, withdraw);
		return new SettlementContext(withdraw.domain().getVerifyingContractAddress(), data, defaultGasLimit, DEFAULT_STORAGE_LIMIT);
	}

}
