package me.bomb.amusic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public final class DataEntry {
	
	public final int size;
	public final String name;
	public final List<String> sounds;
	public final List<Short> length;
	public final byte[] sha1;
	private boolean saved;

	public DataEntry(int size, String name, List<String> sounds, List<Short> length, byte[] sha1) throws IllegalArgumentException {
		if (size < 0 || name == null || sounds == null || length == null || sha1 == null || sounds.size() != length.size() || sha1.length != 20) {
			throw new IllegalArgumentException();
		}
		this.size = size;
		this.name = name;
		this.sounds = sounds;
		this.length = length;
		this.sha1 = sha1;
	}

	public boolean check(File file) {
		if (file.length() != size) {
			return false;
		}
		byte[] filesha1 = null;
		try {
			MessageDigest sha1hash;
			try {
				sha1hash = MessageDigest.getInstance("SHA-1");
			} catch (NoSuchAlgorithmException e) {
				return false;
			}
			DigestInputStream digestInputStream = new DigestInputStream(new FileInputStream(file), sha1hash);
			byte[] bytes = new byte[1024];
			while (digestInputStream.read(bytes) > 0);
			digestInputStream.close();
			filesha1 = sha1hash.digest();
		} catch (IOException e) {
		}	
		for(byte i = 20;--i > -1;) {
			if (sha1[i] != filesha1[i]) {
				return false;
			}
		}
		return true;
	}
	
	public void setSaved() {
		saved = true;
	}
	
	public boolean isSaved() {
		return saved;
	}
	
}