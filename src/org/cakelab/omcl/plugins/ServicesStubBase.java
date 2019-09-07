package org.cakelab.omcl.plugins;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import org.cakelab.omcl.ContextClassLoader;
import org.cakelab.omcl.plugins.utils.log.LogRelay;
import org.cakelab.omcl.utils.Classpath;



public class ServicesStubBase {
	
	private static ClassLoader previousThreadClassLoader;
	private ClassLoader pluginClassLoader;
	private int recursiveCallDepth = 0;

	
	static {
		PluginServices.init();
	}
	
	
	

	public ServicesStubBase(ClassLoader classLoader) {
		this.pluginClassLoader = classLoader;
	}

	
	
	/**
	 * @param stub Class of the stub to determine the class path entry of this plugin.
	 * @param jarFile Jar file needed by the plugin.
	 * @param migrates Those packages which are migrated from the application 
	 * 			to the plugin and will be loaded in plugin-first order.
	 * @return 
	 * @throws IOException
	 */
	@SuppressWarnings("deprecation")
	protected static URLClassLoader createURLClassLoader(Class<? extends ServicesStubBase> stub, File jarFile, String[] migrates) throws IOException {
		URL myClasspathEntry = Classpath.determineClasspathEntry(stub).getUrl();
		URL omclToolsClasspathEntry = Classpath.determineClasspathEntry(ServicesStubBase.class).getUrl();
		URL[] classpath = new URL[]{omclToolsClasspathEntry, myClasspathEntry, jarFile.toURL()};
		String[] allMigrates = new String[migrates.length + 1];
		allMigrates[0] = LogRelay.MIGRATES_PACKAGE;
		System.arraycopy(migrates, 0, allMigrates, 1, migrates.length);
		ClassLoader parent = ServicesStubBase.class.getClassLoader();
		return new ContextClassLoader(classpath, null, allMigrates, parent);
	}


	/**
	 * This method has to be called on every entry to a method of the stub
	 * which will call a method of the plugged in service interface.
	 */
	protected void enterPluginContext() {
		if (recursiveCallDepth == 0) {
			enterPluginContext(pluginClassLoader);
		}
		recursiveCallDepth++;
	}
	
	
	/**
	 * This method has to be called after every call of a method of the 
	 * plugged in service interface.
	 */
	protected void leavePluginContext() {
		recursiveCallDepth--;
		if (recursiveCallDepth == 0) {
			leavePluginContext(pluginClassLoader);
		}
	}
	
	protected static void enterPluginContext(ClassLoader pluginContextClassLoader) {
		previousThreadClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(pluginContextClassLoader);
		ContextClassLoader.setPluginContext(pluginContextClassLoader);
	}
	

	protected static void leavePluginContext(ClassLoader pluginContextClassLoader) {
		ContextClassLoader.resetPluginContext();
		Thread.currentThread().setContextClassLoader(previousThreadClassLoader);
		previousThreadClassLoader = null;
	}

}
