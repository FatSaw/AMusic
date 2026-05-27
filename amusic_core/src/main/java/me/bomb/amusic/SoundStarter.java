package me.bomb.amusic;

import java.util.UUID;

public interface SoundStarter {
	/**
	 * Starts amusic sound.
	 */
	public void startSound(UUID uuid, UUID soundhash, short id, byte part);

}
