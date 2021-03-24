package boomflow.event;

public interface Handler<T> {
	void handle(T data);
}
