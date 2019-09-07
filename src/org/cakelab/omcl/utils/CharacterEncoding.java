package org.cakelab.omcl.utils;

public class CharacterEncoding {
	public static boolean isPureAscii(String s) {
		for (char c : s.toCharArray()) {
			if (c > 127) return false;
		}
		return true;
	}
}
