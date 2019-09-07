package org.cakelab.omcl.utils.log;

import java.util.ArrayList;

public class Log {

	static {
		listeners = new ArrayList<LogListener>();
		UncaughtExceptionLogger.register();
	}

	private Log(){}
	
	
	private static ArrayList<LogListener> listeners;

	
	public static void fatal(String msg, Throwable e) {
		for (LogListener listener : listeners) {
			try {
				listener.fatal(msg, e);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	public static void fatal(String msg) {
		for (LogListener listener : listeners) {
			try {
				listener.fatal(msg);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	public static void error(String msg, Throwable e) {
		for (LogListener listener : listeners) {
			try {
				listener.error(msg, e);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	public static void error(String msg) {
		for (LogListener listener : listeners) {
			try {
				listener.error(msg);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	public static void warn(String msg, Throwable e) {
		for (LogListener listener : listeners) {
			try {
				listener.warn(msg, e);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	public static void warn(String msg) {
		for (LogListener listener : listeners) {
			try {
				listener.warn(msg);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	public static void info(String msg) {
		for (LogListener listener : listeners) {
			try {
				listener.info(msg);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	public static void addLogListener(LogListener listener) {
		listeners.add(listener);
	}
}
