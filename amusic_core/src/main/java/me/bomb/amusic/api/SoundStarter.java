package me.bomb.amusic.api;

import java.util.UUID;

public interface SoundStarter {
	
	public String AMUSIC_SOUNDID_PREFIX = "minecraft:amusic.internal.";
	
	/**
	 * Starts amusic sound.
	 */
	public void startSound(UUID uuid, UUID soundhash);
}
