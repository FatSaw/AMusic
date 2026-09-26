package me.bomb.amusic.api;

import java.util.UUID;

public interface SoundStopper {
	
	/**
	 * Stops amusic sound.
	 */
	public void stopSound(UUID uuid, UUID soundhash, short id, byte part);

}
