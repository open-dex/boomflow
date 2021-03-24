package boomflow.worker.settle;

import java.math.BigInteger;

import org.web3j.abi.datatypes.DynamicStruct;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Int256;
import org.web3j.abi.datatypes.generated.Uint256;

import boomflow.common.EncodeUtils;
import boomflow.common.worker.BatchWorker.Batchable;
import conflux.web3j.types.CfxAddress;

public abstract class PositionSettlement extends Settleable {
	
	private static final String FUNCTION_NAME = "updatePosition";
	private static final BigInteger DEFAULT_GAS_LIMIT = BigInteger.valueOf(100000);
	
	public static class TypedPosition extends DynamicStruct {
		
		public TypedPosition(long opType, 
				CfxAddress userAddress, CfxAddress clearAccountAddress, CfxAddress tokenAddress,
				long deltaLongAmt, long deltaShortAmt,
				long deltaLongQty, long deltaShortQty,
				long deltaLongMargin, long deltaShortMargin,
				long deltaAmount, String symbol) {
			super(new Uint256(opType), 
					userAddress.getABIAddress(), clearAccountAddress.getABIAddress(), tokenAddress.getABIAddress(),
					new Int256(deltaLongAmt), new Int256(deltaShortAmt),
					new Int256(deltaLongQty), new Int256(deltaShortQty),
					new Int256(deltaLongMargin), new Int256(deltaShortMargin),
					new Int256(deltaAmount), new Utf8String(symbol));
		}
	}

	protected PositionSettlement(String txHash, BigInteger nonce) {
		super(txHash, nonce);
	}

	// TODO batch settlement if TPS limited
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
		return SettlementContext.boomflow(data, DEFAULT_GAS_LIMIT, DEFAULT_STORAGE_LIMIT);
	}

}
