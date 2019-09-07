package org.cakelab.omcl.utils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class Classpath {
	
	public static class ClasspathEntry {
		public static enum Type {
			JAR,
			DIR,
			UNKNOWN
		}
		private Type type;
		private URL url;
		
		public ClasspathEntry(Type type, URL url) {
			this.type = type;
			this.url = url;
		}
		
		public File getDirectory() throws IOException {
			switch (type) {
			case JAR:
				return new File(url.getPath()).getParentFile();
			case DIR:
				return new File(url.getPath());
			default:
				throw new IOException("classpath entry does not refer to a local hard drive");
			}
		}

		public URL getUrl() {
			return url;
		}
		
	}


	private static final char URL_PATH_SEPARATOR = '/';
	
	
	private ArrayList<ClasspathEntry> entries = new ArrayList<ClasspathEntry>();
	
	
	public Classpath(URL[] urls) {
		for (URL url : urls) {
			File f = new File(url.getPath());
			if (f.exists()) {
				if (f.isDirectory()) {
					entries.add(new ClasspathEntry(ClasspathEntry.Type.DIR, url));
				} else {
					entries.add(new ClasspathEntry(ClasspathEntry.Type.JAR, url));
				}
			} else {
				entries.add(new ClasspathEntry(ClasspathEntry.Type.UNKNOWN, url));
			}
		}
	}

	public static Classpath fromPath(String classpath) {
		return new Classpath(toUrls(classpath));
	}
	
	public URL[] getURLs() {
		URL[] result = new URL[entries.size()];
		for (int i = 0; i < entries.size(); i++) {
			result[i] = entries.get(i).url;
		}
		return result;
	}

    private static URL[] toUrls(String classpath) {
    	String[] entries = classpath.split(File.pathSeparator);
    	URL[] urls = new URL[entries.length];
    	for (int i = 0; i < entries.length; i++) {
    		try {
    			String entry = entries[i];
    			File f = new File(entry);
    			if (f.isDirectory()) entry = entry + '/';
				urls[i] = new URL("file", "", entry);
			} catch (MalformedURLException e) {
				// this can happen if the classpath given to 
				// the JVM was already malicious.
				// We will skip those entries hoping that they
				// are actually not needed.
				System.err.println("malicious classpath entry ignored.");
				e.printStackTrace();
			}
    	}
		return urls;
	}

    

	/**
	 * This method determines for the given class 'clazz' either the directory 
	 * where the jar file is located or the directory where the class not in a 
	 * jar is located.
	 * @param clazz
	 * @return
	 * @throws IOException 
	 */
	@SuppressWarnings("deprecation")
	public static ClasspathEntry determineClasspathEntry(Class<?> clazz) throws IOException {
		// TODO: consider using original urls with "jar:" and "file:" in ClasspathEntry.
		URL url = clazz.getResource(clazz.getSimpleName() + ".class");
		try {
			
			ClasspathEntry.Type type;
			// Windows compatibility:
			// Regular expression parser turns '\\' in '\' and everything gets screwed.
			// To make things easier, we turn backslashes into a slashes before 
			// we start manipulating the path.


			if(url.getProtocol().equals("jar")) {
				type = ClasspathEntry.Type.JAR;
				url = new URL(url.toString().replaceFirst("jar:", ""));
			} else if (url.getProtocol().equals("file")) {
				type = ClasspathEntry.Type.DIR;
			} else {
				type = ClasspathEntry.Type.UNKNOWN;
			}
			
			String urlPath = url.getPath();
			
			// now cut of the package and class name of the received path.
			String fqnPortion = clazz.getName().replace('.', URL_PATH_SEPARATOR) + ".class";
			urlPath = urlPath.replaceAll(fqnPortion + "$", "");


			// If it is inside a jar file, the remaining path will end 
			// with "<jarfilename>!\". Thus, we have to cut off as well.
			if (type.equals(ClasspathEntry.Type.JAR)) {
				urlPath = urlPath.replaceAll("!/$", "");
			}
			
			return new ClasspathEntry(type, new File(urlPath).toURL());
		} catch (IOException e) {
			throw e;
		} catch (Throwable e) {
			throw new IOException(e);
		}
	}

}
