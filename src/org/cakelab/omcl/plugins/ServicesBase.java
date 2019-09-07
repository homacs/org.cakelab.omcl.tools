package org.cakelab.omcl.plugins;

import org.cakelab.omcl.plugins.interfaces.ServicesListener;
import org.cakelab.omcl.plugins.utils.log.LogRelay;

public class ServicesBase {

	protected ServicesListener listener;
	private LogRelay logRelay;

	public ServicesBase(ServicesListener listener) throws StubException {
		this.listener = listener;
		logRelay = LogRelay.getLogRelay();
	}

	public void prepareContext() throws StubException {
		logRelay.addListener(listener);
		logRelay.start();
	}

	public void resetContext() throws StubException {
		logRelay.stop();
		logRelay.removeListener(listener);
	}

}
