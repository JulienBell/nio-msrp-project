package org.msrpenabler.server.api;

public class LockOptions {

	private boolean lockOutput;
	private boolean lockInput;
	
	private boolean discardOutput;
	private boolean discardInput;

	/**
	 * 
	 * @return
	 */
	public boolean isLockOutput() {
		return lockOutput;
	}

	/**
	 * Ask to put all output msg into a buffer (beware that discardOutput has a higher priority)
	 * On msg are sent to the network
	 * 
	 * @param lockOutput
	 */
	public void setLockOutput(boolean lockOutput) {
		this.lockOutput = lockOutput;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isLockInput() {
		return lockInput;
	}

	/**
	 * Ask to put all input msg into a buffer (beware that discardInput has a higher priority)
	 * on unlock msg are sent to the listeners
	 * 
	 * @param lockInput
	 */
	public void setLockInput(boolean lockInput) {
		this.lockInput = lockInput;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isDiscardOutput() {
		return discardOutput;
	}

	/**
	 * Ask to discard all msg sent to this session and emptying output msg queue if output lock is set
	 * 
	 * @param discardOutput
	 */
	public void setDiscardOutput(boolean discardOutput) {
		this.discardOutput = discardOutput;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isDiscardInput() {
		return discardInput;
	}

	/**
	 * Ask to discard all msg received on this session and emptying input msg queue if input lock is set
	 * 
	 * @param discardInput
	 */
	public void setDiscardInput(boolean discardInput) {
		this.discardInput = discardInput;
	}

	
	public LockOptions(boolean lockInput, boolean lockOutput, boolean discardInput, boolean discardOutput) {
		this.setLockOutput(lockOutput);
		this.setLockInput(lockInput);
		this.setDiscardInput(discardInput);
		this.setDiscardOutput(discardOutput);
	}

//	public boolean isOutpuLock() {
//		return lockOutput;
//	}
//
//	public boolean islistenersLocked() {
//		return lockInput;
//	}

}
