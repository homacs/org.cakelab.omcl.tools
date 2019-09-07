package org.cakelab.omcl;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class ContextClassLoader extends URLClassLoader {
	
	/**
	 * List of all packages exported by this class loader.
	 */
	private String[] exports;

	/**
	 * List of all packages migrated from the application 
	 * context to this class loader.
	 */
	private String[] migrates;

	private static volatile ClassLoader pluginContext = null;
	
	static class ThreadContext {
		/** 
		 * We keep track of recursive calls to prevent life locks
		 * using this set. It contains the names of classes or
		 * resources we are currently loading. If we detect
		 * a call to for example loadClass with a FQN class name
		 * which is stored in this list then we ignore the call.
		 */
		Set<String> delegatedCalls = new HashSet<String>();

		boolean initial = false;
	}
	
	/**
	 * This thread local value indicates if this class loader 
	 * was called to load a given class in the role as the initial loader.
	 * Since multiple threads can call the same class loader to load
	 * different classes at the same time, we need to keep this value
	 * thread specific.
	 */
	private ThreadLocal<ThreadContext> threadContext = new ThreadLocal<ThreadContext>();

	ThreadContext getThreadContext() {
		ThreadContext c = threadContext.get();
		if (c == null) {
			c = new ThreadContext();
			threadContext.set(c);
		}
		return c;
	}
	
	private boolean isInitial() {
		return getThreadContext().initial;
	}

	private void setInitial(boolean value) {
		getThreadContext().initial = value;
	}
	

	
	
	static {
		/* 
		 * This is required to let us use the 
		 * method getClassLoadingLock! It is the
		 * only way to support parallelism with 
		 * class loaders possibly delegating to
		 * child class loaders.
		 */
		ClassLoader.registerAsParallelCapable();
	}
	
	
    public ContextClassLoader(URL[] classpath, String[] exports, ClassLoader parent) {
    	this(classpath, exports, null, parent);
    }

    
    public ContextClassLoader(URL[] classpath, String[] exports, String[] migrates,
			ClassLoader parent) {
        super(classpath, parent);
		this.exports = (exports == null ? new String[0] : exports);
		this.migrates = (migrates == null ? new String[0] : migrates);
	}


	public static synchronized void setPluginContext(ClassLoader _pluginContext) {
    	assert(_pluginContext != null);
    	assert(ContextClassLoader.pluginContext == null);
    	ContextClassLoader.pluginContext = _pluginContext;
    }
    
    public static synchronized void resetPluginContext() {
    	assert(pluginContext != null);
    	pluginContext = null;
    }
    
    
    
	private boolean canDelegate(String name, boolean condition, ClassNotFoundException e) throws ClassNotFoundException {
    	if (canDelegate(name, condition)) {
    		return true;
    	} else {
    		throw e;
    	}
	}

	private boolean canDelegate(String name, boolean condition, NoClassDefFoundError e) throws NoClassDefFoundError {
    	if (canDelegate(name, condition)) {
    		return true;
    	} else {
    		throw e;
    	}
	}


	private boolean canDelegate(String name, boolean condition) {
		if (!condition) return false;
		Set<String> delegatedCalls = getThreadContext().delegatedCalls;
    	if (       (pluginContext != null) 
    			&& (pluginContext != this)
    			&& (!delegatedCalls.contains(name))) 
    	{
    		delegatedCalls.add(name);
    		return true;
    	} else {
    		return false;
    	}
	}

    private void endDelegate(String name) {
    	getThreadContext().delegatedCalls.remove(name);
	}


    
    
    
	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		synchronized (getClassLoadingLock(name)) {
			if (isInitial()) return super.loadClass(name);
			setInitial(true);
			Class<?> c;
			try {
				c = super.loadClass(name);
			} finally {
				setInitial(false);
			}
			return c;
		}
	}


	@Override
    protected Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
		synchronized (getClassLoadingLock(name)) {
            if (name.equals(ContextClassLoader.class.getCanonicalName())) {
            	/*
            	 * This is needed to prevent the ContextClassLoader class to be loaded
            	 * again after we have replaced the system class loader.
            	 */
            	return ContextClassLoader.class;
            }
			//
			// Has this class already been loaded by us 
			// as initiating loader?
			//
	        Class<?> c = super.findLoadedClass(name);
	        if (c == null) {
	        	
	        	// if the class is part of the migrated packages, then 
	        	// do a shortcut here and search our own domain.
				if (checkMigrated(name)) return findClass(name);

		    	try {
			            
						try {
			                c = super.loadClass(name, resolve);
						} catch (ClassNotFoundException e) {
			    	        if (canDelegate(name, c == null, e)) {
			    	        	c = pluginContext.loadClass(name);
			    	        }
			            } catch (NoClassDefFoundError e) {
			    	        if (canDelegate(name, c == null, e)) {
			    	        	c = pluginContext.loadClass(name);
			    	        }
				        }
			        	if (resolve) {
			        		resolveClass(c);
			        	}
		    	} finally {
		    		endDelegate(name);
		    	}
	        }
	        return c;
		}
    }

	


	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		synchronized (getClassLoadingLock(name)) {
			if (!isInitial()) checkExport(name);
			return super.findClass(name);
		}
	}

	/** This method checks if the given class name contains a package name
	 * which is mentioned in the list of exported packages.
	 * @param name Full qualified class name
	 * @throws ClassNotFoundException thrown if the package is not exported.
	 */
	private void checkExport(String name) throws ClassNotFoundException {
		for (String export : exports) {
			if (name.startsWith(export)) {
				return;
			}
		}
		throw new ClassNotFoundException("this class loader does not export class " + name + " ");
	}

	/** This method checks whether the given class name contains a package
	 * name which was migrated to this class loader and has to be loaded 
	 * by this class loader first.
	 */
	private boolean checkMigrated(String name) {
		for (String immigrant : migrates) {
			if (name.startsWith(immigrant)) {
				return true;
			}
		}
		return false;
	}


	@Override
    public URL getResource(String name) {
        URL url = null;
       	url = super.getResource(name);
       	if (canDelegate(name, url == null)) {
       		try {
       			url = pluginContext.getResource(name);
       		} finally {
       			endDelegate(name);
       		}
       	}
        return url;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {

        
        Enumeration<URL> tmp = super.getResources(name);
        
        
        if (canDelegate(name, true)) {
        	try {
	            final List<URL> urls = new ArrayList<URL>();
	            while(tmp.hasMoreElements()) {
	            	urls.add(tmp.nextElement());
	            }
	            
	            tmp = pluginContext.getResources(name);
	            while(tmp.hasMoreElements()) {
	            	urls.add(tmp.nextElement());
	            }
	            
	            
	            Enumeration<URL> result = new Enumeration<URL>() {
	                Iterator<URL> iter = urls.iterator();
	
	                public boolean hasMoreElements() {
	                    return iter.hasNext(); 
	                }
	                public URL nextElement() {
	                    return iter.next();
	                }
	            };
	            
	            return result;
        	} finally {
        		endDelegate(name);
        	}
        } else {
        	return tmp;
        }
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        URL url = getResource(name);
        try {
            return url != null ? url.openStream() : null;
        } catch (IOException e) {
        }
        return null;
    }
    
    
    
    
    /**
     * This method technically replaces the system class loader with an 
     * instance of the ContextClassLoader and calls the main method of the
     * main class provided. The context class loader inherits the class path 
     * set in system property "java.class.path" and will be responsible to 
     * resolve classes from this class path from now on.
     * 
     * @param canonicalMainClassName
     * @param args
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public static void bootstrap(String canonicalMainClassName, String[] exports, String [] args) throws IOException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		URL[] classpath = inheritClassPath(System.getProperty("java.class.path"));
		
		
		
		ClassLoader parent = ClassLoader.getSystemClassLoader();
		while (parent.getParent() != null) {
			parent = parent.getParent();
		}
		
		/* it will be closed on exit */
		ContextClassLoader baseClassLoader = new ContextClassLoader (classpath, exports, parent);
		Thread.currentThread().setContextClassLoader(baseClassLoader);

		Class<?> clazz = baseClassLoader.loadClass(canonicalMainClassName);
		
		
		
		Method mainMethod = clazz.getMethod("main", String[].class);
		Object ignored = null;
		mainMethod.invoke(ignored, (Object)args);
    }
    
    private static URL[] inheritClassPath(String classpath) {
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



}