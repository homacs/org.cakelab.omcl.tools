package org.cakelab.omcl.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import org.cakelab.omcl.utils.shell.Shell;



/** 
 * This is a utility class providing some methods to deal with I/O of text and binary files and 
 * file system access in general.</br>
 * </br>
 * Note: Uses Shell to apply file name expansion (not in all cases).
 * 
 * @author homac
 *
 */
public class FileSystem {
	static char URL_PATH_SEPARATOR = '/';
	static final boolean OPEN_APPEND = true;
	private static final String UNIX_STD_TMP = "/tmp";
	/*
	 * TODO: FileSystem class needs refactoring. Decision about relationship to Shell required.
	 */
	
	
	/** Write the entire stream 'in' to the file identified by filename.
	 * 
	 * @param in Stream to be dumped to file.
	 * @param filename Path to a file to receive the stream.
	 * @throws IOException
	 */
	public static void dump2File(InputStream in, String filename) throws IOException {
		FileOutputStream out = new FileOutputStream(filename);
		dump2stream(in, out);
		out.close();
	}
	/** Write size bytes of stream 'in' to the file identified by filename.
	 * 
	 * @param in Stream to be dumped to file.
	 * @param filename Path to a file to receive the stream.
	 * @throws IOException
	 */
	public static void dump2File(InputStream in, String filename, long size) throws IOException {
		FileOutputStream out = new FileOutputStream(filename);
		dump2stream(in, out, size);
		out.close();
	}

	/** Strips all parent directory entries of the given path and returns the remaining 
	 * file/directory name. The file name is not interpreted, i.e. it may or may not 
	 * exist and it may be a directory itself. Furthermore the resulting file name can 
	 * be invalid, i.e. it can be a character sequence which cannot be used in your file 
	 * system or a relative path such as ".", "..", "~" etc. which has to be further processed 
	 * to point to an actual directory.
	 * 
	 * @param filePath Full path of the file/directory.
	 * @return File name of the file without directories.
	 * @throws IOException In case 
	 */
	public static String basename(String filePath) throws IOException {
		return new File(filePath).getName();
	}

	/** Dumps the given input stream to the given output stream.
	 * None of the streams will be closed afterwards.
	 * 
	 * @param in Input stream
	 * @param out Output stream
	 * @throws IOException
	 */
	public static void dump2stream(InputStream in, OutputStream out) throws IOException {
		byte[] buf = new byte[1024];
		int read;
		while (0 < (read = in.read(buf))) {
			out.write(buf, 0, read);
		}
	}

	/** Dumps size bytes of the given input stream to the given output stream.
	 * None of the streams will be closed afterwards.
	 * 
	 * @param in Input stream
	 * @param out Output stream
	 * @throws IOException
	 */
	public static void dump2stream(InputStream in, OutputStream out, long size) throws IOException {
		if ((int)size < 0) {
			dump2stream(in, out);
			return;
		}
		final int bufsize = 1024;
		byte[] buf = new byte[bufsize];
		int read;
		do {
			int toread = (int)(bufsize > size ? size : bufsize);
			read = in.read(buf, 0, toread);
			out.write(buf, 0, read);
			size -= read;
			// we do not check on EOF to force the exception in case of an error
		} while (size > 0);
	}

	/** create the given directory and its parent directories (if required).
	 * 
	 * @param dir Path of directories to be created.
	 */
	public static void createDirectory(String dir) {
		File d = new File(dir);
		if (!d.exists()) d.mkdirs();
	}

	/**
	 * Returns the directory the given file resides in.
	 * @param fn File name of the file whose directory we are looking for. 
	 * @return The name of the directory containing the file.
	 */
	public static String getDirectory(String fn) {
		File f = new File (fn);
		return f.getParent();
	}

	/** Reads text from a file (entirely).
	 * 
	 * @param fn File name (path).
	 * @return The whole text found in the file.
	 * @throws IOException
	 */
	public static String readText(String fn) throws IOException {
		return readText(fn, Charset.defaultCharset());
	}

	/** Reads text from a file (entirely).
	 * 
	 * @param fn File name (path).
	 * @return The whole text found in the file.
	 * @throws IOException
	 */
	public static String readText(String fn, Charset charset) throws IOException {
		StringBuffer text = new StringBuffer();
		FileInputStream fin = new FileInputStream(fn);
		int size = 1024;
		byte[] buffer = new byte[size];
		while (0 < (size = fin.read(buffer))) {
			text.append(new String(buffer, 0, size, charset));
		}
		fin.close();
		return text.toString();
	}

	
	public static String readText(InputStream in, Charset charset) throws IOException {
		int size = 1024;
		byte[] buffer = new byte[size];
		ByteArrayList out = new ByteArrayList(1024);
		while (0 < (size = in.read(buffer))) {
			out.add(buffer, size);
		}
		byte[] bytes = out.getBuffer();
		size = out.getSize();
		if (bytes == null || bytes.length == 0) {
			return "";
		} else {
			return new String(bytes, 0, size, charset);
		}
	}
	
	/** 
	 * Writes text to a file (appends).
	 * 
	 * @param fn File name (path).
	 * @return The whole text found in the file.
	 * @throws IOException
	 */
	public static void writeText(String text, String fn) throws IOException {
		FileOutputStream fout = new FileOutputStream(fn);
		fout.write(text.getBytes());
		fout.close();
	}

	public static boolean exists(String filename) {
		return new File(filename).exists();
	}

	/** Temporary directory which is deleted on exit. */
	public static File createTempDir(String dirname, boolean unique) throws IOException {
		String tempDir = getTempDir();
		return createTempSubDir(tempDir, dirname, unique);
	}

	public static File createLocalTempDir(File location, String dirname, boolean unique) throws IOException {
		String tempDir = getLocalTempDir(location);
		return createTempSubDir(tempDir, dirname, unique);
	}

	/** Tries to determin a location on the same drive as 
	 * location which allows write access. 
	 * @throws IOException 
	 * */
	private static String getLocalTempDir(File location) throws IOException {
		FileStore store = Files.getFileStore(location.toPath());
		// try system temp dir first
		String tmp = getTempDir();
		File dir = new File(tmp);
		if (!store.equals(Files.getFileStore(dir.toPath()))) {
			// failed. Try the parent directory of the given location next
			dir = location.getParentFile();
			if (!store.equals(Files.getFileStore(dir.toPath()))) {
				// give up
				throw new FileNotFoundException("Could not locate a location for "
						+ "temporary files on the same drive as " + location);
			}
		}
		
		
		return dir.getCanonicalPath();
	}
	public static File createTempSubDir(String tempDir, String subdirname, boolean unique) throws IOException {
		File dir;
		if (unique) {
			do {
				Random rand = new Random();
				dir = new File(tempDir + File.separatorChar + subdirname + "." + rand.nextInt(999999));
			
			} while(dir.exists());
		} else {
			dir = new File(tempDir + File.separatorChar + subdirname);
		}
		dir.mkdir();
		dir.deleteOnExit();
		return dir;
	}

	
	/** 
	 * @return Returns the system specific temp directory (e.g. /tmp for UNIX).
	 * @throws FileNotFoundException
	 */
	private static String getTempDir() throws FileNotFoundException {
		if (exists(UNIX_STD_TMP)) return UNIX_STD_TMP;
		Map<String, String> env = System.getenv();
		String tmpdir = env.get("TEMP");
		if (exists(tmpdir)) return tmpdir;

		throw new FileNotFoundException("no system temp directory found");
	}

	public static boolean isSpecialFileName(String name) {
		return name.equals(".") || name.equals("..");
	}

	public static String getCanonicalPath(String file) throws IOException {
		if (file.length() == 0) file = ".";
		File f = Shell.resolveFileName(file);
		return f.getCanonicalPath();
	}

	/** Copies from one location to another.
	 * 
	 * @param src
	 * @param trg
	 * 
	 * @throws IOException 
	 * 
	 * 
	 */
	public static void cp(File src, File trg) throws IOException {
		cp(src, trg, new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return true;
			}
		});
	}

	
	public static void cp(File src, File trg, FileFilter exclude) throws IOException {
		src = src.getCanonicalFile();
		trg = trg.getCanonicalFile();
		
		if (!exclude.accept(src)) return;

		
		if (src.isDirectory()) {
			if (!trg.isDirectory()) throw new IOException("if source is a directory target has to be a directory as well");
			File targetDir = new File(trg.toString() + File.separatorChar + src.getName());
			targetDir.mkdir();
			for (File f : src.listFiles(exclude)) {
				cp(f, targetDir, exclude);
			}
		} else {
			if (trg.isDirectory()) {
				File targetFile = new File(trg.toString() + File.separatorChar + src.getName());
				targetFile.createNewFile();
				cp(src, targetFile, exclude);
			} else {
				FileInputStream in = new FileInputStream(src);
				FileOutputStream out = new FileOutputStream(trg);
				FileSystem.dump2stream(in, out);
				in.close();
				out.close();
			}
		}
		
	}

	/** Removes the given parent part of the given path 
	 * 
	 * @param parentPath Portion of the file path to be removed from 'path'.
	 * @param path Path to be modified.
	 * @return The modified path (relative to parent path).
	 */
	static public File removeParentPath(File parentPath, File path) {
		String p = parentPath.toString();
		String relative = path.toString();
		if (p.equals(relative)) return removeParentPath(parentPath.getParentFile(), path);
		if (!relative.startsWith(p)) throw new IllegalArgumentException("File '" + relative + "' does not contain the prefix '" + parentPath + "'");
		return new File(relative.substring(parentPath.toString().length()+1));
	}
	
	/**
	 * Determines the file name extension of the given file.
	 * 
	 * @param file File whose extension is requested.
	 * @return File name extension. Returns null, if there was no extension.
	 */
	public static String getFileNameExtension(File file) {
		String filename = file.getName();
		int extIdx = filename.lastIndexOf('.');
		if (extIdx < 0 || extIdx+1 >= filename.length()) return null;
		return filename.substring(extIdx+1);
	}

	public static boolean samePaths(File p1, File p2) throws IOException {
		boolean p1exists = p1.exists();
		boolean p2exists = p2.exists();
		if (p1exists && p2exists) {
			return Files.isSameFile(p1.toPath(), p2.toPath());
		} else if (p1exists || p2exists) {
			// if just one of the files exist, they cannot be equal
			return false;
		} else {
			// none of both exist: compare paths
			return p1.getCanonicalPath().equals(p2.getCanonicalPath());
		}
	}

	public static File combineCanonicalPath(File parent, File relative) throws IOException {
		File result = new File(parent.toString() + File.separatorChar + relative.toString());
		return result.getCanonicalFile();
	}

	/**
	 * Deletes files and directories (recursive)
	 * @param path
	 */
	public static void delete(File path) {
		if (path.isDirectory()) {
			for (File f : path.listFiles()) delete(f);
		}
		path.delete();
	}

	public static File getCanonicalFile(String filename) throws IOException {
		return new File(getCanonicalPath(filename));
	}

	/**
	 * Enumerates all files recursively.
	 * @param dir
	 * @return list of files
	 */
	public static File[] listRecursive(File dir) {
		ArrayList<File> list = new ArrayList<File>(128);
		listRecursive(dir, list);
		return (File[]) list.toArray(new File[]{});
	}
	
	/**
	 * Enumerates all files recursively.
	 * @param dir
	 * @param list of files
	 */
	public static void listRecursive(File parent, ArrayList<File> list) {
		list.add(parent);
		if (parent.isDirectory()) {
			for (File f : parent.listFiles()) {
				listRecursive(f, list);
			}
		}
	}
	public static void mv(File source, File target, boolean override) throws IOException {
		if (override) {
			Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} else {
			Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
		}
	}

	public static File getUserHome() {
		return new File(System.getProperty("user.home"));
	}
	public static String getStandardisedPath(File location) {
		return location.getPath().replace('\\', URL_PATH_SEPARATOR);
	}
	public static boolean hasAccessibleParent(File wd) {
		File dir = wd;
		while (!dir.exists() && dir.getParentFile() != null) dir = dir.getParentFile();
		
		return Files.isExecutable(dir.toPath()) && Files.isReadable(dir.toPath()) && Files.isWritable(dir.toPath());
	}
	public static boolean isChildOf(File child, File potentialParent) {
		String standardizedChild = FileSystem.getStandardisedPath(child);
		String standardizedParent = FileSystem.getStandardisedPath(potentialParent);
		return standardizedChild.startsWith(standardizedParent);
	}




}
