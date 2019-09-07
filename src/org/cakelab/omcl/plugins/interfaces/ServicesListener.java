package org.cakelab.omcl.plugins.interfaces;

import javax.swing.JFrame;

public interface ServicesListener extends LogListener {

	JFrame getFrame();

	void updateProgress(long total, long current, float percent, String status);

	void endProgress();

	void showInfo(String message, String details);

	void showWarning(String message, String details);

	void showError(String message, String details);

	void logGameOutput(String msg);

	
}
