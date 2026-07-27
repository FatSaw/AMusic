package me.bomb.amusic.packedinfo;

public interface SoundSource<T extends SourceEntry> {

	/**
	 * {@link SourceEntry} ready on return.
	 */
	public abstract T get(String entrykey);
	
	/**
	 * @return true if entry has at least one element
	 */
	public abstract boolean exists(String entrykey);
	
	public abstract String[] getPlaylists();
	
	public abstract String[] getSounds(String playlistname);
	
}
