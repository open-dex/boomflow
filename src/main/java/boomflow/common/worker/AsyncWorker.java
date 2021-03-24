package boomflow.common.worker;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Asynchronous worker to handle data in sequence.
 * 
 * @param <T> data to handle.
 */
public abstract class AsyncWorker<T> implements Runnable {
	
	private ScheduledExecutorService executor;
	
	private Deque<T> queue = new ConcurrentLinkedDeque<T>();
	private AtomicBoolean working = new AtomicBoolean();
	private Object lock = new Object();
	
	private AtomicBoolean paused = new AtomicBoolean();
	private long pauseIntervalMillis = 3000;
	
	protected AsyncWorker(ScheduledExecutorService executor) {
		this.executor = executor;
	}
	
	/**
	 * Returns the number of pending data to handle.
	 */
	public int getPendingCount() {
		return this.queue.size();
	}
	
	/**
	 * Indicates whether the worker is working.
	 */
	public boolean isWorking() {
		return this.working.get();
	}
	
	/**
	 * Indicates whether the worker is paused.
	 */
	public boolean isPaused() {
		return this.paused.get();
	}
	
	/**
	 * Pause or unpause the worker.
	 */
	public void setPaused(boolean paused) {
		this.paused.set(paused);
	}
	
	/**
	 * Get the interval in milliseconds to wait for the next schedule when worker is paused.
	 * By default, it is 3000.
	 */
	public long getPauseIntervalMillis() {
		return pauseIntervalMillis;
	}
	
	/**
	 * Set the interval in milliseconds to wait for the next schedule when worker is paused.
	 */
	public void setPauseIntervalMillis(long pauseIntervalMillis) {
		this.pauseIntervalMillis = pauseIntervalMillis;
	}
	
	/**
	 * Handle the specified data from queue.
	 */
	protected abstract void doWork(T data) throws PendingException, Exception;
	
	/**
	 * Handle the failure case except the PendingException.
	 */
	protected abstract void onFailure(T data, Exception e);
	
	/**
	 * Append the specified data in the end of queue to handle asynchronously.
	 */
	public void submit(T data) {
		this.submit(data, false);
	}
	
	/**
	 * Append the specified data in the front or end of queue to handle asynchronously.
	 */
	public void submit(T data, boolean asFirst) {
		synchronized (this.lock) {
			if (asFirst) {
				this.queue.addFirst(data);
			} else {
				this.queue.addLast(data);
			}
		}
		
		if (this.working.compareAndSet(false, true)) {
			this.executor.submit(this);
		}
	}

	/**
	 * Handle data from queue.
	 */
	@Override
	public void run() {
		// retry later if worker paused
		if (this.paused.get()) {
			this.executor.schedule(this, this.pauseIntervalMillis, TimeUnit.MILLISECONDS);
			return;
		}
		
		// prepare data to handle
		T data;
		
		try {
			data = this.prepareData(this.queue);
		} catch (PendingException e) {
			this.executor.schedule(this, e.getTimeoutMillis(), TimeUnit.MILLISECONDS);
			return;
		}
		
		// handle data
		try {
			this.doWork(data);
		} catch (PendingException e) {
			this.queue.addFirst(data);
			this.executor.schedule(this, e.getTimeoutMillis(), TimeUnit.MILLISECONDS);
			return;
		} catch (Exception e) {
			this.queue.addFirst(data);
			this.onFailure(data, e);
			this.executor.schedule(this, this.pauseIntervalMillis, TimeUnit.MILLISECONDS);
			return;
		}
		
		// schedule next if succeeded
		boolean completed;
		synchronized (this.lock) {
			completed = this.queue.isEmpty();
			if (completed) {
				this.working.set(false);
			}
		}
		
		if (!completed) {
			this.executor.submit(this);
		}
	}
	
	/**
	 * Retrieve data from specified queue to handle. By default, the first data in
	 * queue is retrieved to handle with.
	 * 
	 * If the data is not ready to handle, do not pop from queue and please throw 
	 * <code>PendingException</code> to handle the data again later.
	 */
	protected T prepareData(Deque<T> queue) throws PendingException {
		return queue.removeFirst();
	}

}
