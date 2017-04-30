/**
 * 
 */
package org.msrpenabler.mcu.start;

/**
 * @author Julien Bellanger
 *
 */
public class McuMonitor implements McuMonitorMBean {

	public Object lockStop = new Object();

	@Override
	public void stop() {

		synchronized (lockStop) {
			lockStop.notifyAll(); 
		}
		
		return;
		
	}
	
}


