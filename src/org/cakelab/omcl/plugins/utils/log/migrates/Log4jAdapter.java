package org.cakelab.omcl.plugins.utils.log.migrates;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.cakelab.omcl.plugins.interfaces.LogListener;

public class Log4jAdapter extends AbstractAppender {

	private static final String APPENDER_NAME = "LogRelayLog4jAdapter";
	private LogListener listener;

	
	private static Log4jAdapter INSTANCE = null;

	
	
	public synchronized static void connect(LogListener logRelay) {
		Logger rootLogger = (Logger) LogManager.getRootLogger();
		rootLogger.setLevel(Level.INFO);
		if (!rootLogger.getAppenders().containsKey(APPENDER_NAME)) {
			rootLogger.addAppender(createInstance(logRelay));
		}
	}

	public synchronized static void disconnect() {
		if (INSTANCE != null) {
			Logger rootLogger = (Logger) LogManager.getRootLogger();
			rootLogger.removeAppender(INSTANCE);
			INSTANCE = null;
		}
	}
	

	
	
	
	synchronized static Log4jAdapter getInstance() {
		return INSTANCE;
	}

	synchronized static Log4jAdapter createInstance(LogListener logRelay) {
		assert(INSTANCE == null);
		return INSTANCE = new Log4jAdapter(logRelay);
	}
	
	Log4jAdapter(LogListener listener) {
		super(APPENDER_NAME, null, null);
		this.listener = listener;
		start();
	}

	
	
	

	@Override
	public void append(LogEvent e) {
		Level level = e.getLevel();
		switch(level) {
		case FATAL:
			listener.fatal(e.getMessage().getFormattedMessage(), e.getThrown());
			break;
		case ERROR:
			listener.error(e.getMessage().getFormattedMessage(), e.getThrown());
			break;
		case WARN:
			listener.warn(e.getMessage().getFormattedMessage(), e.getThrown());
			break;
		case INFO:
			listener.info(e.getMessage().getFormattedMessage(), e.getThrown());
			break;
		case DEBUG:
			listener.info(e.getMessage().getFormattedMessage(), e.getThrown());
			break;
		case TRACE:
			listener.info(e.getMessage().getFormattedMessage(), e.getThrown());
			break;
		default:
			/* 
			 * This is a logger configuration error of the application 
			 * we are appending our logger to. So, we don't care.
			 */
			System.err.println("LogRelay: non standard log level in log event: event ignored.");
		}
	}


}
