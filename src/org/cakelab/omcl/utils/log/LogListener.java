package org.cakelab.omcl.utils.log;

public interface LogListener {

	void fatal(String msg, Throwable e);
	void fatal(String msg);
	void error(String msg, Throwable e);
	void error(String msg);
	void warn(String msg, Throwable e);
	void warn(String msg);
	void info(String msg);
	
	
}
