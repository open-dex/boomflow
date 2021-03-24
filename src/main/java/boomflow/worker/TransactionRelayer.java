package boomflow.worker;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import org.web3j.utils.Strings;

import boomflow.common.CfxBuilder;
import boomflow.common.worker.AsyncWorker;
import boomflow.common.worker.PendingException;
import conflux.web3j.Cfx;
import conflux.web3j.RpcException;

/**
 * TransactionRelayer sends transactions to multiple RPC servers, so that
 * temporary network issue will not lead to transaction not propagated timely
 * or even not propagated out.
 */
public class TransactionRelayer extends AsyncWorker<String> {
	
	private Map<String, Server> servers = new ConcurrentHashMap<String, Server>();
	
	public TransactionRelayer(ScheduledExecutorService executor) {
		super(executor);
	}
	
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
			this.servers.computeIfAbsent(url.toLowerCase(), Server::new);
		}
	}
	
	public void removeServer(String url) {
		this.servers.remove(url.toLowerCase());
	}
	
	public static class Server {
		
		private static final int DEFAULT_CFX_TIMEOUT_MILLIS = 3_000;
		
		private static final int NUM_ERRORS_TO_SKIP = 10;
		private static final int SKIP_TIMEOUT_MILLIS = 300_000;	// 5 minutes to recover
		
		private String url;
		private Cfx cfx;
		
		private long numTotal;
		private long numRpcErrors;
		private long numIoErrors;
		private long numUnknownErrors;
		
		private long skipCounter;
		private long skipTime;
		
		public Server(String url) {
			this.url = url;
			this.cfx = new CfxBuilder(url).withCallTimeout(DEFAULT_CFX_TIMEOUT_MILLIS).build();
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
				this.cfx.sendRawTransaction(signedTx).send();
				this.skipCounter = 0;
			} catch (RpcException e) {
				this.numRpcErrors++;
				this.skipCounter = 0;
			} catch (IOException e) {
				this.numIoErrors++;
				this.skipCounter++;
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
	
}
