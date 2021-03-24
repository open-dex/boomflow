package boomflow.common;

import java.time.Duration;

import org.web3j.protocol.Web3jService;
import org.web3j.protocol.http.HttpService;

import conflux.web3j.Cfx;
import okhttp3.OkHttpClient;

/**
 * Utility to build <code>Cfx</code> instance.
 */
public class CfxBuilder {
	
	private String url;
	
	private int retry;
	private long retryIntervalMillis;
	
	private long callTimeoutMillis;
	
	public CfxBuilder(String url) {
		this.url = url;
	}
	
	/**
	 * Allow to re-send RPC requests when temporary IO error occurred.
	 */
	public CfxBuilder withRetry(int retry, long intervalMillis) {
		this.retry = retry;
		this.retryIntervalMillis = intervalMillis;
		return this;
	}
	
	/**
	 * Set timeout in milliseconds for each RPC request.
	 */
	public CfxBuilder withCallTimeout(long millis) {
		this.callTimeoutMillis = millis;
		return this;
	}
	
	/**
	 * Build a <code>Cfx</code> instance.
	 */
	public Cfx build() {
		OkHttpClient client = new OkHttpClient.Builder()
				.callTimeout(Duration.ofMillis(this.callTimeoutMillis))
				.build();
		Web3jService service = new HttpService(this.url, client);
		return Cfx.create(service, this.retry, this.retryIntervalMillis);
	}

}
