package boomflow;

import java.math.BigInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.web3j.protocol.core.Response.Error;

import boomflow.common.Address;
import boomflow.eip712.core.Domain;
import boomflow.worker.CfxSettlementWorker;
import boomflow.worker.SettlementHandler;
import boomflow.worker.SettlementWorker;
import boomflow.worker.settle.PositionSettlement;
import boomflow.worker.settle.Settleable;
import boomflow.worker.settle.SettlementStatus;
import conflux.web3j.Account;
import conflux.web3j.Cfx;
import conflux.web3j.CfxUnit;
import conflux.web3j.types.RawTransaction;

public class SettlementDemo {

	public static void main(String[] args) {
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
		
		// one extra thread required for transaction relayer
//		TransactionRelayer relayer = new TransactionRelayer(executor);
//		relayer.addServer("http://test1.confluxrpc.org");
//		relayer.addServer("http://test2.confluxrpc.org");
		
		Cfx cfx = MonitorEventLogDemo.connectTestnet();
		Account admin = Account.create(cfx, "0xbe2fbe75ed7226147fc382620c706be2bc999c00268d58ea638b1627cdee6eab");
		System.out.println("admin: " + admin.getAddress().getAddress());
		
		if (cfx.getBalance(admin.getAddress()).sendAndGet().compareTo(CfxUnit.CFX_ONE) < 0) {
			System.out.println("admin balance not enough");
			return;
		}
		
		RawTransaction.setDefaultChainId(cfx.getStatus().sendAndGet().getChainId());
		Domain.defaultName = "MiragexToken";
		Domain.defaultChainId = RawTransaction.getDefaultChainId().longValueExact();
		Domain.boomflowAddress = Address.createCfxAddress("cfxtest:acdf0hmrmzfdz29whnpvm81mk7atcfdkbex3tavgfk");
		
		SettlementWorker worker = new CfxSettlementWorker(executor, admin, new ConsoleSettlementHandler());
		
		// set tx relayer to faster tx propagation
//		worker.setTxRelayer(relayer);
		
		executor.scheduleWithFixedDelay(new Runnable() {
			
			@Override
			public void run() {
				try {
					worker.getMonitor().update();
				} catch (Exception e) {
					System.out.println("[ERROR] failed to update tx confirmation status: " + e.getMessage());
				}
			}
		}, 3000, 3000, TimeUnit.MILLISECONDS);
		
		worker.submit(new PositionSettlement(null, null) {
			
			@Override
			protected void update(SettlementStatus status, String txHash, BigInteger nonce) {
				System.out.printf("[SETTLEMENT] update, status = %s, tx = %s, nonce = %s\n", status, txHash, nonce);
			}
			
			@Override
			protected void update(SettlementStatus status) {
				System.out.println("[SETTLEMENT] update, status = " + status);
			}
			
			@Override
			protected TypedPosition toTypedData() {
				return new TypedPosition(1,
						Address.createCfxAddress("cfxtest:aar61c2b0cvt4gr2m4ytn6xe7a55s261me411kwwkc"),
						Address.createCfxAddress("cfxtest:aan02vpwvz8crpa1n10j17ufceefptdc2y9cx0zd94"),
						Address.createCfxAddress("cfxtest:achf549sa9mxsge2dtzme1mvftm5vv69h2u13t9hew"),
						1, 1, 1, 1, 1, 1, 1, "CFX/USDT");
			}
			
		});
	}

}

class ConsoleSettlementHandler implements SettlementHandler {
	
	private String lastTxHash;
	private BigInteger lastTxNonce;

	@Override
	public void persistTxHashAndNonce(String txHash, BigInteger nonce) {
		System.out.printf("[HANDLER] persistTxHashAndNonce: tx = %s, nonce = %s\n", txHash, nonce);
		this.lastTxHash = txHash;
		this.lastTxNonce = nonce;
	}

	@Override
	public String getLastTxHash() {
		return this.lastTxHash;
	}

	@Override
	public BigInteger getLastNonce() {
		return this.lastTxNonce;
	}

	@Override
	public void onTransactionFailure(Settleable data) {
		System.out.println("[HANDLER] onTransactionFailure, data = " + data);
	}

	@Override
	public void onNonceTooFuture(BigInteger offChainNonce, BigInteger onChainNonce) {
		System.out.printf("[HANDLER] onNonceTooFuture, offChain = %s, onChain = %s\n", offChainNonce, onChainNonce);
	}

	@Override
	public void onTransactionPoolFull(Settleable data) {
		System.out.println("[HANDLER] onTransactionPoolFull");
	}

	@Override
	public void onUnexpectedTransactionError(Settleable data, Error error) {
		System.out.println("[HANDLER] onUnexpectedTransactionError, erorr = " + error);
	}

	@Override
	public void onException(Settleable data, Exception e) {
		System.out.println("[HANDLER] onException, exception = " + e);
	}
	
}