package org.cakelab.omcl.plugins;

public class StubException extends Exception {

	private static final long serialVersionUID = 1L;

	public StubException() {
	}

	public StubException(String message) {
		super(message);
	}

	public StubException(Throwable cause) {
		super(cause);
	}

	public StubException(String message, Throwable cause) {
		super(message, cause);
	}

	public StubException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
