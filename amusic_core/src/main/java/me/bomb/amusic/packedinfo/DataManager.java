package me.bomb.amusic.packedinfo;

import java.io.File;
import java.util.Set;

import me.bomb.amusic.resource.ResourcePacker;

public final class DataManager {
	
	private final Data data;
	private final boolean lockwrite;
	public final File datadirectory;
	
	public DataManager(Data data, File datadirectory, boolean lockwrite) {
		this.data = data;
		this.lockwrite = lockwrite;
		this.datadirectory = datadirectory;
	}
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
			data.removePlaylist(id);
			data.save();
			return true;
		}
		packer.run();
		if (data.containsPlaylist(id)) {
			DataEntry options = data.getPlaylist(id);
			options.sha1 = packer.sha1;
			options.size = packer.resourcepack.length;
			options.sounds = packer.sounds;
			options.saved = false;
			data.save();
			return true;
		}
		if(resourcefile.exists()) {
			data.setPlaylist(id, packer.sounds, (int)resourcefile.length(), filterName(id), packer.sha1);
			data.save();
			return true;
		}
		
		data.removePlaylist(id);
		data.save();
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
		if (data.containsPlaylist(id)) {
			DataEntry options = data.getPlaylist(id);
			options.sha1 = sha1;
			options.size = size;
			options.sounds = sounds;
			options.saved = false;
			data.save();
			return true;
		}
		data.setPlaylist(id, sounds, size, filterName(id), sha1);
		data.save();
		return true;
	}
	
	public DataEntry getPlaylist(final String id) {
		return data.getPlaylist(id);
	}
	
	public Set<String> getPlaylists() {
		return data.getPlaylists();
	}
	
	protected void end() {
		data.end();
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
