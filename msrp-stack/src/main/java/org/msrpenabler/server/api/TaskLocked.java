package org.msrpenabler.server.api;

public abstract class TaskLocked<T> implements Runnable {

	final T container;
	
	public TaskLocked(T container) {
		this.container = container; 
	}

}
