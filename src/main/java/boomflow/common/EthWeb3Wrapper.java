package boomflow.common;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.EthLog.LogObject;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import conflux.web3j.RpcException;

public class EthWeb3Wrapper {
	
	private Web3j web3j;
	
	private int retryCount;
	private long retryIntervalMillis;
	
	public EthWeb3Wrapper(String url) {
		this(url, 0, 1000);
	}
	
	public EthWeb3Wrapper(String url, int retryCount, long retryIntervalMillis) {
		this(url, retryCount, retryIntervalMillis, 3000);
	}
	
	public EthWeb3Wrapper(String url, int retryCount, long retryIntervalMillis, long callTimeoutMillis) {
		this.web3j = Web3j.build(new CfxBuilder(url).withCallTimeout(callTimeoutMillis).buildWeb3jService());
		this.retryCount = retryCount;
		this.retryIntervalMillis = retryIntervalMillis;
	}
	
	public Web3j getWeb3j() {
		return web3j;
	}
	
	private <T extends Response<?>> T sendWithRetry(Request<?, T> request) throws RpcException {
		int retry = this.retryCount;
		
		while (true) {
			try {
				return request.send();
			} catch (IOException e) {
				if (retry <= 0) {
					throw RpcException.sendFailure(e);
				}
				
				retry--;
				
				if (this.retryIntervalMillis > 0) {
					try {
						Thread.sleep(this.retryIntervalMillis);
					} catch (InterruptedException e1) {
						throw RpcException.interrupted(e1);
					}
				}
			}
		}
	}
	
	private void throwOnError(Response<?> response) throws RpcException {
		if (response.hasError()) {
			throw new RpcException(response.getError());
		}
	}
	
	public BigInteger getGasPrice() throws RpcException {
		EthGasPrice response = this.sendWithRetry(this.web3j.ethGasPrice());
		this.throwOnError(response);
		return response.getGasPrice();
	}
	
	public BigInteger getBlockNumber() throws RpcException {
		EthBlockNumber response = this.sendWithRetry(this.web3j.ethBlockNumber());
		this.throwOnError(response);
		return response.getBlockNumber();
	}
	
	public List<Log> getLogs(EthFilter filter) throws RpcException {
		EthLog response = this.sendWithRetry(this.web3j.ethGetLogs(filter));
		this.throwOnError(response);
		return response.getLogs().stream()
				.map(l -> ((LogObject) l).get())
				.collect(Collectors.toList());
	}
	
	public String sendRawTransaction(String signedTx) throws RpcException {
		EthSendTransaction response = this.sendWithRetry(this.web3j.ethSendRawTransaction(signedTx));
		this.throwOnError(response);
		return response.getTransactionHash();
	}
	
	public Optional<TransactionReceipt> getReceipt(String txHash) throws RpcException {
		EthGetTransactionReceipt response = this.sendWithRetry(this.web3j.ethGetTransactionReceipt(txHash));
		this.throwOnError(response);
		return response.getTransactionReceipt();
	}
	
	public Optional<Transaction> getTransaction(String txHash) throws RpcException {
		EthTransaction response = this.sendWithRetry(this.web3j.ethGetTransactionByHash(txHash));
		this.throwOnError(response);
		return response.getTransaction();
	}
	
	public BigInteger getNonce(String address, DefaultBlockParameter block) throws RpcException {
		EthGetTransactionCount response = this.sendWithRetry(this.web3j.ethGetTransactionCount(address, block));
		this.throwOnError(response);
		return response.getTransactionCount();
	}

}
