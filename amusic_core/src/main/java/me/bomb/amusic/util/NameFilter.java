package me.bomb.amusic.util;

import static java.io.File.separatorChar;

public final class NameFilter {
	
	private NameFilter() {
	}
	
	public static String filterName(String name) {
		char[] chars = name.toCharArray();
		int finalcount = 0;
		int i = chars.length;
		while(--i > -1) {
			char c = chars[i];
			//if(c == separatorChar || c == '/' || c == '\\' || c == ':' || c == '<' || c == '>' || c == '*' || c == '?' || c == '|' || c == '\"' || (c > 0 && c < 32)) {
			if(c == separatorChar || c == '/' || c == '\\') {
				chars[i] = '\0';
			} else {
				++finalcount;
			}
		}
		char[] filtered = new char[finalcount];
		int j = 0;
		while(++i < chars.length && j < finalcount) {
			char c = chars[i];
			if(c != '\0') {
				filtered[j] = c;
				++j;
			}
		}
		return new String(filtered);
	}

}
