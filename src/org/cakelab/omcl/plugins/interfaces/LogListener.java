package org.cakelab.omcl.plugins.interfaces;

public interface LogListener {

	void fatal(String msg, Throwable e);
	void error(String msg, Throwable e);
	void warn(String msg, Throwable e);
	void info(String msg, Throwable e);
	
}
