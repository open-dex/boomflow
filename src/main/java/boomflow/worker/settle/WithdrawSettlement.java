package boomflow.worker.settle;

import java.math.BigInteger;

import org.web3j.abi.datatypes.DynamicBytes;

import boomflow.common.EncodeUtils;
import boomflow.common.worker.BatchWorker.Batchable;
import boomflow.eip712.TypedWithdraw;

/**
 * Settlement for withdraw request.
 */
public abstract class WithdrawSettlement extends Settleable {
	
	private static final String FUNCTION_NAME = "withdraw";
	private static final BigInteger DEFAULT_GAS_LIMIT = BigInteger.valueOf(200000);

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
		DynamicBytes signature = EncodeUtils.hex2Bytes(withdraw.signature());
		String data = EncodeUtils.encode(FUNCTION_NAME, withdraw, signature);
		return new SettlementContext(withdraw.domain().getVerifyingContractAddress(), data, DEFAULT_GAS_LIMIT, DEFAULT_STORAGE_LIMIT);
	}

}
