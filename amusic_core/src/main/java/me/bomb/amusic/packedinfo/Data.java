package me.bomb.amusic.packedinfo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import me.bomb.amusic.resource.ResourcePacker;

public abstract class Data {
	protected final Map<String, DataEntry> options = new HashMap<String, DataEntry>();
	private final boolean lockwrite;
	protected Data(boolean lockwrite) {
		this.lockwrite = lockwrite;
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
	 * @param packer
	 * @return true if something changed
	 */
	public boolean update(final String id, final ResourcePacker packer) {
		if(this.lockwrite || id == null) {
			return false;
		}
		final File resourcefile;
		if(packer == null || (resourcefile = packer.resourcefile) == null) {
			removePlaylist(id);
			save();
			return true;
		}
		packer.run();
		if (containsPlaylist(id)) {
			DataEntry options = getPlaylist(id);
			options.sha1 = packer.sha1;
			options.size = packer.resourcepack.length;
			options.sounds = packer.sounds;
			options.saved = false;
			save();
			return true;
		}
		if(resourcefile.exists()) {
			setPlaylist(id, packer.sounds, (int)resourcefile.length(), filterName(id), packer.sha1);
			save();
			return true;
		}
		
		removePlaylist(id);
		save();
		return true;
	}
	
	/**
	 * Update or create new packed info
	 * @param id
	 * @return true if something changed
	 */
	public boolean update(final String id,final int size,final byte[] sha1,final SoundInfo[] sounds) {
		if(this.lockwrite || id == null) {
			return false;
		}
		if (containsPlaylist(id)) {
			DataEntry options = getPlaylist(id);
			options.sha1 = sha1;
			options.size = size;
			options.sounds = sounds;
			options.saved = false;
			save();
			return true;
		}
		setPlaylist(id, sounds, size, filterName(id), sha1);
		save();
		return true;
	}
	
	protected final void setPlaylist(String playlistname, SoundInfo[] sounds, int resourcesize, String resourcename, byte[] sha1) {
		try {
			options.put(playlistname, new DataEntry(resourcesize, resourcename, sounds, sha1));
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

	protected final void removePlaylist(String playlistname) {
		options.remove(playlistname);
	}

	public final Set<String> getPlaylists() {
		return options == null ? null : options.keySet();
	}
	
	public static String filterName(String name) {
		char[] chars = name.toCharArray();
		int finalcount = 0;
		int i = chars.length;
		while(--i > -1) {
			char c = chars[i];
			//if(c == '/' || c == '\\' || c == ':' || c == '<' || c == '>' || c == '*' || c == '?' || c == '|' || c == '\"' || c == '\0' || (c > 0 && c < 32)) { // who use windows for servers
			if(c == '/' || c == '\0') { //unix
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
