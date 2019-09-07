package org.cakelab.omcl.utils.archive;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import org.cakelab.omcl.utils.FileSystem;



public abstract class Archive {
	public interface Entry {
		boolean isDirectory();
		String getName();
		long getSize();
	}

	private static final FileFilter emptyFilter = new FileFilter() {

		@Override
		public boolean accept(File pathname) {
			return true;
		}};
	static HashMap<String, Factory> registered = new HashMap<String, Factory>(4);
	
	public static class Factory {

		public Constructor<? extends Archive> defaultConstructor;
		public Constructor<? extends Archive> constructorFile;

		public Factory() {
		}

		public Archive create() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			return defaultConstructor.newInstance();
		}

		public Archive create(File archive) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			return constructorFile.newInstance(archive);
		}
		
		
	}

	public static void register(String arName, Class<? extends Archive> clazz) {
		Factory factory = new Factory();
		
		try {
			factory.constructorFile = clazz.getConstructor(File.class);
		} catch (NoSuchMethodException e) {
			/* This error will not occur in production state 
			 * (it's even hard to produce it in development state).
			 * Thus, treat it as a critical internal error. */
			throw new Error(e);
		}
		
		registered.put(arName, factory);
	}
	
	/**
	 * Creates an instance of the archiver selected by the given archiverName. The created archiver 
	 * instance points to an archive identified by the given path 'archive'. This archive don't 
	 * need to exist at the time of the instantiation of the archiver.</br>
	 * </br>
	 * The archiver name is most likely the file name extension used by the archiver, such as "zip" 
	 * for the Zip archiver.
	 * If you are sure, that the file name extension always matches the archiver name, you can use
	 * method .. instead.
	 * 
	 * @param archiverName
	 * @param archive
	 * @return
	 * @throws ArchiverException
	 */
	public static Archive newInstance(String archiverName, File archive) throws ArchiverException {
		Factory factory = registered.get(archiverName);
		if (factory == null) throw new ArchiverException("Archiver '" + archiverName + "' not supported.");
		try {
			return factory.create(archive);
		} catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
			/* This error should occur in development state only.
			 * Critical internal runtime error when occurring in production state. */
			throw new Error(e);
		}
	}
	
	/**
	 * Creates an instance of the archiver for the given archive file. The archive file name 
	 * extension is used for the selection of the appropriate archiver tool chain, so make sure 
	 * that the extension matches the standard file name extension, otherwise you will receive an
	 * exception. </br></br>
	 * The created archiver instance points to an archive identified by the given path 'archive'. 
	 * The archive don't need to exist at the time of the instantiation of the archiver.</br>
	 * </br>
	 * 
	 * @param archiverName
	 * @param archive
	 * @return
	 * @throws ArchiverException
	 */
	public static Archive newInstance(File archive) throws ArchiverException {
		String archiverName = FileSystem.getFileNameExtension(archive);
		if (archiverName == null) throw new ArchiverException("Given archive file '" + archive + "' has an unknown or missing file name extension.");
		return newInstance(archiverName, archive);
	}
	
	
	
	protected String name;
	protected File archive;
	
	public Archive(String name, File archive) {
		this.name = name;
		this.archive = archive;
		
	}

	
	/**
	 * This method creates a new archive and adds the given directory and 
	 * its content to it. Paths in the archive will be relative to the 
	 * given directory removing the part of the parent directory of the path.
	 * 
	 * 
	 * @param dir Directory to be archived.
	 * @throws IOException
	 */
	public void create(File dir) throws IOException {
		create(dir, emptyFilter);
	}
	
	/**
	 * This method creates a new archive and adds the given directory and 
	 * its content to it. Paths in the archive will be relative to the 
	 * given directory removing the part of the parent directory of the path.
	 * 
	 * 
	 * @param dir Directory to be archived.
	 * @param filter Filters files to be excluded from the archive.
	 * @throws IOException
	 */
	public void create(File dir, FileFilter filter) throws IOException {
		File file = FileSystem.removeParentPath(dir.getParentFile(), dir);
		create(dir.getParentFile(), file, filter);
	}
	

	/**
	 * This method creates a new archive and adds the given (sub)directory and 
	 * its content to it. The method combines parentDir and subdir to a full path 
	 * which has to point to an existing directory. This directory will be added 
	 * to the archive. Paths in the archive will be relative to the parentDir to 
	 * prevent you to have absolute paths if you don't want them.
	 * 
	 * 
	 * @param parentDir Path pointing to the parent directory which contains the 
	 *        directory to be archived.
	 * @param subdir Directory to be archived.
	 * @throws IOException
	 */
	public void create(File parentDir, File subdir) throws IOException {
		create(parentDir, subdir, emptyFilter);
	}

	public void create(File parentDir, File[] subdir) throws IOException {
		create(parentDir, subdir, emptyFilter);
	}

	/**
	 * This method creates a new archive and adds the given (sub)directory and 
	 * its content to it. The method combines parentDir and subdir to a full path 
	 * which has to point to an existing directory. This directory will be added 
	 * to the archive. Paths in the archive will be relative to the parentDir to 
	 * prevent you to have absolute paths if you don't want them.
	 * 
	 * 
	 * @param parentDir Path pointing to the parent directory which contains the 
	 *        directory to be archived.
	 * @param subdir Directory to be archived.
	 * @param filter Filters files to be excluded from the archive.
	 * @throws IOException
	 */
	public abstract void create(File parentDir, File subdir, FileFilter filter)
			throws IOException;
	
	public abstract void create(File parentDir, File[] subdir, FileFilter filter)
			throws IOException;
	
	public abstract void create() throws IOException;
	
	public abstract void open() throws IOException;

	public abstract void close() throws IOException;

	public abstract void add(File canonicalFile, File file, FileFilter exclude) throws IOException;

	public abstract void extract(File targetPath, FileFilter exclude, boolean override) throws IOException;
	

	
}
