package me.bomb.amusic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Data {
	protected final Map<String, DataEntry> options = new HashMap<String, DataEntry>();
	
	public Data() {
	}

	public abstract void save();

	public abstract void load();

	private static byte[] calcSHA1(File file) {
		try {
			FileInputStream fileInputStream = new FileInputStream(file);
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			DigestInputStream digestInputStream = new DigestInputStream(fileInputStream, digest);
			byte[] bytes = new byte[1024];
			while (digestInputStream.read(bytes) > 0);
			digestInputStream.close();
			return digest.digest();
		} catch (IOException | NoSuchAlgorithmException e) {
		}
		return null;
	}

	public final byte[] setPlaylist(String playlistname, List<String> sounds, List<Short> length, File file) {
		byte[] sha1 = calcSHA1(file);
		options.put(playlistname, new DataEntry((int) file.length(), file.getName(), sounds, length, sha1));
		return sha1;
	}

	public final DataEntry getPlaylist(String playlistname) {
		return options.get(playlistname);
	}

	public final boolean containsPlaylist(String playlistname) {
		return options.containsKey(playlistname);
	}

	public final void removePlaylist(String playlistname) {
		options.remove(playlistname);
	}

	public final Set<String> getPlaylists() {
		return options == null ? null : options.keySet();
	}
}
