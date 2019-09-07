package org.cakelab.omcl.utils.watchdog;

import java.io.File;
import java.io.IOException;


/**
 * WatchDogClient listens to notifications of
 * WatchDogServer to determine if its still alive.
 * @author homac
 *
 */
public class WatchDogClient extends Thread {
	static final int MAX_INIT_CHECKS = 10;
	static final int CHECK_INTERVAL = WatchDogService.UPDATE_INTERVAL * 2;
	private long lastUpdate;
	private volatile boolean running;
	private File messageBus;
	private Runnable callback;
	private int initChecks;
	/**
	 * 
	 * @param messageBus file which is used by server and clients to exchange update messages
	 * @param callback Callback called when the server dies.
	 * @throws IOException 
	 */
	public WatchDogClient (File messageBus, Runnable callback) throws IOException {
		super("watch dog client: " + messageBus.getName());
		this.messageBus = messageBus;
		this.callback = callback;
		lastUpdate = -1;
		initChecks = MAX_INIT_CHECKS;
		if (!messageBus.exists()) messageBus.createNewFile();
	}
	
	public void shutdown () {
		running = false;
		interrupt();
	}


	@Override
	public void run() {
		running = true;
		try {
			//
			// initialisation phase - wait for first update
			//
			while (running && !isInitialised()) {
				Thread.sleep(CHECK_INTERVAL);
				if (--initChecks == 0) {
					serverDied();
				}
			}

			//
			// observation phase - if we don't get an update, we consider him dead
			//
			while (running) {
				Thread.sleep(CHECK_INTERVAL);
				if (!checkAlive()) {
					serverDied();
					return;
				}
			}
		} catch (InterruptedException e) {
		} finally {
			running = false;
		}
	}
	
	private void serverDied() {
		shutdown();
		callback.run();
	}

	private boolean checkAlive() {
		long update = getUpdateTime(messageBus);
		if (update > lastUpdate) {
			lastUpdate = update;
			return true;
		}
		return false;
	}

	private boolean isInitialised() {
		if (lastUpdate == -1) {
			lastUpdate = getUpdateTime(messageBus);
		} else {
			long update = getUpdateTime(messageBus);
			if (update > lastUpdate) {
				lastUpdate = update;
				return true;
			}
		}
		return false;
	}

	private long getUpdateTime(File messageBus) {
		// Unfortunately File.lastModifed returns the last modified time 
		// which was read when the File object was created.
		// Thus, we have to instantiate a new File object each time.
		return new File(messageBus.getAbsolutePath()).lastModified();
	}
	
}
