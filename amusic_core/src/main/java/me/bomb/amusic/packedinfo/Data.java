package me.bomb.amusic.packedinfo;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import me.bomb.amusic.resource.ResourcePacker;
import me.bomb.amusic.source.PackSource;
import me.bomb.amusic.source.SoundSource;

public abstract class Data {
	
	protected final Map<String,DataEntry> options = new HashMap<>();
	public final boolean lockwrite;
	
	protected Data(boolean lockwrite) {
		this.lockwrite = lockwrite;
	}

	/**
	 * Get default data storage
	 * @param id
	 * @return default data storage
	 */
	public static Data getDefault(Path datadirectory, boolean lockwrite) {
		return new DataStorage(datadirectory, lockwrite);
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
	 * @param source
	 * @return ResourcePacker if write allowed
	 */
	public abstract ResourcePacker createPacker(final String id, final SoundSource source,  final PackSource packsource);
	
	/**
	 * Update packed info
	 * @param id
	 * @param packer
	 * @return true if something changed
	 */
	public abstract boolean update(final String id, final ResourcePacker packer);
	

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
