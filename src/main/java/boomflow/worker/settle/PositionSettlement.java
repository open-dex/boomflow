package boomflow.worker.settle;

import java.math.BigInteger;

import org.web3j.abi.datatypes.DynamicStruct;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Int256;
import org.web3j.abi.datatypes.generated.Uint256;

import boomflow.common.Address;
import boomflow.common.EncodeUtils;
import boomflow.common.worker.BatchWorker.Batchable;

public abstract class PositionSettlement extends Settleable {
	
	private static final String FUNCTION_NAME = "updatePosition";
	public static BigInteger defaultGasLimit = BigInteger.valueOf(150000);
	
	public static class TypedPosition extends DynamicStruct {
		
		public TypedPosition(long opType, 
				Address userAddress, Address clearAccountAddress, Address tokenAddress,
				BigInteger deltaLongAmt, BigInteger deltaShortAmt,
				BigInteger deltaLongQty, BigInteger deltaShortQty,
				BigInteger deltaLongMargin, BigInteger deltaShortMargin,
				BigInteger deltaAmount, String symbol,
				BigInteger fee, Address gasFeeToken, BigInteger gasFee) {
			super(new Uint256(opType), 
					userAddress.toABI(), clearAccountAddress.toABI(), tokenAddress.toABI(),
					new Int256(deltaLongAmt), new Int256(deltaShortAmt),
					new Int256(deltaLongQty), new Int256(deltaShortQty),
					new Int256(deltaLongMargin), new Int256(deltaShortMargin),
					new Int256(deltaAmount), new Utf8String(symbol),
					new Uint256(fee), gasFeeToken.toABI(), new Uint256(gasFee));
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
		return SettlementContext.boomflow(data, defaultGasLimit, DEFAULT_STORAGE_LIMIT);
	}

}
