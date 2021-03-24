package boomflow.common.worker;

import java.util.Deque;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Asynchronous worker to handle data in batch.
 */
public abstract class BatchWorker<T extends BatchWorker.Batchable> extends AsyncWorker<T> {
	
	/**
	 * Enable to handle data in batch.
	 */
	public static interface Batchable {
		boolean batchWith(Batchable other);
		int size();
	}
	
	// maximum number of data to handle together.
	private int batchSize;
	// number of times to wait for enough data to handle together.
	private int waitCountdown;
	// interval to wait for enough data to handle together.
	private long waitIntervalMillis;
	
	// current batch data whose size is not enough to handle together.
	private T currentBatchData;
	// current countdown to wait for more data to handle together.
	private int currentCountdown;
	
	/**
	 * Create an instance of BatchWorker.
	 * @param batchSize maximum number of data to handle in a batch.
	 * @param waitCountdown number of times to wait for enough data to handle in a batch.
	 * @param waitIntervalMillis interval in milliseconds to wait for enough data to handle in a batch.
	 */
	protected BatchWorker(ScheduledExecutorService executor, int batchSize, int waitCountdown, long waitIntervalMillis) {
		super(executor);
		
		this.batchSize = batchSize;
		this.waitCountdown = waitCountdown;
		this.waitIntervalMillis = waitIntervalMillis;
		this.currentCountdown = waitCountdown;
	}
	
	@Override
	protected T prepareData(Deque<T> queue) throws PendingException {
		T data = this.currentBatchData == null ? queue.removeFirst() : this.currentBatchData;
		
		while (data.size() < this.batchSize) {
			T next = queue.peekFirst();
			if (next != null) {
				if (!data.batchWith(next)) {
					break;
				}
				
				queue.removeFirst();
			} else if (this.currentCountdown > 0) {
				this.currentBatchData = data;
				this.currentCountdown--;
				throw new PendingException(this.waitIntervalMillis, "wait for more data for a batch");
			} else {
				break;
			}
		}
		
		this.currentBatchData = null;
		this.currentCountdown = this.waitCountdown;
		
		return data;
	}

}
