package org.cakelab.omcl.utils.archive;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import org.cakelab.omcl.utils.FileSystem;


public abstract class IOStreamArchive extends Archive {
	public static final FileFilter FILTER_ALL = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return false;
		}};
	public static final FileFilter FILTER_NOTHING = new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return true;
			}};
	private OutputStream out;
	private HashMap<String, String> entries = new HashMap<String, String>(128);
	private InputStream in;
	
	public IOStreamArchive(String name, File archive) {
		super(name, archive);
	}

	
	@Override
	public void create() throws IOException {
		if (archive.exists()) archive.delete();
		open();
	}


	@Override
	public void open() throws IOException {
		entries.clear();

        out = createArchiveOutputStream(new FileOutputStream(archive));
        in = createArchiveInputStream(new FileInputStream(archive));

	}

	@Override
	public void close() throws IOException {
		if (out != null) {
			out.close();
			out = null;
		}
		if (in != null) {
			in.close();
			in = null;
		}
	}
	
	public void create(File parentDir, File[] files, FileFilter filter) throws IOException {
		parentDir = parentDir.getCanonicalFile();

        open();
        for (File file : files) {
			file = FileSystem.removeParentPath(parentDir, file);
        	add(parentDir, file, filter);
        }
        close();
	}
	@Override
	public void create(File parentDir, File subdir, FileFilter filter) throws IOException {
		parentDir = parentDir.getCanonicalFile();
		File dir = new File(parentDir.toString() + File.separatorChar + subdir.toString());
		if (!dir.exists() || !dir.isDirectory()) throw new IOException("Source '" + dir + "' is not an existing directory");

        open();

        add(parentDir, subdir, filter);
        
        close();
	}

	@Override
	public void extract(File targetPath, FileFilter exclude, boolean override) throws IOException {
		in = createArchiveInputStream(new FileInputStream(archive));
		if (exclude == null) exclude = FILTER_NOTHING;
		for (Entry entry = getNextEntry(); entry != null; entry = getNextEntry()) {
			File entryFile = new File(entry.getName());
			if (!exclude.accept(entryFile)) continue;
			File target = FileSystem.combineCanonicalPath(targetPath, entryFile);
			
			
			if (entry.isDirectory()) {
				target.mkdirs();
			} else {
				if (!override && target.exists()) throw new IOException("Target '" + target.toString() + "' in archive '"+ archive.toString() + "' exists and override was not permitted");
				target.getParentFile().mkdirs();
				FileSystem.dump2File(in, target.getPath(), entry.getSize());
			}
		}
	}




	protected abstract Entry getNextEntry() throws IOException;


	protected abstract InputStream createArchiveInputStream(FileInputStream fileInputStream) throws IOException;


	protected abstract OutputStream createArchiveOutputStream(
			FileOutputStream archive) throws IOException;

	public void add(File parentDir, File file, FileFilter filter) throws IOException {

		File fullpath = FileSystem.getCanonicalFile(parentDir.toString() + File.separatorChar + file.toString());
		
		if (fullpath.isDirectory()) {
			String dirname = FileSystem.removeParentPath(parentDir, fullpath).toString();
			if (!dirname.endsWith(File.separator)) {
				dirname = dirname + File.separator;
			}
			putNextDirectory(dirname);
			for (File f : fullpath.listFiles(filter)) {
				add(parentDir, FileSystem.removeParentPath(parentDir, f), filter);
			}
		} else {
	        // name the file inside the archive
			putNextEntry(file.toString());

	        FileSystem.dump2stream(new FileInputStream(fullpath), out);
		}
	}

	
	protected void putNextDirectory(String entryname) throws IOException {
		if (!entries.containsKey(entryname)) {
			putNextEntry(entryname);
			entries.put(entryname, entryname);
		}
	}


	protected abstract void putNextEntry(String file) throws IOException;

}
