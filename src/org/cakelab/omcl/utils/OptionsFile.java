package org.cakelab.omcl.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class OptionsFile extends NameValueFile {
	
	private transient File file;
	protected transient boolean modified = false;
	
	public void loadFile(File file) throws IOException {
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			load(in);
			this.file = file;
			this.modified = false;
		} finally {
			if (in != null) in.close();
		}
	}
	
	public void loadFromString(String str) {
		InputStream stream = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
		try {
			load(stream);
		} catch (Throwable e) {
			// debugging only
			e.printStackTrace();
		}
	}


	public void save(File f) throws IOException {
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(f);
			super.store(out, SimpleDateFormat.getInstance().format(new Date()));
			this.file = f;
			modified = false;
		} finally {
			if (out != null) out.close();
		}
	}
	
	public void save() throws IOException {
		save(file);
	}

	public void setFile(File f) {
		this.file = f;
		modified = true;
	}


	
	public boolean isModified() {
		return modified;
	}

	public boolean getBooleanProperty(String key, boolean defaultValue) {
		String value = super.getProperty(key);
		if (value != null) {
			if (value.toLowerCase().equals("true")) {
				return true;
			} else {
				return false;
			}
		}
		return defaultValue;
	}

	public String setProperty(String key, String value) {
		String previous = getProperty(key);
		if (!value.equals(previous)) {
			modified = true;
			super.setProperty(key, value);
		}
		return previous;
	}

	public void setProperty(String key, long value) {
		setProperty(key, Long.toString(value));
	}

	public void setProperty(String key, boolean value) {
		setProperty(key, Boolean.toString(value));
	}

	public int getIntProperty(String key, int defaultValue) {
		String v = getProperty(key);
		if (v != null) {
			return Integer.parseInt(v);
		} else {
			return defaultValue;
		}
	}


}
