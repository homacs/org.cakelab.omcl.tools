package org.cakelab.omcl.plugins.minecraft.launcher;

import java.io.File;
import java.net.Proxy;

import org.cakelab.omcl.plugins.ServicesBase;
import org.cakelab.omcl.plugins.StubException;
import org.cakelab.omcl.plugins.interfaces.ServicesListener;
import org.cakelab.omcl.plugins.utils.UnsafeAllocator;

public class LauncherServices extends ServicesBase {
	
	
	public LauncherServices(ServicesListener listener) throws StubException {
		super(listener);
	}
	
	
	public boolean installVersion(String version, File workDir) throws StubException {
		try {
			super.prepareContext();
			UnsafeAllocator allocator = UnsafeAllocator.create();
			ModdedLauncher launcher =  (ModdedLauncher) allocator.newInstance(ModdedLauncher.class);
			String[] leftoverArgs = new String[0];
			launcher.init(listener, workDir, Proxy.NO_PROXY, null, leftoverArgs, Integer.valueOf(100));
			launcher.installVersion(version);
			return launcher.getResult();
		} catch (Throwable t) {
			throw new StubException(t);
		} finally {
			super.resetContext();
		}
	}
	

	public boolean launchSelectedProfile(File workDir) throws StubException {
		super.prepareContext();
		try {
			UnsafeAllocator allocator = UnsafeAllocator.create();
			ModdedLauncher launcher =  (ModdedLauncher) allocator.newInstance(ModdedLauncher.class);
			String[] leftoverArgs = new String[0];
			launcher.init(listener, workDir, Proxy.NO_PROXY, null, leftoverArgs, Integer.valueOf(100));
			
			// Init should have ensured that the user is logged in
			// but it is possible that he denied to login intending 
			// to abort the launch. In that case it is no error.
			if (!launcher.isLoggedIn()) { 
				listener.info("user canceled launch", null);
				return true;
			}
			launcher.launchSelectedProfile();
			return launcher.getResult();
		} catch (Throwable t) {
			throw new StubException(t);
		} finally {
			super.resetContext();
		}
		
	}
	
	
	
	public static File getWorkingDirectory() {
		String userHome = System.getProperty("user.home", ".");

		File workingDirectory;
		switch (com.mojang.launcher.OperatingSystem.getCurrentPlatform()) {
		case LINUX:
			workingDirectory = new File(userHome, ".minecraft/");
			break;
		case WINDOWS:
			String applicationData = System.getenv("APPDATA");
			String folder = applicationData != null ? applicationData
					: userHome;

			workingDirectory = new File(folder, ".minecraft/");
			break;
		case OSX:
			workingDirectory = new File(userHome,
					"Library/Application Support/minecraft");
			break;
		default:
			workingDirectory = new File(userHome, "minecraft/");
		}

		return workingDirectory;
	}
	
	
	public static void main (String [] args) throws StubException {
		File workDir = new File(System.getProperty("user.home"), ".litwrl" + File.separator + "minecraft");
		LauncherServices services = new LauncherServices(null);
		services.launchSelectedProfile(workDir);
		System.exit(0);
	}
}