package boomflow.worker.settle;

import java.math.BigInteger;

import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.StaticStruct;
import org.web3j.abi.datatypes.generated.Uint256;

import boomflow.common.Address;
import boomflow.common.EncodeUtils;
import boomflow.common.worker.BatchWorker.Batchable;
import boomflow.eip712.TypedWithdraw;

/**
 * Settlement for withdraw request.
 */
public abstract class WithdrawSettlement extends Settleable {
	
	public static class TypedGasFee extends StaticStruct {
		
		public TypedGasFee(Address token, BigInteger amount, Address recipient) {
			super(token.toABI(), new Uint256(amount), recipient.toABI());
		}
	}
	
	private static final String FUNCTION_NAME = "withdraw";
	public static BigInteger defaultGasLimit = BigInteger.valueOf(200000);
	
	private TypedGasFee gasFee;

	protected WithdrawSettlement(String txHash, BigInteger nonce, TypedGasFee gasFee) {
		super(txHash, nonce);
		
		this.gasFee = gasFee;
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
		String data = EncodeUtils.encode(FUNCTION_NAME, withdraw, signature, this.gasFee);
		return new SettlementContext(withdraw.domain().getVerifyingContractAddress(), data, defaultGasLimit, DEFAULT_STORAGE_LIMIT);
	}

}
