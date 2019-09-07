package org.cakelab.omcl.plugins.minecraft.launcher;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.minecraft.launcher.LauncherConstants;
import net.minecraft.launcher.MinecraftUserInterface;
import net.minecraft.launcher.game.MinecraftGameRunner;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;
import net.minecraft.launcher.ui.popups.login.LogInPopup;

import org.apache.logging.log4j.Logger;
import org.cakelab.omcl.plugins.interfaces.ServicesListener;

import com.google.common.util.concurrent.SettableFuture;
import com.mojang.authlib.UserAuthentication;
import com.mojang.launcher.events.GameOutputLogProcessor;
import com.mojang.launcher.game.GameInstanceStatus;
import com.mojang.launcher.game.process.GameProcess;
import com.mojang.launcher.updater.DownloadProgress;
import com.mojang.launcher.versions.CompleteVersion;

public class ModdedUserInterface implements
		MinecraftUserInterface {

	private JFrame frame;
	private ModdedLauncher minecraftLauncher;
	private static final Logger LOGGER = org.apache.logging.log4j.LogManager
			.getLogger();
	private ServicesListener listener;

	public ModdedUserInterface(ModdedLauncher minecraftLauncher, ServicesListener listener) {
		this.minecraftLauncher = minecraftLauncher;
		this.listener = listener;
		this.frame = listener.getFrame();
	}

	/**
	 * Called whenever the login status is invalid (either expired or failed).
	 * Installation can proceed without having a valid login.
	 * Game cannot start without a valid login.
	 */
	public void showLoginPrompt() {
		final ProfileManager profileManager = minecraftLauncher
				.getProfileManager();
		try {
			profileManager.saveProfiles();
		} catch (IOException e) {
			LOGGER.error("Couldn't save profiles before logging in!", e);
		}

		final Profile selectedProfile = profileManager.getSelectedProfile();
		
		
		
		final JDialog dialog = new JDialog(frame);
		LogInPopup popup = new LogInPopup(minecraftLauncher, new LogInPopup.Callback() {
			public void onLogIn(String uuid) {
				UserAuthentication auth = profileManager.getAuthDatabase()
						.getByUUID(uuid);
				profileManager.setSelectedUser(uuid);

				if ((selectedProfile.getName().equals("(Default)"))
						&& (auth.getSelectedProfile() != null)) {
					String playerName = auth.getSelectedProfile().getName();
					String profileName = auth.getSelectedProfile().getName();
					int count = 1;

					while (profileManager.getProfiles()
							.containsKey(profileName)) {
						profileName = playerName + " " + ++count;
					}

					Profile newProfile = new Profile(selectedProfile);
					newProfile.setName(profileName);
					profileManager.getProfiles().put(profileName, newProfile);
					profileManager.getProfiles().remove("(Default)");
					profileManager.setSelectedProfile(profileName);
				}
				try {
					profileManager.saveProfiles();
				} catch (IOException e) {
					LOGGER.error("Couldn't save profiles after logging in!", e);
				}

				if (uuid == null) {
					minecraftLauncher.getLauncher().shutdownLauncher();
				} else {
					profileManager.fireRefreshEvent();
				}

				dialog.setVisible(false);
			}
		});
		
		

		dialog.add(popup);
		dialog.pack();
		
		// center window
		Dimension dim = dialog.getToolkit().getScreenSize();
		Rectangle abounds = dialog.getBounds();
		dialog.setLocation((dim.width - abounds.width) / 2, (dim.height - abounds.height) / 2);
		
		dialog.setAlwaysOnTop(true);
		dialog.setAutoRequestFocus(true);
		dialog.setModal(true);
		dialog.setVisible(true);

		
	}

	/** Called in three cases
	 * <ul>
	 * <li>Game has exited.</li>
	 * <li>Login failed</li>
	 * <li>Launcher is outdated</li>
	 * <ul>
	 */
	@Override
	public void shutdownLauncher() {
		// dispose any windows
		this.minecraftLauncher.updateJobState();
		listener.info("launcher shut down", null);
		System.exit(0);
	}

	@Override
	public void hideDownloadProgress() {
		// This basically tells the UI whether a download is in progress or not.
		listener.endProgress();
	}

	@Override
	public void setDownloadProgress(DownloadProgress downloadProgress) {
		// If this function is called, a download has either been started or its
		// progress has been updated.
		listener.updateProgress(downloadProgress.getTotal(), downloadProgress.getCurrent(), downloadProgress.getPercent(), downloadProgress.getStatus());
	}

	/**
	 * This method is called if the game crashed only.
	 * 
	 * The game status will be set to idle afterwards.
	 */
	@Override
	public void showCrashReport(final CompleteVersion version, File crashReportFile,
			final String crashReport) {
		// report crash and game status update
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run() {
				listener.showError("Minecraft client " + version.getId() + " crashed.", crashReport);
			}
		});
		minecraftLauncher.setError(true);
		minecraftLauncher.setFinished(true);
		minecraftLauncher.updateJobState();
		
	}

	/**
	 * This is called when the game did not even start.
	 * 
	 * Games state will be set to idle afterwards.
	 */
	@Override
	public void gameLaunchFailure(final String reason) {
		// report launcher failure and game status update
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run() {
				listener.showError("Minecraft client failed to launch.", reason);
			}
		});
		listener.error("Minecraft client failed to launch. Reason: " + reason, null);
		minecraftLauncher.updateJobState();
	}

	@Override
	public void updatePlayState() {
		// check here, whether the client is actually starting 
		// Is called during preparation of the client version to be launched.
		GameInstanceStatus instanceStatus = minecraftLauncher.getLaunchDispatcher().getInstanceStatus();
		if (instanceStatus == GameInstanceStatus.IDLE) {
			this.minecraftLauncher.updateJobState();
		}
	}

	
	/**
	 * This is called if the minecraft bootstrap is outdated.
	 * We can't fix it here.
	 */
	@Override
	public void showOutdatedNotice() {
		/* 
		 * Since we are the bootstrap this method will not be called unless 
		 * we have started the plugin with the wrong parameter for the bootstrap version.
		 * See ModdedLauncher.init()
		 */
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run() {
				listener.showError("Life in the Woods Launcher Error.", "Unfortunately there is an internal error. \nPlease try restarting to receive a new update.");
			}
		});
		listener.fatal("Wrong minecraft bootstrap version.", null);
		minecraftLauncher.updateJobState();
	}

	@Override
	public String getTitle() {
		if (frame != null) return frame.getTitle();
		else return "";
	}

	@Override
	public GameOutputLogProcessor showGameOutputTab(
			MinecraftGameRunner gameRunner) {

		// The future is used for synchronization between gamerunner and gui.
		// the game runner might wait on the future to be set.
		
		final SettableFuture<GameOutputLogProcessor> future = SettableFuture.create();

		
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				future.set(new GameOutputLogProcessor() {

					@Override
					public void onGameOutput(GameProcess process,
							String msg) {
						if (!process.isRunning()) {
							listener.logGameOutput("game exited with " + process.getExitCode());
						} else {
							listener.logGameOutput(msg);
						}
					}
					
				});
			}
			
		});
		

		GameOutputLogProcessor logProcessor = (GameOutputLogProcessor)com.google.common.util.concurrent.Futures.getUnchecked(future);
		return logProcessor;
	}

	@Override
	public void setVisible(boolean visible) {
		// we decide elsewhere  whether the frame should stay visible or not
		if (frame != null) frame.setVisible(visible);
	}
	public boolean shouldDowngradeProfiles() {
		int result = JOptionPane.showOptionDialog(frame, "It looks like you've used a newer launcher than this one! If you go back to using this one, we will need to reset your configuration.", "Outdated launcher", 0, 0, null, LauncherConstants.LAUNCHER_OUT_OF_DATE_BUTTONS, LauncherConstants.LAUNCHER_OUT_OF_DATE_BUTTONS[0]);
	    return result == 1;
	}

}
