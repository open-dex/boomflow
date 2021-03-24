package boomflow.common.worker;

/**
 * Throws when any data requires to handle later.
 */
@SuppressWarnings("serial")
public class PendingException extends Exception {
	
	private long timeoutMillis;
	
	public PendingException(long timeoutMillis, String causeFormat, Object... causeArgs) {
		super(String.format(causeFormat, causeArgs));
		
		this.timeoutMillis = timeoutMillis;
	}
	
	/**
	 * Return the timeout in milliseconds to pend.
	 */
	public long getTimeoutMillis() {
		return timeoutMillis;
	}

}
