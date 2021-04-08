package boomflow.worker.settle;

import java.math.BigInteger;

import boomflow.common.Address;
import boomflow.eip712.core.Domain;
import conflux.web3j.types.CfxAddress;
import conflux.web3j.types.RawTransaction;

public class SettlementContext {
	
	private Address contract;
	private String data;
	private BigInteger gasLimit;
	private BigInteger storageLimit;
	
	public SettlementContext(Address contract, String data, BigInteger gasLimit, BigInteger storageLimit) {
		this.contract = contract;
		this.data = data;
		this.gasLimit = gasLimit;
		this.storageLimit = storageLimit;
	}
	
	public static SettlementContext boomflow(String data, BigInteger gasLimit, BigInteger storageLimit) {
		return new SettlementContext(Domain.boomflow().getVerifyingContractAddress(), data, gasLimit, storageLimit);
	}
	
	public RawTransaction buildCfxTx(BigInteger nonce, BigInteger epoch) {
		CfxAddress contract = new CfxAddress(this.contract.toString());
		return RawTransaction.call(nonce, this.gasLimit, contract, this.storageLimit, epoch, this.data);
	}
	
	public org.web3j.crypto.RawTransaction buildEthTx(BigInteger nonce, BigInteger gasPrice) {
		return org.web3j.crypto.RawTransaction.createTransaction(nonce, gasPrice, this.gasLimit, this.contract.toHex(), this.data);
	}

}
