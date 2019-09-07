package org.cakelab.omcl.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map.Entry;

public class NameValueFile {
	private static final String SEPARATOR = ":";

	private static final char LINE_END = '\n';

	private static final String COMMONT_PREFIX = "#";
	
	private HashMap<String, String> content = new HashMap<String, String>();
	
	
	public void load(InputStream in) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
		String line;
		int count = 0;
		while (null != (line = reader.readLine())) {
			line = line.trim();
			if (line.length() > 0 && !line.startsWith(COMMONT_PREFIX)) {
				int index = line.indexOf(SEPARATOR);
				if (index < 0) {
					throw new IOException("syntax error in line " + count);
				} else {
					String key = line.substring(0, index);
					String value = line.substring(index+1, line.length());
					
					content.put(key, value);
				}
			}
		}
	}

	public void store(OutputStream out, String comment) throws IOException {
		for (Entry<String, String> entry : content.entrySet()) {
			String line = entry.getKey() + SEPARATOR + entry.getValue() + LINE_END;
			out.write(line.getBytes(Charset.forName("UTF-8")));
		}
		out.flush();
	}

	public String getProperty(String key) {
		return content.get(key);
	}

	public String setProperty(String key, String value) {
		return content.put(key, value);
	}

}
