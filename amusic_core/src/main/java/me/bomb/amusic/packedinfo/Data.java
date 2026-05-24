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
	public final SoundSource soundsource;
	public final PackSource packsource;
	public final boolean lockwrite, storeinram;
	
	
	protected Data(SoundSource soundsource, PackSource packsource, boolean lockwrite, boolean storeinram) {
		this.soundsource = soundsource;
		this.packsource = packsource;
		this.lockwrite = lockwrite;
		this.storeinram = storeinram;
	}
	
	/**
	 * Get no storage
	 * @param id
	 * @return no storage
	 */
	public static Data getNoStorage(SoundSource soundsource, PackSource packsource, boolean lockwrite, boolean storeinram) {
		return new NoStorage(soundsource, packsource, lockwrite, storeinram);
	}

	/**
	 * Get default data storage
	 * @param id
	 * @return default data storage
	 */
	public static Data getDefault(SoundSource soundsource, PackSource packsource, boolean lockwrite, boolean storeinram, Path datadirectory) {
		return new DataStorage(soundsource, packsource, lockwrite, storeinram, datadirectory);
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
	 * @return ResourcePacker if write allowed
	 */
	public abstract ResourcePacker createPacker(final String id);
	
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
