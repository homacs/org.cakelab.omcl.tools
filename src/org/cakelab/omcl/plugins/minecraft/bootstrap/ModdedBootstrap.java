package org.cakelab.omcl.plugins.minecraft.bootstrap;

import java.awt.Font;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.nio.channels.FileChannel;
import java.security.DigestInputStream;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.minecraft.bootstrap.FatalBootstrapError;

import org.cakelab.omcl.plugins.interfaces.ServicesListener;

public class ModdedBootstrap extends JFrame {
	private static final long serialVersionUID = 1L;

	private static final Font MONOSPACED = new Font("Monospaced", 0, 12);

	public static final String LAUNCHER_URL = "https://s3.amazonaws.com/Minecraft.Download/launcher/launcher.pack.lzma";
	private final File workDir;
	private final Proxy proxy;
	private final File launcherJar;
	private final File packedLauncherJar;
	private final File packedLauncherJarNew;
	private final JTextArea textArea;
	private final JScrollPane scrollPane;
	private final PasswordAuthentication proxyAuth;
	private final String[] remainderArgs;
	private final static StringBuilder outputBuffer = new StringBuilder();

	private ServicesListener listener;

	public ModdedBootstrap(File workDir, Proxy proxy,
			PasswordAuthentication proxyAuth, String[] remainderArgs, ServicesListener listener) {
		super("Minecraft Launcher");
		this.workDir = workDir;
		this.proxy = proxy;
		this.proxyAuth = proxyAuth;
		this.remainderArgs = remainderArgs;
		this.listener = listener;
		launcherJar = new File(workDir, "launcher.jar");
		packedLauncherJar = new File(workDir, "launcher.pack.lzma");
		packedLauncherJarNew = new File(workDir,
				"launcher.pack.lzma.new");

		setSize(854, 480);
		setDefaultCloseOperation(3);

		textArea = new JTextArea();
		textArea.setLineWrap(true);
		textArea.setEditable(false);
		textArea.setFont(MONOSPACED);
		((javax.swing.text.DefaultCaret) textArea.getCaret())
				.setUpdatePolicy(1);

		scrollPane = new JScrollPane(textArea);
		scrollPane.setBorder(null);
		scrollPane.setVerticalScrollBarPolicy(22);

		add(scrollPane);
		setLocationRelativeTo(null);
		setVisible(false);

		println("Bootstrap (v5)");
		println("Current time is "
				+ java.text.DateFormat.getDateTimeInstance(2, 2,
						java.util.Locale.US).format(new java.util.Date()));
		println("System.getProperty('os.name') == '"
				+ System.getProperty("os.name") + "'");
		println("System.getProperty('os.version') == '"
				+ System.getProperty("os.version") + "'");
		println("System.getProperty('os.arch') == '"
				+ System.getProperty("os.arch") + "'");
		println("System.getProperty('java.version') == '"
				+ System.getProperty("java.version") + "'");
		println("System.getProperty('java.vendor') == '"
				+ System.getProperty("java.vendor") + "'");
		println("System.getProperty('sun.arch.data.model') == '"
				+ System.getProperty("sun.arch.data.model") + "'");
		println("");
	}

	public void execute(boolean force) {
		if (packedLauncherJarNew.isFile()) {
			println("Found cached update");
			renameNew();
		}

		Downloader.Controller controller = new Downloader.Controller();

		if ((force) || (!packedLauncherJar.exists())) {
			Downloader downloader = new Downloader(controller, this,
					proxy, null, packedLauncherJarNew);
			downloader.run();

			if (controller.hasDownloadedLatch.getCount() != 0L) {
				throw new FatalBootstrapError(
						"Unable to download while being forced");
			}

			renameNew();
		} else {
			String md5 = getMd5(packedLauncherJar);

			Thread thread = new Thread(new Downloader(controller, this, proxy,
					md5, packedLauncherJarNew));
			thread.setName("Launcher downloader");
			thread.start();
			try {
				println("Looking for update");
				boolean wasInTime = controller.foundUpdateLatch.await(3L,
						java.util.concurrent.TimeUnit.SECONDS);

				if (controller.foundUpdate.get()) {
					println("Found update in time, waiting to download");
					controller.hasDownloadedLatch.await();
					renameNew();
				} else if (!wasInTime) {
					println("Didn't find an update in time.");
				}
			} catch (InterruptedException e) {
				throw new FatalBootstrapError("Got interrupted: "
						+ e.toString());
			}
		}

		unpack();
	}

	public void unpack() {
		File lzmaUnpacked = getUnpackedLzmaFile(packedLauncherJar);
		InputStream inputHandle = null;
		java.io.OutputStream outputHandle = null;

		println("Reversing LZMA on " + packedLauncherJar + " to "
				+ lzmaUnpacked);
		try {
			inputHandle = new LZMA.LzmaInputStream(new FileInputStream(
					packedLauncherJar));
			outputHandle = new FileOutputStream(lzmaUnpacked);

			byte[] buffer = new byte[65536];

			int read = inputHandle.read(buffer);
			while (read >= 1) {
				outputHandle.write(buffer, 0, read);
				read = inputHandle.read(buffer);
			}
		} catch (Exception e) {
			throw new FatalBootstrapError("Unable to un-lzma: " + e);
		} finally {
			closeSilently(inputHandle);
			closeSilently(outputHandle);
		}

		println("Unpacking " + lzmaUnpacked + " to " + launcherJar);

		java.util.jar.JarOutputStream jarOutputStream = null;
		try {
			jarOutputStream = new java.util.jar.JarOutputStream(
					new FileOutputStream(launcherJar));
			java.util.jar.Pack200.newUnpacker().unpack(lzmaUnpacked,
					jarOutputStream);
		} catch (Exception e) {
			throw new FatalBootstrapError("Unable to un-pack200: " + e);
		} finally {
			closeSilently(jarOutputStream);
		}

		println("Cleaning up " + lzmaUnpacked);

		lzmaUnpacked.delete();
	}

	public static void closeSilently(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException ignored) {
			}
		}
	}

	private File getUnpackedLzmaFile(File packedLauncherJar) {
		String filePath = packedLauncherJar.getAbsolutePath();
		if (filePath.endsWith(".lzma")) {
			filePath = filePath.substring(0, filePath.length() - 5);
		}
		return new File(filePath);
	}

	public String getMd5(File file) {
		DigestInputStream stream = null;
		try {
			stream = new DigestInputStream(new FileInputStream(file),
					java.security.MessageDigest.getInstance("MD5"));
			byte[] buffer = new byte[65536];

			int read = stream.read(buffer);
			while (read >= 1)
				read = stream.read(buffer);
		} catch (Exception ignored) {
			return null;
		} finally {
			closeSilently(stream);
		}

		return String.format("%1$032x",
				new Object[] { new java.math.BigInteger(1, stream
						.getMessageDigest().digest()) });
	}

	public void println(String string) {
		print(string + "\n");
	}

	public void print(String string) {
		if (string.contains("\n")) {
			String[] lines = string.split("\n");
			int i = 0;
			if (outputBuffer.length() != 0) {
				listener.info(outputBuffer.toString() + lines[i], null);
				i++;
			}
			if (i < lines.length) {
				for (; i < lines.length-1; i++) {
					listener.info(lines[i], null);
				}
				if (string.endsWith("\n")) {
					listener.info(lines[i], null);
				} else {
					outputBuffer.append(lines[i]);
				}
			}
		} else {
			outputBuffer.append(string);
		}

	}

	public void startLauncher(File launcherJar) {
		println("Starting launcher.");
		try {
			@SuppressWarnings("resource")
			Class<?> aClass = new java.net.URLClassLoader(
					new java.net.URL[] { launcherJar.toURI().toURL() })
					.loadClass("net.minecraft.launcher.Launcher");
			java.lang.reflect.Constructor<?> constructor = aClass
					.getConstructor(new Class[] { JFrame.class, File.class,
							Proxy.class, PasswordAuthentication.class,
							String[].class, Integer.class });
			constructor.newInstance(new Object[] { this, workDir, proxy,
					proxyAuth, remainderArgs, Integer.valueOf(5) });
		} catch (Exception e) {
			throw new FatalBootstrapError("Unable to start: " + e);
		}
	}

	public void renameNew() {
		if ((packedLauncherJar.exists()) && (!packedLauncherJar.isFile())
				&& (!packedLauncherJar.delete())) {
			throw new FatalBootstrapError("while renaming, target path: "
					+ packedLauncherJar.getAbsolutePath()
					+ " is not a file and we failed to delete it");
		}

		if (packedLauncherJarNew.isFile()) {
			println("Renaming " + packedLauncherJarNew.getAbsolutePath()
					+ " to " + packedLauncherJar.getAbsolutePath());

			if (packedLauncherJarNew.renameTo(packedLauncherJar)) {
				println("Renamed successfully.");
			} else {
				if ((packedLauncherJar.exists())
						&& (!packedLauncherJar.canWrite())) {
					throw new FatalBootstrapError("unable to rename: target"
							+ packedLauncherJar.getAbsolutePath()
							+ " not writable");
				}

				println("Unable to rename - could be on another filesystem, trying copy & delete.");

				if ((packedLauncherJarNew.exists())
						&& (packedLauncherJarNew.isFile())) {
					try {
						copyFile(packedLauncherJarNew, packedLauncherJar);
						if (packedLauncherJarNew.delete()) {
							println("Copy & delete succeeded.");
						} else {
							println("Unable to remove "
									+ packedLauncherJarNew.getAbsolutePath()
									+ " after copy.");
						}
					} catch (IOException e) {
						throw new FatalBootstrapError("unable to copy:" + e);
					}
				} else {
					println("Nevermind... file vanished?");
				}
			}
		}
	}

	public static void copyFile(File source, File target) throws IOException {
		if (!target.exists()) {
			target.createNewFile();
		}

		FileInputStream in = null;
		FileOutputStream out = null;
		try {
			in = new FileInputStream(source);
			out = new FileOutputStream(target);
			FileChannel sourceChannel = null;
			FileChannel targetChannel = null;
			sourceChannel = in.getChannel();
			targetChannel = out.getChannel();
			targetChannel.transferFrom(sourceChannel, 0L, sourceChannel.size());
		} finally {
			if (in != null) {
				in.close();
			}

			if (out != null) {
				out.close();
			}
		}
	}


	public static boolean stringHasValue(String string) {
		return (string != null) && (!string.isEmpty());
	}
}
