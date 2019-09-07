package org.cakelab.omcl.plugins.minecraft.launcher;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.LauncherConstants;
import net.minecraft.launcher.profile.AuthenticationDatabase;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;
import net.minecraft.launcher.updater.CompleteMinecraftVersion;
import net.minecraft.launcher.updater.Library;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.logging.log4j.Logger;
import org.cakelab.omcl.plugins.interfaces.ServicesListener;
import org.cakelab.omcl.plugins.utils.ReflectionHelper;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.updater.DateTypeAdapter;
import com.mojang.launcher.updater.VersionManager;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.updater.download.DownloadJob;
import com.mojang.launcher.updater.download.DownloadListener;
import com.mojang.launcher.updater.download.Downloadable;
import com.mojang.launcher.updater.download.assets.AssetIndex;
import com.mojang.launcher.versions.CompleteVersion;
import com.mojang.launcher.versions.Version;
import com.mojang.util.UUIDTypeAdapter;

public class ModdedLauncher extends Launcher {

	private ModdedLauncher(ServicesListener listener, File workingDirectory,
			Proxy proxy, PasswordAuthentication proxyAuth, String[] args) {
		super(listener.getFrame(), workingDirectory, proxy, proxyAuth, args);
		throw new RuntimeException("use unsafe allocator and init instead");
	}

	@SuppressWarnings("unused")
	private static FileFilter FILES = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return FileFileFilter.FILE.accept(pathname);
		}
	};

	private static FileFilter DIRECTORIES = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return DirectoryFileFilter.INSTANCE.accept(pathname);
		}
	};

	private static final Logger LOGGER = org.apache.logging.log4j.LogManager
			.getLogger();

	private Gson gson;

	private boolean finished;

	private ArrayList<DownloadJob> failed;

	private boolean error;


	public void init(ServicesListener listener, File workingDirectory, Proxy proxy,
			PasswordAuthentication proxyAuth, String[] args)
			throws IllegalArgumentException, IllegalAccessException,
			NoSuchFieldException, IOException {
		this.init(listener, workingDirectory, proxy, proxyAuth, args,
				Integer.valueOf(0));
	}

	public void init(ServicesListener listener, File workingDirectory, Proxy proxy,
			PasswordAuthentication proxyAuth, String[] args,
			Integer bootstrapVersion) throws IllegalArgumentException,
			IllegalAccessException, NoSuchFieldException, IOException {
		
		Field INSTANCE_FIELD = Launcher.class.getDeclaredField("INSTANCE");
		INSTANCE_FIELD.setAccessible(true);
		INSTANCE_FIELD.set(null, this);
		
		setupErrorHandling();
		error = false;
		gson = new Gson();
		jobs = new ArrayList<DownloadJob>();
		failed = new ArrayList<DownloadJob>();
		setSuperClassMember("gson", new Gson());
		setSuperClassMember("clientToken", UUID.randomUUID());
		setBootstrapVersion(bootstrapVersion);
		setSuperClassMember("userInterface", new ModdedUserInterface(this, listener));

		if (bootstrapVersion.intValue() < 4) {
			getUserInterface().showOutdatedNotice();
			throw new Error("Outdated bootstrap");
		}

		LOGGER.info(getUserInterface().getTitle() + " (through bootstrap "
				+ bootstrapVersion + ") started on "
				+ OperatingSystem.getCurrentPlatform().getName() + "...");
		LOGGER.info("Current time is "
				+ DateFormat.getDateTimeInstance(2, 2, java.util.Locale.US)
						.format(new Date()));

		if (!OperatingSystem.getCurrentPlatform().isSupported()) {
			listener.showWarning("Minecraft warning:", "This operating system is unknown or unsupported, we cannot guarantee that the game will launch successfully.");
		}
		LOGGER.info("System.getProperty('os.name') == '"
				+ System.getProperty("os.name") + "'");
		LOGGER.info("System.getProperty('os.version') == '"
				+ System.getProperty("os.version") + "'");
		LOGGER.info("System.getProperty('os.arch') == '"
				+ System.getProperty("os.arch") + "'");
		LOGGER.info("System.getProperty('java.version') == '"
				+ System.getProperty("java.version") + "'");
		LOGGER.info("System.getProperty('java.vendor') == '"
				+ System.getProperty("java.vendor") + "'");
		LOGGER.info("System.getProperty('sun.arch.data.model') == '"
				+ System.getProperty("sun.arch.data.model") + "'");
		LOGGER.info("proxy == " + proxy);

		setLaunchDispather(new net.minecraft.launcher.game.GameLaunchDispatcher(
				this, processArgs(args)));
		com.mojang.launcher.Launcher _launcher = new com.mojang.launcher.Launcher(
				getUserInterface(), workingDirectory, proxy, proxyAuth, 
				new net.minecraft.launcher.updater.MinecraftVersionManager(
						new net.minecraft.launcher.updater.LocalVersionList(workingDirectory), 
						new net.minecraft.launcher.updater.RemoteVersionList(LauncherConstants.PROPERTIES.getVersionManifest(), proxy)), com.mojang.authlib.Agent.MINECRAFT, net.minecraft.launcher.game.MinecraftReleaseTypeFactory.instance(), 18);
		setLauncher(_launcher);
		setProfileManager(new ProfileManager(this));

		refreshVersionsAndProfilesSync();

	}

	void installVersion(String requestedVersion) throws IOException {
		setFinished(false);
		VersionManager vm = getLauncher().getVersionManager();
		
		LOGGER.info("Queueing library & version downloads");
		String lastVersionId = requestedVersion;
		
		VersionSyncInfo syncInfo = vm.getVersionSyncInfo(lastVersionId);
		if (syncInfo.getLocalVersion() == null && syncInfo.getRemoteVersion() == null) {
			LOGGER.error("Could not determin version info. Probably no connection.");
			error = true;
			setFinished(true);
			return;
		}
		CompleteVersion completeVersion = vm.getLatestCompleteVersion(syncInfo);
		vm.installVersion(completeVersion);

		if (!syncInfo.isUpToDate()) {
			try {
				getLauncher().getVersionManager().installVersion(
						completeVersion);
			} catch (IOException e) {
				LOGGER.error("Couldn't save version info to install "
						+ syncInfo.getLatestVersion(), e);
				throw e;
			}
		}

		downloadRequiredFiles(syncInfo, completeVersion,
				new DownloadListener() {

					@Override
					public void onDownloadJobFinished(DownloadJob job) {
						LOGGER.info("download job " + job.getName()
								+ " finished  (took " + job.getStopWatch().toString() + ")");
						synchronized (jobs) {
							jobs.remove(job);
							if (job.getFailures() > 0) {
								failed.add(job);
								error = true;
							}
							updateJobState();
						}
					}

					@Override
					public void onDownloadJobProgressChanged(DownloadJob job) {
						updateProgressBar();
						synchronized (jobs) {
							if (job.getFailures() > 0) {
								LOGGER.error("Job '" + job.getName() + "' finished with " + job.getFailures() + " failure(s)! (took " + job.getStopWatch().toString() + ")");
								jobs.remove(job);
								failed.add(job);
							} else {
								if (!hasRemainingJobs()) {
									try {
										updateJobState();
									} catch (Throwable ex) {
										LOGGER.fatal("Fatal error installing minecraft.", ex);
									}
								}
							}
						}
					}

				});

	}

	protected void updateProgressBar() {
		synchronized (jobs) {
			if (hasRemainingJobs()) {
				long total = 0L;
				long current = 0L;
				Downloadable longestRunning = null;

				for (DownloadJob job : jobs) {
					for (Downloadable file : job.getAllFiles()) {
						total += file.getMonitor().getTotal();
						current += file.getMonitor().getCurrent();

						if ((longestRunning == null)
								|| (longestRunning.getEndTime() > 0L)
								|| ((file.getStartTime() < longestRunning
										.getStartTime()) && (file.getEndTime() == 0L))) {
							longestRunning = file;
						}
					}
				}

				getLauncher().getUserInterface().setDownloadProgress(
						new com.mojang.launcher.updater.DownloadProgress(
								current, total, longestRunning == null ? null
										: longestRunning.getStatus()));
			} else {
				jobs.clear();
				getLauncher().getUserInterface().hideDownloadProgress();
			}
		}
	}

	public boolean hasRemainingJobs() {
		synchronized (jobs) {
			for (DownloadJob job : jobs) {
				if (!job.isComplete()) {
					return true;
				}
			}
		}
		return false;
	}

	public void launchSelectedProfile() {
		setFinished(false);
		// finished status will be set by ModdedUserInterface
		this.getLaunchDispatcher().play();
		
	}
	
	
	void updateJobState() {
		synchronized (jobs) {
			if (jobs.isEmpty()) setFinished(true);
		}
	}
	
	void setError(boolean error) {
		this.error = this.error || error;
	}
	
	void setFinished(boolean b) {
		synchronized (jobs) {
			finished = b;
			jobs.notifyAll();
		}
	}

	public void waitForFinish() throws InterruptedException {
		synchronized (jobs) {
			while (!finished) jobs.wait();
			getLauncher().getDownloaderExecutorService().shutdown();
			getLauncher().getVersionManager().getExecutorService().shutdown();
		}
	}

	
	public boolean getResult() throws InterruptedException {
		waitForFinish();
		return !error && failed.isEmpty();
	}

	protected void downloadRequiredFiles(VersionSyncInfo syncInfo,
			CompleteVersion version, DownloadListener listener) {
		try {
			DownloadJob librariesJob = new DownloadJob("Version & Libraries",
					false, listener);
			addJob(librariesJob);
			getLauncher().getVersionManager().downloadVersion(syncInfo,
					librariesJob);
			librariesJob.startDownloading(getLauncher()
					.getDownloaderExecutorService());

			DownloadJob resourceJob = new DownloadJob("Resources", true,
					listener);
			addJob(resourceJob);
			getLauncher().getVersionManager().downloadResources(resourceJob,
					version);
			resourceJob.startDownloading(getLauncher()
					.getDownloaderExecutorService());
		} catch (IOException e) {
			LOGGER.error(
					"Couldn't get version info for "
							+ syncInfo.getLatestVersion(), e);
			error = true;
			clearJobs();
		}
	}
	private void clearJobs() {
		synchronized (jobs) {
			jobs.clear();
			finished = true;
		}
	}

	private List<DownloadJob> jobs;
	private void addJob(DownloadJob resourceJob) {
		synchronized (jobs) {
			jobs.add(resourceJob);
		}
	}

	private void setProfileManager(ProfileManager profileManager)
			throws IllegalArgumentException, IllegalAccessException,
			NoSuchFieldException {
		setSuperClassMember("profileManager", profileManager);
	}

	private void setLauncher(com.mojang.launcher.Launcher launcher)
			throws IllegalArgumentException, IllegalAccessException,
			NoSuchFieldException {
		setSuperClassMember("launcher", launcher);
	}

	private void setLaunchDispather(
			net.minecraft.launcher.game.GameLaunchDispatcher gameLaunchDispatcher)
			throws IllegalArgumentException, IllegalAccessException,
			NoSuchFieldException {
		setSuperClassMember("launchDispatcher", gameLaunchDispatcher);
	}

	private void setBootstrapVersion(Integer bootstrapVersion)
			throws IllegalArgumentException, IllegalAccessException,
			NoSuchFieldException {
		setSuperClassMember("bootstrapVersion", bootstrapVersion);
	}

	private void setRequestedUser(String requestedUser)
			throws IllegalArgumentException, IllegalAccessException,
			NoSuchFieldException {
		setSuperClassMember("requestedUser", requestedUser);
	}

	private String getRequestedUser() throws NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {
		return (String) getSuperClassMember("requestedUser");
	}

	private Object getSuperClassMember(String memberName)
			throws NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException {
		try {
			Class<?> clazz = ModdedLauncher.class;
			Class<?> superClazz = (Class<?>) clazz.getGenericSuperclass();
			Field member = ReflectionHelper.getDeclaredField(superClazz,
					memberName);
			boolean wasAccessible = member.isAccessible();
			member.setAccessible(true);
			Object value = member.get(this);
			member.setAccessible(wasAccessible);
			return value;
		} catch (Throwable t) {
			System.out.println(memberName);
			error = true;
			throw t;
		}
	}

	private void setSuperClassMember(String memberName, Object value)
			throws IllegalArgumentException, IllegalAccessException,
			NoSuchFieldException {
		try {
			Class<?> clazz = ModdedLauncher.class;
			Class<?> superClazz = (Class<?>) clazz.getGenericSuperclass();
			Field member = ReflectionHelper.getDeclaredField(superClazz,
					memberName);
			boolean wasAccessible = member.isAccessible();
			member.setAccessible(true);
			member.set(this, value);
			member.setAccessible(wasAccessible);
		} catch (Throwable t) {
			System.out.println(memberName);
			error = true;
			throw t;
		}
	}

	private void setupErrorHandling() {
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, Throwable e) {
				ModdedLauncher.LOGGER.fatal(
						"Unhandled exception in thread " + t, e);
				ModdedLauncher.this.error = true;
			}
		});
	}

	private String[] processArgs(String[] args)
			throws IllegalArgumentException, IllegalAccessException,
			NoSuchFieldException {
		OptionParser optionParser = new OptionParser();
		optionParser.allowsUnrecognizedOptions();

		OptionSpec<String> userOption = optionParser.accepts("user")
				.withRequiredArg().ofType(String.class);
		OptionSpec<String> nonOptions = optionParser.nonOptions();
		OptionSet optionSet;
		try {
			optionSet = optionParser.parse(args);
		} catch (OptionException e) {
			return args;
		}

		if (optionSet.has(userOption)) {
			setRequestedUser(((String) optionSet.valueOf(userOption)));
		}

		List<String> remainingOptions = optionSet.valuesOf(nonOptions);
		return (String[]) remainingOptions.toArray(new String[remainingOptions
				.size()]);
	}

	public void refreshVersionsAndProfiles() {
		getLauncher().getVersionManager().getExecutorService()
				.submit(new Runnable() {
					public void run() {
						refreshVersionsAndProfilesSync();
					}

				});
	}

	protected void refreshVersionsAndProfilesSync() {
		try {
			try {
				getLauncher().getVersionManager()
						.refreshVersions();
			} catch (Throwable e) {
				ModdedLauncher.LOGGER
						.error("Unexpected exception refreshing version list",
								e);
			}
			try {
				getProfileManager().loadProfiles();
				ModdedLauncher.LOGGER.info("Loaded "
						+ getProfileManager().getProfiles()
								.size()
						+ " profile(s); selected '"
						+ getProfileManager()
								.getSelectedProfile().getName()
						+ "'");
			} catch (Throwable e) {
				ModdedLauncher.LOGGER
						.error("Unexpected exception refreshing profile list",
								e);
			}

			if (getRequestedUser() != null) {
				AuthenticationDatabase authDatabase = getProfileManager()
						.getAuthDatabase();
				boolean loggedIn = false;
				try {
					String uuid = UUIDTypeAdapter.fromUUID(UUIDTypeAdapter
							.fromString(getRequestedUser()));
					UserAuthentication auth = authDatabase
							.getByUUID(uuid);

					if (auth != null) {
						getProfileManager().setSelectedUser(
								uuid);
						loggedIn = true;
					}
				} catch (RuntimeException localRuntimeException) {
				}
				if ((!loggedIn)
						&& (authDatabase
								.getByName(getRequestedUser()) != null)) {
					UserAuthentication auth = authDatabase
							.getByName(getRequestedUser());
					if (auth.getSelectedProfile() != null) {
						getProfileManager().setSelectedUser(
								UUIDTypeAdapter.fromUUID(auth
										.getSelectedProfile()
										.getId()));
					} else {
						getProfileManager().setSelectedUser(
								"demo-" + auth.getUserID());
					}
				}
			}

			ensureLoggedIn();
		} catch (Throwable t) {
			error = true;
			throw new RuntimeException(t);
		}
	}

	public void ensureLoggedIn() {
		UserAuthentication auth = getProfileManager().getAuthDatabase()
				.getByUUID(getProfileManager().getSelectedUser());

		if (auth == null) {
			getUserInterface().showLoginPrompt();
		} else if (!auth.isLoggedIn()) {
			if (auth.canLogIn()) {
				try {
					auth.logIn();
					try {
						getProfileManager().saveProfiles();
					} catch (IOException e) {
						LOGGER.error(
								"Couldn't save profiles after refreshing auth!",
								e);
					}
					getProfileManager().fireRefreshEvent();
				} catch (AuthenticationException e) {
					LOGGER.error("Exception whilst logging into profile", e);
					getUserInterface().showLoginPrompt();
				}
			} else {
				getUserInterface().showLoginPrompt();
			}
		} else if (!auth.canPlayOnline()) {
			try {
				LOGGER.info("Refreshing auth...");
				auth.logIn();
				try {
					getProfileManager().saveProfiles();
				} catch (IOException e) {
					LOGGER.error(
							"Couldn't save profiles after refreshing auth!", e);
				}
				getProfileManager().fireRefreshEvent();
			} catch (InvalidCredentialsException e) {
				LOGGER.error("Exception whilst logging into profile", e);
				getUserInterface().showLoginPrompt();
			} catch (AuthenticationException e) {
				LOGGER.error("Exception whilst logging into profile", e);
			}
		}
		
	}

	
	public boolean isLoggedIn() {
		// homac: check whether we are logged in now
		UserAuthentication auth = getProfileManager().getAuthDatabase()
				.getByUUID(getProfileManager().getSelectedUser());
		
		return auth != null && auth.isLoggedIn();
	}
	
	
	
	public void cleanupOrphanedAssets() throws IOException {
		File assetsDir = new File(getLauncher().getWorkingDirectory(), "assets");
		File indexDir = new File(assetsDir, "indexes");
		File objectsDir = new File(assetsDir, "objects");
		Set<String> referencedObjects = Sets.newHashSet();

		if (!objectsDir.isDirectory()) {
			return;
		}

		for (VersionSyncInfo syncInfo : getLauncher().getVersionManager()
				.getInstalledVersions()) {
			if ((syncInfo.getLocalVersion() instanceof CompleteMinecraftVersion)) {
				CompleteMinecraftVersion version = (CompleteMinecraftVersion) syncInfo
						.getLocalVersion();
				String assetVersion = version.getAssetIndex().getId();
				File indexFile = new File(indexDir, assetVersion + ".json");
				AssetIndex index = (AssetIndex) gson.fromJson(FileUtils
						.readFileToString(indexFile,
								org.apache.commons.io.Charsets.UTF_8),
						AssetIndex.class);
				for (AssetIndex.AssetObject object : index.getUniqueObjects()
						.keySet()) {
					referencedObjects.add(object.getHash().toLowerCase());
				}
			}
		}

		File[] directories = objectsDir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return DirectoryFileFilter.INSTANCE.accept(pathname);
			}
		});
		if (directories != null) {
			for (File directory : directories) {
				File[] files = directory.listFiles(new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						return FileFileFilter.FILE.accept(pathname);
					}
				});
				if (files != null) {
					for (File file : files) {
						if (!referencedObjects.contains(file.getName()
								.toLowerCase())) {
							LOGGER.info("Cleaning up orphaned object {}",
									new Object[] { file.getName() });
							FileUtils.deleteQuietly(file);
						}
					}
				}
			}
		}

		deleteEmptyDirectories(objectsDir);
	}

	public void cleanupOrphanedLibraries() throws IOException {
		File librariesDir = new File(getLauncher().getWorkingDirectory(),
				"libraries");
		Set<File> referencedLibraries = Sets.newHashSet();

		if (!librariesDir.isDirectory()) {
			return;
		}

		for (VersionSyncInfo syncInfo : getLauncher().getVersionManager()
				.getInstalledVersions()) {
			if ((syncInfo.getLocalVersion() instanceof CompleteMinecraftVersion)) {
				CompleteMinecraftVersion version = (CompleteMinecraftVersion) syncInfo
						.getLocalVersion();
				for (Library library : version.getRelevantLibraries(version.createFeatureMatcher())) {
					String file = null;

					if (library.getNatives() != null) {
						String natives = (String) library.getNatives().get(
								OperatingSystem.getCurrentPlatform());
						if (natives != null) {
							file = library.getArtifactPath(natives);
						}
					} else {
						file = library.getArtifactPath();
					}

					if (file != null) {
						referencedLibraries.add(new File(librariesDir, file));
						referencedLibraries.add(new File(librariesDir, file
								+ ".sha"));
					}
				}
			}
		}

		Collection<File> libraries = FileUtils.listFiles(librariesDir,
				TrueFileFilter.TRUE, TrueFileFilter.TRUE);
		if (libraries != null) {
			for (File file : libraries) {
				if (!referencedLibraries.contains(file)) {
					LOGGER.info("Cleaning up orphaned library {}",
							new Object[] { file });
					FileUtils.deleteQuietly(file);
				}
			}
		}

		deleteEmptyDirectories(librariesDir);
	}

	public void cleanupOldSkins() {
		File assetsDir = new File(getLauncher().getWorkingDirectory(), "assets");
		File skinsDir = new File(assetsDir, "skins");

		if (!skinsDir.isDirectory()) {
			return;
		}

		Collection<File> files = FileUtils.listFiles(skinsDir,
				new AgeFileFilter(System.currentTimeMillis() - 604800000L),
				TrueFileFilter.TRUE);
		if (files != null) {
			for (File file : files) {
				LOGGER.info("Cleaning up old skin {}",
						new Object[] { file.getName() });
				FileUtils.deleteQuietly(file);
			}
		}

		deleteEmptyDirectories(skinsDir);
	}

	public void cleanupOldVirtuals() throws IOException {
		File assetsDir = new File(getLauncher().getWorkingDirectory(), "assets");
		File virtualsDir = new File(assetsDir, "virtual");
		DateTypeAdapter dateAdapter = new DateTypeAdapter();
		Calendar calendar = Calendar.getInstance();
		calendar.add(5, -5);
		Date cutoff = calendar.getTime();

		if (!virtualsDir.isDirectory()) {
			return;
		}

		File[] directories = virtualsDir.listFiles(DIRECTORIES);
		if (directories != null) {
			for (File directory : directories) {
				File lastUsedFile = new File(directory, ".lastused");

				if (lastUsedFile.isFile()) {
					Date lastUsed = dateAdapter.deserializeToDate(FileUtils
							.readFileToString(lastUsedFile));
					if (cutoff.after(lastUsed)) {
						LOGGER.info("Cleaning up old virtual directory {}",
								new Object[] { directory });
						FileUtils.deleteQuietly(directory);
					}
				} else {
					LOGGER.info("Cleaning up strange virtual directory {}",
							new Object[] { directory });
					FileUtils.deleteQuietly(directory);
				}
			}
		}

		deleteEmptyDirectories(virtualsDir);
	}

	public void cleanupOldNatives() {
		File root = new File(getLauncher().getWorkingDirectory(), "versions/");
		LOGGER.info("Looking for old natives & assets to clean up...");
		IOFileFilter ageFilter = new AgeFileFilter(
				System.currentTimeMillis() - 3600000L);

		if (!root.isDirectory()) {
			return;
		}

		File[] versions = root.listFiles(DIRECTORIES);
		if (versions != null) {
			for (File version : versions) {
				File[] files = version
						.listFiles(wrap(FileFilterUtils
								.and(new IOFileFilter[] {
										new org.apache.commons.io.filefilter.PrefixFileFilter(
												version.getName() + "-natives-"),
										ageFilter })));
				if (files != null) {
					for (File folder : files) {
						LOGGER.debug("Deleting " + folder);

						FileUtils.deleteQuietly(folder);
					}
				}
			}
		}
	}

	private FileFilter wrap(final IOFileFilter filter) {
		return new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return filter.accept(pathname);
			}
		};
	}

	public void cleanupOrphanedVersions() {
		LOGGER.info("Looking for orphaned versions to clean up...");
		Set<String> referencedVersions = Sets.newHashSet();

		for (Profile profile : getProfileManager().getProfiles().values()) {
			String lastVersionId = profile.getLastVersionId();
			VersionSyncInfo syncInfo = null;

			if (lastVersionId != null) {
				syncInfo = getLauncher().getVersionManager()
						.getVersionSyncInfo(lastVersionId);
			}

			if ((syncInfo == null) || (syncInfo.getLatestVersion() == null)) {
				syncInfo = (VersionSyncInfo) getLauncher().getVersionManager()
						.getVersions(profile.getVersionFilter()).get(0);
			}

			if (syncInfo != null) {
				Version version = syncInfo.getLatestVersion();
				referencedVersions.add(version.getId());

				if ((version instanceof CompleteMinecraftVersion)) {
					CompleteMinecraftVersion completeMinecraftVersion = (CompleteMinecraftVersion) version;
					referencedVersions.add(completeMinecraftVersion
							.getInheritsFrom());
					referencedVersions.add(completeMinecraftVersion.getJar());
				}
			}
		}

		Calendar calendar = Calendar.getInstance();
		calendar.add(5, -7);
		Date cutoff = calendar.getTime();

		for (VersionSyncInfo versionSyncInfo : getLauncher()
				.getVersionManager().getInstalledVersions()) {
			if ((versionSyncInfo.getLocalVersion() instanceof CompleteMinecraftVersion)) {
				CompleteVersion version = (CompleteVersion) versionSyncInfo
						.getLocalVersion();

				if ((!referencedVersions.contains(version.getId()))
						&& (version.getType() == net.minecraft.launcher.game.MinecraftReleaseType.SNAPSHOT)) {
					if (versionSyncInfo.isOnRemote()) {
						LOGGER.info(
								"Deleting orphaned version {} because it's a snapshot available on remote",
								new Object[] { version.getId() });
						try {
							getLauncher().getVersionManager().uninstallVersion(
									version);
						} catch (IOException e) {
							LOGGER.warn(
									"Couldn't uninstall version "
											+ version.getId(), e);
						}
					} else if (version.getUpdatedTime().before(cutoff)) {
						LOGGER.info(
								"Deleting orphaned version {} because it's an unsupported old snapshot",
								new Object[] { version.getId() });
						try {
							getLauncher().getVersionManager().uninstallVersion(
									version);
						} catch (IOException e) {
							LOGGER.warn(
									"Couldn't uninstall version "
											+ version.getId(), e);
						}
					}
				}
			}
		}
	}

	private static Collection<File> listEmptyDirectories(File directory) {
		List<File> result = com.google.common.collect.Lists.newArrayList();
		File[] files = directory.listFiles();

		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					File[] subFiles = file.listFiles();

					if ((subFiles == null) || (subFiles.length == 0)) {
						result.add(file);
					} else {
						result.addAll(listEmptyDirectories(file));
					}
				}
			}
		}

		return result;
	}

	private static void deleteEmptyDirectories(File directory) {
		for (;;) {
			Collection<File> files = listEmptyDirectories(directory);
			if (files.isEmpty()) {
				return;
			}

			for (File file : files) {
				if (FileUtils.deleteQuietly(file)) {
					LOGGER.info("Deleted empty directory {}",
							new Object[] { file });
				} else {
					return;
				}
			}
		}
	}

	public void performCleanups() throws IOException {
		cleanupOrphanedVersions();
		cleanupOrphanedAssets();
		cleanupOldSkins();
		cleanupOldNatives();
		cleanupOldVirtuals();
	}

}
