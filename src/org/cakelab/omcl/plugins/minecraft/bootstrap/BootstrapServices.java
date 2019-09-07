package org.cakelab.omcl.plugins.minecraft.bootstrap;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.PasswordAuthentication;
import java.net.Proxy;

import net.minecraft.bootstrap.FatalBootstrapError;

import org.cakelab.omcl.plugins.ServicesBase;
import org.cakelab.omcl.plugins.StubException;
import org.cakelab.omcl.plugins.interfaces.ServicesListener;

public class BootstrapServices extends ServicesBase {

	
	public BootstrapServices(ServicesListener listener) throws StubException {
		super(listener);
	}

	
	public void initWorkDir(File workDir) throws StubException {
		try {
			super.prepareContext();
			
			String previousIPv4StackPropertyValue = System.getProperty("java.net.preferIPv4Stack", "false");
			System.setProperty("java.net.preferIPv4Stack", "true");
			
			
			if ((workDir.exists()) && (!workDir.isDirectory()))
				throw new FatalBootstrapError("Invalid working directory: "
						+ workDir);
			if ((!workDir.exists()) && (!workDir.mkdirs())) {
				throw new FatalBootstrapError("Unable to create directory: "
						+ workDir);
			}
		
			
			PasswordAuthentication passwordAuthentication = null;
	
			String[] remainderArgs = new String[0];
			boolean force = false;
			
			ModdedBootstrap frame = new ModdedBootstrap(workDir, Proxy.NO_PROXY,
					passwordAuthentication, remainderArgs, listener);
			try {
				frame.execute(force);
			} catch (Throwable t) {
				ByteArrayOutputStream stracktrace = new ByteArrayOutputStream();
				t.printStackTrace(new PrintStream(stracktrace));
	
	
				frame.println("FATAL ERROR: " + stracktrace.toString());
				frame.println("\nPlease fix the error and restart.");
				throw t;
			} finally {
				System.setProperty("java.net.preferIPv4Stack", previousIPv4StackPropertyValue);
			}
		} finally {
			super.resetContext();
		}
	}
	
	

	public static boolean stringHasValue(String string) {
		return (string != null) && (!string.isEmpty());
	}
}
