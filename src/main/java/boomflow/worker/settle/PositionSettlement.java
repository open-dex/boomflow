package boomflow.worker.settle;

import java.math.BigInteger;

import boomflow.common.EncodeUtils;
import boomflow.common.worker.BatchWorker.Batchable;
import boomflow.eip712.TypedPosition;

public abstract class PositionSettlement extends Settleable {
	
	private static final String FUNCTION_NAME = "updatePosition";
	public static BigInteger defaultGasLimit = BigInteger.valueOf(150000);

	protected PositionSettlement(String txHash, BigInteger nonce) {
		super(txHash, nonce);
	}

	// batch settlement if TPS limited
	@Override
	public boolean batchWith(Batchable other) {
		return false;
	}

	@Override
	public int size() {
		return 1;
	}
	
	protected abstract TypedPosition toTypedData();

	@Override
	public SettlementContext getSettlementContext() throws Exception {
		TypedPosition position = this.toTypedData();
		String data = EncodeUtils.encode(FUNCTION_NAME, position);
		return SettlementContext.boomflow(data, defaultGasLimit, DEFAULT_STORAGE_LIMIT);
	}

}
