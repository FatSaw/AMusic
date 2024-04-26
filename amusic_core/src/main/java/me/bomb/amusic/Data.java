package me.bomb.amusic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Data {
	protected final Map<String, DataEntry> options = new HashMap<String, DataEntry>();
	
	protected Data() {
	}
	/**
	 * Save {@link Data#options} to storage.
	 */
	protected abstract void save();

	/**
	 * Load {@link Data#options} from storage.
	 */
	protected abstract void load();
	
	public final void setPlaylist(String playlistname, List<String> sounds, List<Short> lengths, int resourcesize, String resourcename, byte[] sha1) {
		try {
			options.put(playlistname, new DataEntry(resourcesize, resourcename, sounds, lengths, sha1));
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
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
