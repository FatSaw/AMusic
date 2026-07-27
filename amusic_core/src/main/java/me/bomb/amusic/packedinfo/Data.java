package me.bomb.amusic.packedinfo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class Data {
	
	protected final Map<String,DataEntry> options = new HashMap<>();
	public final boolean lockwrite;
	
	
	protected Data(boolean lockwrite) {
		this.lockwrite = lockwrite;
	}
	
	/**
	 * Get no storage
	 * @param id
	 * @return no storage
	 */
	public static NoStorage getNoStorage(boolean lockwrite, LocalConvertedZerocopySource source) {
		return new NoStorage(lockwrite, source);
	}
	
	/**
	 * Save {@link Data#options} to storage.
	 */
	protected abstract void save();

	/**
	 * Load {@link Data#options} from storage.
	 */
	public abstract void load();
	
	public abstract void start();
	
	public abstract void end();
	
	/**
	 * Update packed info
	 * @param id
	 * @return not null if write allowed
	 */
	public abstract UpdateResult update(final String id);
	

	public final DataEntry getPlaylist(String playlistname) {
		return options.get(playlistname);
	}

	public final boolean containsPlaylist(String playlistname) {
		return options.containsKey(playlistname);
	}

	public final String[] getPlaylists() {
		int i = options.size();
		String[] playlists = new String[i];
		Iterator<String> iterator = options.keySet().iterator();
		while(iterator.hasNext() && --i > -1) {
			playlists[i] = iterator.next();
		}
		return playlists;
	}
	
}
