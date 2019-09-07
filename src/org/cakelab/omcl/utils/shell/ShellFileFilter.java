package org.cakelab.omcl.utils.shell;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;


public class ShellFileFilter implements FileFilter {

	private PathMatcher[] matcher;
	private boolean exclude;

	public ShellFileFilter(String[] filters, boolean exclude) {
		
		this.exclude = exclude;
		this.matcher = new PathMatcher[filters.length];
		
		for (int i = 0; i < filters.length; i++) {
			matcher[i] = FileSystems.getDefault().getPathMatcher("glob:" + filters[i]);
		}
	}

	@Override
	public boolean accept(File f) {
		for (PathMatcher matcher : matcher) {
			if (matcher.matches(f.toPath())) {
				return !exclude;
			}
		}
		return exclude;
	}

	
}
