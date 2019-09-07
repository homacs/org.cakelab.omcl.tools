package org.cakelab.omcl.utils.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class LogFileListener extends StreamLogBase implements LogListener {

	private File logfile;
	private PrintStream out;

	public LogFileListener(File logfile) throws IOException {
		this.logfile = logfile;
		
		if (!logfile.exists()) {
			logfile.createNewFile();
		}
		
		open();
	}

	public void close() {
		out.close();
	}

	private void open() throws FileNotFoundException {
		out = new PrintStream(new FileOutputStream(logfile));
	}

	@Override
	public void fatal(String msg, Throwable e) {
		fatal(out, msg, e);
		out.flush();
	}

	@Override
	public void fatal(String msg) {
		fatal(out, msg);
		out.flush();
	}

	@Override
	public void error(String msg, Throwable e) {
		error(out, msg, e);
		out.flush();
	}

	@Override
	public void error(String msg) {
		error(out, msg);
		out.flush();
	}

	@Override
	public void warn(String msg, Throwable e) {
		warn(out, msg, e);
		out.flush();
	}

	@Override
	public void warn(String msg) {
		warn(out, msg);
		out.flush();
	}

	@Override
	public void info(String msg) {
		info(out, msg);
		out.flush();
	}

}
