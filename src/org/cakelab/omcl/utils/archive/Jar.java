package org.cakelab.omcl.utils.archive;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.cakelab.omcl.utils.FileSystem;



public class Jar extends IOStreamArchive {
	
	public class MyEntry extends JarEntry implements Entry {

		public MyEntry(JarEntry entry) {
			super(entry);
		}

	}


	private static final String AR_NAME = "jar";
	private JarOutputStream out;
	private JarInputStream in;

	
	
	public static void init() {
		Archive.register(AR_NAME, Jar.class); 
	}


	public Jar(File archive) {
		super(AR_NAME, archive);
	}



	@Override
	protected OutputStream createArchiveOutputStream(FileOutputStream archive) throws IOException {
		return out = new JarOutputStream(archive);
	}



	@Override
	protected void putNextEntry(String file) throws IOException {
		out.putNextEntry(new JarEntry(file));
	}


	@Override
	protected InputStream createArchiveInputStream(
			FileInputStream archive) throws IOException {
		return in = new JarInputStream(archive);
	}

	@Override
	protected Entry getNextEntry() throws IOException {
		JarEntry jarentry = in.getNextJarEntry();
		if (jarentry == null) return null;
		Archive.Entry entry = new MyEntry(jarentry);
		return entry;
	}


	public Manifest readManifest() throws IOException {
		open();
		Manifest mf = in.getManifest();
		close();
		return mf;
	}


	public void writeManifest(Manifest mf) throws IOException {
		in = (JarInputStream) createArchiveInputStream(new FileInputStream(archive));
		File tmpDir = FileSystem.createTempDir(archive.getName(), true);
		FileFilter acceptAll = new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return true;
			}
		};
		extract(tmpDir, acceptAll, false);
		in.close();
		archive.delete();
		create();
		for (File f : tmpDir.listFiles()) {
			add(f.getParentFile(), f, acceptAll);
		}
		close();
		FileSystem.delete(tmpDir);
	}
	
	
	
}
