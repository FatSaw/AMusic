package me.bomb.amusic;

import java.util.UUID;

public interface SoundStarter {
	/**
	 * Starts amusic sound.
	 */
	public void startSound(UUID uuid, UUID soundhash, short id, byte part);
	/**
	 * Starts amusic sound.
	 */
	public void startSound(UUID uuid, UUID soundhash, short id, byte part, double x, double y, double z, float volume, float pitch);

}
