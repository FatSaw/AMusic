package me.bomb.amusic.source;

public abstract class SoundSource {

	/**
	 * {@link SourceEntry#names} must be ready on return.
	 * Other SourceEntry fields may be not ready.
	 */
	public abstract SourceEntry get(String entrykey);
	
	/**
	 * @return true if entry has at least one element
	 */
	public abstract boolean exists(String entrykey);
	
	public abstract String[] getAll();
	
}
