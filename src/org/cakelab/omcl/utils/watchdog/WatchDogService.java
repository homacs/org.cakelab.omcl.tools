package org.cakelab.omcl.utils.watchdog;

import java.io.File;
import java.io.FileNotFoundException;


/**
 * WatchDogServer sends notifications to WatchDogClients 
 * which subscribed to its messageBus.
 * @author homac
 *
 */
public class WatchDogService extends Thread {
	// Note:
	// I'd love to use interrupts here to save system resources,
	// but Java does not allow us to do so.
	
	private File messageBus;
	private volatile boolean running;
	static final int UPDATE_INTERVAL = 5000;

	public WatchDogService(File messageBus) throws FileNotFoundException {
		super("watch dog service: " + messageBus.getName());
		this.messageBus = messageBus;
	}

	public void shutdown() {
		running = false;
		interrupt();
	}
	
	@Override
	public void run() {
		this.running = true;
		try {
			while (running) {
				sendAlive();
				Thread.sleep(UPDATE_INTERVAL);
			}
		} catch (InterruptedException e) {
		} finally {
			running = false;
		}
	}
	
	
	private void sendAlive() {
		if (!messageBus.setLastModified(System.currentTimeMillis())) {
			System.err.println("Warning: watchdog not supported");
			shutdown();
		}
	}

	
}
