package org.cakelab.omcl.plugins;

import org.cakelab.omcl.plugins.interfaces.ServicesListener;
import org.cakelab.omcl.plugins.utils.log.LogRelay;
import org.cakelab.omcl.utils.log.Log;

public class PluginServices {

	
	private static ServicesListener listener;


	public static void init() {
		// preloading classes that should be available for all plugins
		ClassLoader cl = PluginServices.class.getClassLoader();
		try {
			cl.loadClass(ServicesBase.class.getName());
			cl.loadClass(ServicesListener.class.getName());
			cl.loadClass(StubException.class.getName());
			cl.loadClass(LogRelay.class.getName());
		} catch (ClassNotFoundException e) {
			// That would be hilarious!
			Log.warn("weird condition during initialisation of plugin services", e);
		}
	}

	public synchronized static ServicesListener getListener() {
		return listener;
	}


	public synchronized static void setListener(ServicesListener listener) {
		PluginServices.listener = listener;
	}
	
	
}
