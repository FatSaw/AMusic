package me.bomb.amusic;

import java.util.UUID;

public interface SoundStopper {
	
	/**
	 * Stops amusic sound.
	 */
	public void stopSound(UUID uuid, byte id);

}
