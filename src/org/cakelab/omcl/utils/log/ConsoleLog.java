package org.cakelab.omcl.utils.log;



public class ConsoleLog extends StreamLogBase implements LogListener {

	@Override
	public void fatal(String msg, Throwable e) {
		fatal(System.err, msg, e);
	}

	@Override
	public void fatal(String msg) {
		fatal(System.err, msg);
	}

	@Override
	public void error(String msg, Throwable e) {
		error(System.err, msg, e);
	}

	@Override
	public void error(String msg) {
		error(System.err, msg);
	}

	@Override
	public void warn(String msg, Throwable e) {
		warn(System.err, msg, e);
	}

	@Override
	public void warn(String msg) {
		warn(System.err, msg);
	}

	@Override
	public void info(String msg) {
		info(System.out, msg);
	}

}
