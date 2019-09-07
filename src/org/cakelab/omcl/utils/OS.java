package org.cakelab.omcl.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class OS {
	
	
	
	
	public enum OSFamily {
		WINDOWS,
		LINUX,
		SOLARIS,
		MACOS,
		UNKNOWN
	}

	private static final long MEM_ERROR = -1;
	private static final long MEM_UNDEF = -2;

	/* max available memory [bytes] in JVM with 32bit address width. */
	private static final long MEM_MAX_TOTAL_32BIT = 3L*1024*1024*1024;

	
	private static long totalMem  = MEM_UNDEF;
	private static long physMem   = MEM_UNDEF;
	private static long totalSwap = MEM_UNDEF;
	private static long freeSwap  = MEM_UNDEF;
	
	
	public static OSFamily getOSFamily() {
	    String osname = System.getProperty("os.name", "unknown").toLowerCase();
	    if (osname.startsWith("windows")) {
	      return OSFamily.WINDOWS;
	    }
	    else if (osname.startsWith("linux")) {
	      return OSFamily.LINUX;
	    }
	    else if (osname.startsWith("sunos")) {
	      return OSFamily.SOLARIS;
	    }
	    else if (osname.startsWith("mac") || osname.startsWith("darwin")) {
	      return OSFamily.MACOS;
	    }
	    else return OSFamily.UNKNOWN;
	}

	public static boolean isUnixDerivate() {
		return isLinux() || isMac() || isSolaris();
	}
	

	public static boolean isWindows() {
		return getOSFamily().equals(OSFamily.WINDOWS);
	}


	public static boolean isLinux() {
		OSFamily family = getOSFamily();
		return family.equals(OSFamily.LINUX);
	}


	public static boolean isMac() {
		OSFamily family = getOSFamily();
		return family.equals(OSFamily.MACOS);
	}

	
	public static boolean isSolaris() {
		return getOSFamily().equals(OSFamily.SOLARIS);
	}
	
	public static File getJavaExecutable() throws IOException {
		String javaHome = System.getProperty("java.home");
		File javaCmd;
		if (OS.isWindows()) {
			javaCmd = new File(javaHome + File.separator + "bin" + File.separator + "java.exe");
		} else {
			javaCmd = new File(javaHome + File.separator + "bin" + File.separator + "java");
		}
		
		if (!javaCmd.exists() || !javaCmd.canExecute() || !javaCmd.canRead()) {
			throw new IOException("file '" + javaCmd + "' does not exist or it is not executable");
		}

		return javaCmd;
	}

	/**
	 * Tries to determine the process ID of the running virtual machine.
	 * This is not supported by some VMs, so this method will throw 
	 * an UnsupportedOperationException in that case.
	 * @return pid (if found)
	 * @throws UnsupportedOperationException (if pid was not found)
	 */
	public static int getpid() throws UnsupportedOperationException {
	    // Note: may fail in some JVM implementations

	    // something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
	    final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
	    final int index = jvmName.indexOf('@');

	    if (index > 0) {
	
		    try {
		        return Integer.parseInt(jvmName.substring(0, index).trim());
		    } catch (NumberFormatException e) {
		        // ignore, i.e. consider it as not supported (see below)
		    }
	    }
        // part before '@' empty (index = 0) / '@' not found (index = -1)
        throw new UnsupportedOperationException("running JVM does not support reading the process ID.");
	}

	public static long getTotalPhysicalMemorySize() {
		if (totalMem == MEM_UNDEF) {
			OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
			try {
				totalMem = invokeMXBMethodL(os,"getTotalPhysicalMemorySize");
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				totalMem = MEM_ERROR;
			}
		}
		return totalMem;
	}
	

	public static long getFreePhysicalMemorySize() {
		if (physMem == MEM_UNDEF) {
	
			OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
			try {
				physMem = invokeMXBMethodL(os,"getFreePhysicalMemorySize");
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				physMem = MEM_ERROR;
			}
		}
		return physMem;
	}
	
	public static long getTotalSwapSpaceSize() {
		if (totalSwap == MEM_UNDEF) {
			
			OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
			try {
				totalSwap = invokeMXBMethodL(os,"getTotalSwapSpaceSize");
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				totalSwap = MEM_ERROR;
			}
		}
		return totalSwap;
	}
	
	public static long getFreeSwapSpaceSize() {
		if (freeSwap == MEM_UNDEF) {
			OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
			try {
				freeSwap = invokeMXBMethodL(os,"getTotalSwapSpaceSize");
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				freeSwap = MEM_ERROR;
			}
		}
		return freeSwap;
	}
	
	
	/** 
	 * Returns the maximum size of memory available considering the architecture
	 * settings of the running JVM.
	 * 
	 * If the JVM is running in 32bit mode, the physical memory size can 
	 * be larger than the memory size the JVM can handle. To be safe the max 
	 * available memory is set to 3GB in case of a 32bit JVM running on a system
	 * with more memory.
	 * 
	 * 
	 * @return available memory in bytes
	 */
	public static long getTotalAvailabMemorySize() {
		long total = getTotalPhysicalMemorySize();
		if (!isArch64bit()) {
			total = MEM_MAX_TOTAL_32BIT;
		}
		return total;
	}

	/**
	 * Tells whether the JVM is running in 64 bit mode or not.
	 * @return
	 */
	private static boolean isArch64bit() {
		String arch = System.getProperty("os.arch");
		return arch.contains("64");
	}

	
	private static long invokeMXBMethodL(OperatingSystemMXBean os, String methodName) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method method_getTotalPhysicalMemorySize;
		method_getTotalPhysicalMemorySize = os.getClass().getMethod(methodName);
		method_getTotalPhysicalMemorySize.setAccessible(true);
		return (long)method_getTotalPhysicalMemorySize.invoke(os);
	}

	public static boolean isKillSupported() {
		return isUnixDerivate() || isWindows();
	}

	/**
	 * Kills a process or the whole process tree (means: all of 
	 * its children, too).
	 * 
	 * Uses system specific command line tools (kill/taskkill) to kill a process.
	 * The command line tool is executed synchronously but some systems
	 * perform the kill (and related release of system resources) asynchronously.
	 * 
	 * Return value is the return value of the command line tool.
	 * 
	 * @param pid ID of the process to kill
	 * @param killTree Whether or not the whole process tree will be killed.
	 * @return 0 on success, !0 on failure (error codes are system dependent)
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static int kill(int pid, boolean killTree) throws InterruptedException, IOException {
		if (isUnixDerivate()) {
			Process p;
			if (killTree) {
				for (int c : findChildren(pid)) {
					kill(c, killTree);
				}
			}
			p = Runtime.getRuntime().exec("kill -9 " + pid);
			return -p.waitFor();
		} else if (isWindows()){
			Process p;
			if (killTree) {
				p = Runtime.getRuntime().exec("taskkill /F /T /PID " + pid);
			} else {
				p = Runtime.getRuntime().exec("taskkill /F /PID " + pid);
			}
			return -p.waitFor();
		} else {
			return -1;
		}
	}

	public static int[] findChildren(int pid) {
		ArrayList<Integer> children = new ArrayList<Integer>();
		// ps --ppid pid
		try {
			Process p = Runtime.getRuntime().exec("ps --ppid " + pid);
			int result = p.waitFor();
			if (result == 0) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
				while (reader.ready()) {
					String line = reader.readLine().trim();
					String token = line.substring(0, line.indexOf(" "));
					try {
						children.add(Integer.decode(token));
					} catch (NumberFormatException e) {
						// first line apparently or errors
					}
				}
			}
		} catch (IOException | InterruptedException e) {
		}
		int[] result = new int[children.size()];
		for (int i = 0; i < result.length; i++) result[i] = children.get(i);
		return result;
	}

	public static int getNumProcessors() {
		return Runtime.getRuntime().availableProcessors();
	}

	
}
