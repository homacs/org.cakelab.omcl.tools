package org.cakelab.omcl.utils.log;

import java.io.PrintStream;

public class StreamLogBase {

	protected void fatal(PrintStream out, String msg, Throwable e) {
		fatal(out, msg);
		e.printStackTrace(out);
	}

	protected void fatal(PrintStream out, String msg) {
		msg = "[fatal] " + msg;
		out.println(msg);
	}


	protected void error(PrintStream out, String msg, Throwable e) {
		error(out, msg);
		e.printStackTrace(out);
	}

	protected void error(PrintStream out, String msg) {
		msg = "[error] " + msg;
		out.println(msg);
	}


	protected void warn(PrintStream out, String msg, Throwable e) {
		warn(out, msg);
		e.printStackTrace(out);
	}

	protected void warn(PrintStream out, String msg) {
		msg = "[warn] " + msg;
		out.println(msg);
	}


	protected void info(PrintStream out, String msg, Throwable e) {
		info(out, msg);
		e.printStackTrace(out);
	}

	protected void info(PrintStream out, String msg) {
		msg = "[info] " + msg;
		out.println(msg);
	}


}
