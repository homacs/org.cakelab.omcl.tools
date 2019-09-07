package org.cakelab.omcl.utils.archive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Zip extends IOStreamArchive {
	public class MyEntry extends ZipEntry implements Entry {

		public MyEntry(ZipEntry e) {
			super(e);
		}

	}



	static final String AR_NAME = "zip";
	private ZipOutputStream out;
	private ZipInputStream in;
	
	public static void init() {
		Archive.register(AR_NAME, Zip.class);
	}
	
	
	
	public Zip(File archive) {
		super(AR_NAME, archive);
	}


	@Override
	protected OutputStream createArchiveOutputStream(FileOutputStream archive)
			throws IOException {
		return out = new ZipOutputStream(archive);
	}

	@Override
	protected void putNextEntry(String file) throws IOException {
		out.putNextEntry(new ZipEntry(file));
	}



	@Override
	protected Entry getNextEntry() throws IOException {
		ZipEntry entry = in.getNextEntry();
		if (entry == null) return null;
		return new MyEntry(entry);
	}



	@Override
	protected InputStream createArchiveInputStream(
			FileInputStream input) throws IOException {
		return in = new ZipInputStream(input);
	}




}
