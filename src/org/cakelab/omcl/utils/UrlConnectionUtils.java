package org.cakelab.omcl.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class UrlConnectionUtils {

	// TODO: try improve performance using alternative HttpClient implementations
	
	private static final int STANDARD_READ_TIMEOUT = 10000;
	private static final int STANDARD_CONNECT_TIMEOUT = 20000;
	

	public static void testAvailability(URL url, int timeout) throws IOException {
		InputStream in = getInputStream(url, timeout, timeout);
		try {
			in.read();
		} finally {
			UrlConnectionUtils.close(in);
		}
	}

	public static void testAvailability(URL url) throws IOException {
		testAvailability(url, STANDARD_CONNECT_TIMEOUT);
	}

	public static boolean testAvailabilitySilent(String url) throws MalformedURLException {
		return testAvailabilitySilent(new URL(url), STANDARD_CONNECT_TIMEOUT);
	}


	public static boolean testAvailabilitySilent(URL url, int timeout) {
		boolean available = true;
		try {
			testAvailability(url, timeout);
		} catch (Throwable t) {
			available = false;
		}
		return available;
	}

	
	public static InputStream getInputStream(URL url) throws IOException {
		return getInputStream(url, STANDARD_CONNECT_TIMEOUT, STANDARD_READ_TIMEOUT);
	}

	public static InputStream getInputStream(URL url, int connectTimeout,
			int readTimeout) throws IOException {
		URLConnection connection = openConnection(url, connectTimeout, readTimeout);
		return connection.getInputStream();
	}

	public static URLConnection openConnection(URL url, int connectTimeout,
			int readTimeout) throws IOException {
		URLConnection connection = url.openConnection();
		if (connection instanceof HttpURLConnection) {
			HttpURLConnection httpConnection = (HttpURLConnection) connection;
			httpConnection.setConnectTimeout(connectTimeout);
			httpConnection.setReadTimeout(readTimeout);
			httpConnection.setUseCaches(false);
		}
		return connection;
	}

	public static void close(InputStream in) {
		if (in != null)	try {
			in.close();
		} catch (IOException e1) {
		}
	}

	public static void initSystemProperties() {
		System.setProperty("sun.net.client.defaultReadTimeout", Integer.toString(STANDARD_READ_TIMEOUT));
		System.setProperty("sun.net.client.defaultConnectTimeout", Integer.toString(STANDARD_CONNECT_TIMEOUT));
		System.setProperty("http.keepAlive", "true");
		System.setProperty("http.maxConnections", "50");
		System.setProperty("java.net.preferIPv4Stack", "true");
		//
		// this improves performance in case of keep-alive support.
		//
		System.setProperty("sun.net.http.errorstream.enableBufferin", "true");
		System.setProperty("sun.net.http.errorstream.timeout", "300");
		System.setProperty("sun.net.http.errorstream.bufferSize", "4096");
		
		
	}
}
