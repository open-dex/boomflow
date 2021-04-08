package boomflow.worker;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.web3j.protocol.Web3j;

import boomflow.common.Utils;
import conflux.web3j.Cfx;
import conflux.web3j.RpcException;

/**
 * HeartBeat could be used to monitor the health of full node.
 * <p/>
 * It is achieved via a very lightweight RPC, e.g. getBlockNumber on ETH,
 * or getEpochNumber on Conflux network. Generally, client should schedule
 * a task with this <code>Runnable</code> object, e.g. every 1 second for
 * a private full node.
 * <p/>
 * Once the full node is unavailable, administrator should be involved to
 * recover the full node service timely.
 */
public abstract class HeartBeat implements Runnable {
	
	public static interface Handler {
		
		/**
		 * Fired when IO error occurred. Especially, when IO error occurred
		 * for a long period, e.g. 3 minutes, administrator should be involved
		 * to recover the full node timely.
		 * @param e IOException for the IO error.
		 * @param errorCounter times of continuous error, which will be set to 0
		 * when full node recovered.
		 */
		void onIoError(IOException e, int errorCounter);
		
		/**
		 * Fired when any unexpected error occurred.
		 * @param e Unexpected error.
		 * @param errorCounter times of continuous error, which will be set to 0
		 * when full node recovered.
		 */
		void onUnexpectedException(Exception e, int errorCounter);
		
	}
	
	private Handler handler;
	private AtomicReference<BigInteger> currentBlockNumber = new AtomicReference<BigInteger>(BigInteger.ZERO);
	private AtomicInteger errorCounter = new AtomicInteger();
	
	protected HeartBeat(Handler handler) {
		this.handler = handler;
	}
	
	/**
	 * Create an instance of HeartBeat on Conflux network.
	 */
	public static HeartBeat create(Cfx cfx, Handler handler) {
		return new HeartBeat(handler) {
			
			@Override
			protected BigInteger checkBlockNumber() throws RpcException {
				return cfx.getEpochNumber().sendAndGet();
			}
		};
	}
	
	/**
	 * Create an instance of HeartBeat on BSC.
	 */
	public static HeartBeat create(Web3j web3j, Handler handler) {
		return new HeartBeat(handler) {
			
			@Override
			protected BigInteger checkBlockNumber() throws IOException {
				return web3j.ethBlockNumber().send().getBlockNumber();
			}
		};
	}
	
	protected abstract BigInteger checkBlockNumber() throws IOException, RpcException;
	
	/**
	 * Returns the current pivot block number.
	 */
	public BigInteger getBlockNumber() {
		return this.currentBlockNumber.get();
	}
	
	@Override
	public void run() {
		try {
			BigInteger num = this.checkBlockNumber();
			this.currentBlockNumber.set(num);
			this.errorCounter.set(0);
		} catch (RpcException e) {
			if (Utils.isRpcError(e)) {
				this.handler.onUnexpectedException(e, this.errorCounter.incrementAndGet());
			} else {
				this.handler.onIoError((IOException) e.getCause(), this.errorCounter.incrementAndGet());
			}
		} catch (IOException e) {
			this.handler.onIoError(e, this.errorCounter.incrementAndGet());
		} catch (Exception e) {
			this.handler.onUnexpectedException(e, this.errorCounter.incrementAndGet());
		}
	}

}
