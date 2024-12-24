package me.bomb.amusic;

import java.util.UUID;

public interface SoundStopper {
	
	/**
	 * Stops amusic sound.
	 */
	public void stopSound(UUID uuid, short id);
	
	public default boolean isStopAll() {
		return false;
	}
	
	public default boolean isLock() {
		return false;
	}

}
