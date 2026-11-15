package me.bomb.amusic.api;

import java.util.UUID;

public interface SoundStopper {
	
	public String AMUSIC_SOUNDID_PREFIX = "minecraft:amusic.internal.";
	
	/**
	 * Stops amusic sound.
	 */
	public void stopSound(UUID uuid, UUID soundhash);

}
