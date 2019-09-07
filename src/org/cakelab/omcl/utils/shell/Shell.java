package org.cakelab.omcl.utils.shell;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;




public class Shell {
	
	private static class Local {
		Environment env;
		String HOME;
	}
	
	private static ThreadLocal<Local> local = new ThreadLocal<Local>(){
		@Override
		protected Local initialValue() {
			return global;
		}
	}; 
	
	
	
	private static Local global;
	private static final String VAR_EXPR_START = "${";
	private static final String VAR_EXPR_END = "}";

	
	static {
		/* needs no synchronisation because it is executed exactly once
		 * and guaranteed to be finished before anyone can access members 
		 * of it. */
		global = new Local();
		setEnvironment(new Environment(System.getenv()));
	}
	
	
	public static void setEnvironment(Environment env) {
		Local localEnv = local.get();
		localEnv.env = new Environment(env);
		localEnv.HOME = localEnv.env.get("HOME");
	}
	
	public static Environment getEnvironment() {
		return new Environment(local.get().env);
	}


	public static void addEnvironment(Environment env) {
		local.get().env.putAll(env);
	}
	
	
	public static File resolveFileName(String filename) {
		if (local.get().env != null) {
			filename = resolveVariables(filename);
		}
		if (filename.startsWith("~")) {
			if (filename.length() == 1) {
				filename = local.get().HOME;
			} else {
				filename = local.get().HOME + filename.substring(1);
			}
		}
		return new File(filename);
	}

	public static String resolveVariables(String expression) {
		HashMap<String, Integer> resolving = new HashMap<String, Integer>();
		return resolveVariables(expression, 0, resolving);
	}
	
	private static String resolveVariables(String expression, int level, HashMap<String, Integer> resolving) {
		
		while (expression.contains(VAR_EXPR_START)) {
			StringBuffer result = new StringBuffer("");
			int end = 0;
			int start = expression.indexOf(VAR_EXPR_START, end);
			end--;
			while (start >= 0) {
				result.append(expression.substring(end+1, start));
				
				end = expression.indexOf(VAR_EXPR_END, start);
				if (end < 0) throw new Error("missing '" + VAR_EXPR_END + "' in variable expression '" + expression + '"');
				String name = expression.substring(start+2, end);
				String value = resolveVariable(name, level, resolving);
				
				result.append(value);

				start = expression.indexOf(VAR_EXPR_START, end);
			}
			result.append(expression.substring(end+1, expression.length()));
			
			expression = result.toString();
		}
		return expression;
	}


	private static String resolveVariable(String name, int level,
			HashMap<String, Integer> resolving) {
		
		if (resolving.containsKey(name)) {
			throw new IllegalArgumentException("cyclic dependecies involving variable '" + name + "'");
		}
		
		String value = local.get().env.get(name);
		if (value == null) {
			throw new IllegalArgumentException("variable '" + name + "' undefined");
		}

		if (value.contains(VAR_EXPR_START)) {
			resolving.put(name, level);
			value = resolveVariables(value, level++, resolving);
			local.get().env.put(name, value);
			resolving.remove(name);
		}
		return value;
	}

	public static FileFilter createRelativeFileFilter(String parent,
			String[] filters, final boolean exclude) throws IOException {
		if (filters == null) return new FileFilter(){
			@Override
			public boolean accept(File pathname) {
				return exclude;
			}
		};
		for (int i = 0; i < filters.length; i++) {
			String filter = parent + File.separator + filters[i];
			filter = filter.replaceAll("\\/\\.$", "");
			filter = filter.replace("/./", "/");
			while (filter.contains("..")) {
				filter = filter.replaceAll("/[^\\/]*[^\\/\\.]/\\.\\.","");
			}
			filters[i] = filter;
		}
		return createFileFilter(filters, exclude);
	}

	public static FileFilter createFileFilter(String[] filters, boolean exclude) {
		return new ShellFileFilter(filters, exclude);
	}


}
