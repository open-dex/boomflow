package boomflow.worker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import org.web3j.utils.Strings;

import boomflow.common.CfxBuilder;
import boomflow.common.EthWeb3Wrapper;
import boomflow.common.Utils;
import boomflow.common.worker.AsyncWorker;
import boomflow.common.worker.PendingException;
import conflux.web3j.Cfx;
import conflux.web3j.RpcException;

/**
 * TransactionRelayer sends transactions to multiple RPC servers, so that
 * temporary network issue will not lead to transaction not propagated timely
 * or even not propagated out.
 */
public abstract class TransactionRelayer extends AsyncWorker<String> {
	
	private Map<String, Server> servers = new ConcurrentHashMap<String, Server>();
	
	protected TransactionRelayer(ScheduledExecutorService executor) {
		super(executor);
	}
	
	public static TransactionRelayer createCfxTransactionRelayer(ScheduledExecutorService executor) {
		return new TransactionRelayer(executor) {
			
			@Override
			protected Server createServer(String url) {
				return new CfxServer(url);
			}
		};
	}
	
	public static TransactionRelayer createEthTransactionRelayer(ScheduledExecutorService executor) {
		return new TransactionRelayer(executor) {
			
			@Override
			protected Server createServer(String url) {
				return new EthServer(url);
			}
		};
	}
	
	protected abstract Server createServer(String url);
	
	public Map<String, Server> getServers() {
		return servers;
	}

	@Override
	protected void doWork(String data) throws PendingException, Exception {
		for (Server server : this.servers.values()) {
			server.send(data);
		}
	}
	
	@Override
	protected void onFailure(String data, Exception e) {
		// do nothing
	}
	
	public void addServer(String url) {
		if (!Strings.isEmpty(url)) {
			this.servers.computeIfAbsent(url.toLowerCase(), this::createServer);
		}
	}
	
	public void removeServer(String url) {
		this.servers.remove(url.toLowerCase());
	}
	
	public abstract static class Server {
		
		protected static final int DEFAULT_TIMEOUT_MILLIS = 3_000;
		
		private static final int NUM_ERRORS_TO_SKIP = 10;
		private static final int SKIP_TIMEOUT_MILLIS = 300_000;	// 5 minutes to recover
		
		private String url;
		
		private long numTotal;
		private long numRpcErrors;
		private long numIoErrors;
		private long numUnknownErrors;
		
		private long skipCounter;
		private long skipTime;
		
		protected Server(String url) {
			this.url = url;
		}
		
		public String getUrl() {
			return url;
		}
		
		public long getNumTotal() {
			return numTotal;
		}
		
		public long getNumRpcErrors() {
			return numRpcErrors;
		}
		
		public long getNumIoErrors() {
			return numIoErrors;
		}
		
		public long getNumUnknownErrors() {
			return numUnknownErrors;
		}
		
		protected abstract void sendRawTx(String signedTx) throws RpcException;
		
		public void send(String signedTx) {
			// Current server may be skipped due to continuous non-PRC errors.
			if (this.skipTime > 0) {
				if (System.currentTimeMillis() - this.skipTime <= SKIP_TIMEOUT_MILLIS) {
					return;
				}
				
				// recovered
				this.skipTime = 0;
			}
			
			try {
				this.sendRawTx(signedTx);
				this.skipCounter = 0;
			} catch (RpcException e) {
				if (Utils.isRpcError(e)) {
					this.numRpcErrors++;
					this.skipCounter = 0;
				} else {
					this.numIoErrors++;
					this.skipCounter++;
				}
			} catch (Exception e) {
				this.numUnknownErrors++;
				this.skipCounter++;
			}
			
			this.numTotal++;
			
			// Skip current server due to continuous non-PRC errors.
			if (this.skipCounter >= NUM_ERRORS_TO_SKIP) {
				this.skipCounter = 0;
				this.skipTime = System.currentTimeMillis();
			}
		}
	}
	
	static class CfxServer extends Server {
		
		private Cfx cfx;

		public CfxServer(String url) {
			super(url);
			
			this.cfx = new CfxBuilder(url).withCallTimeout(DEFAULT_TIMEOUT_MILLIS).build();
		}

		@Override
		protected void sendRawTx(String signedTx) throws RpcException {
			this.cfx.sendRawTransaction(signedTx).sendAndGet();
		}
		
	}
	
	static class EthServer extends Server {
		
		private EthWeb3Wrapper web3j;
		
		public EthServer(String url) {
			super(url);
			
			this.web3j = new EthWeb3Wrapper(url, 0, 1000, DEFAULT_TIMEOUT_MILLIS);
		}

		@Override
		protected void sendRawTx(String signedTx) throws RpcException {
			this.web3j.sendRawTransaction(signedTx);
		}
	}
	
}
