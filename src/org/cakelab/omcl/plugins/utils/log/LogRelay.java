package org.cakelab.omcl.plugins.utils.log;


import java.lang.reflect.Method;
import java.util.ArrayList;

import org.cakelab.omcl.plugins.StubException;
import org.cakelab.omcl.plugins.interfaces.LogListener;
import org.cakelab.omcl.utils.log.Log;





public class LogRelay implements LogListener {
	
	public static final String MIGRATES_PACKAGE = "org.cakelab.omcl.plugins.utils.log.migrates";
	
	private static final String LOG4J_ADAPTER_CLASSNAME = MIGRATES_PACKAGE + ".Log4jAdapter";



	private static LogRelay INSTANCE = null;

	
	
	private ArrayList<LogListener> listeners = new ArrayList<LogListener>();



	private boolean log4jConnected;
	
	
	
	



	public static synchronized LogRelay getLogRelay() throws StubException {
		if (INSTANCE == null) {
			INSTANCE = new LogRelay();
		}
		return INSTANCE;
	}
	
	public void start() throws StubException {
		connectLog4j();
	}

	public void stop() throws StubException {
		disconnectLog4j();
	}
	
	public synchronized void addListener(LogListener listener) {
		if (listener != null && !listeners.contains(listeners)) {
			listeners.add(listener);
		}
	}
	
	public synchronized void removeListener(LogListener listener) {
		if (listener != null) listeners.remove(listener);
	}

	
	
	private void connectLog4j() throws StubException {
		assert(!log4jConnected);
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			Class<?> log4jAdapterClass = classLoader.loadClass(LOG4J_ADAPTER_CLASSNAME);
			
			Method method_connect = log4jAdapterClass.getMethod("connect", LogListener.class);
			method_connect.invoke(null, this);
			log4jConnected = true;
		} catch (Throwable e) {
			// there is no log4j logger
			// so we don't need to adapt.
			Log.info("log4j impl not found or not accessible");
			return;
		}
	}

	private void disconnectLog4j() throws StubException {
		if(log4jConnected) {
			
			try {
				ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
				Class<?> log4jAdapterClass = classLoader.loadClass(LOG4J_ADAPTER_CLASSNAME);
				Method method_disconnect = log4jAdapterClass.getMethod("disconnect");
				method_disconnect.invoke(null);
				log4jConnected = false;
			} catch (Throwable e) {
				// something suspicious happend.
				// don't try this again
				log4jConnected = false;
				throw new StubException("Failed to disconnect from log4j logger", e);
			}
		}
	}

	



	

	
	

	@Override
	public synchronized void fatal(String msg, Throwable e) {
		for (LogListener l : listeners) {
			l.fatal(msg, e);
		}
	}




	@Override
	public synchronized void error(String msg, Throwable e) {
		for (LogListener l : listeners) {
			l.error(msg, e);
		}
	}




	@Override
	public synchronized void warn(String msg, Throwable e) {
		for (LogListener l : listeners) {
			l.warn(msg, e);
		}
	}




	@Override
	public synchronized void info(String msg, Throwable e) {
		for (LogListener l : listeners) {
			l.info(msg, e);
		}
	}
}
