package me.bomb.amusic.api;

import java.util.UUID;
import java.util.function.Consumer;

public interface AMusic {
	
	/**
	 * Update cached values.
	 *
	 * @return true if cache supported.
	 */
	public boolean updateCache();
	
	/**
	 * Starts threads.
	 */
	public void enable();
	
	/**
	 * Stops threads.
	 */
	public void disable();
	
	/**
	 * Handle login.
	 */
	public void login(UUID playeruuid);
	
	/**
	 * Handle logout.
	 */
	public void logout(UUID playeruuid);
	
	/**
	 * Get player uuids that loaded specific playlistname.
	 *
	 * @return true if async used.
	 */
	public boolean getPlayersLoaded(String playlistname, Consumer<UUID[]> resultConsumer);

	/**
	 * Set sound repeat mode, null to not repeat.
	 * 
	 * @return true if async used.
	 */
	public boolean setRepeatMode(UUID playeruuid, RepeatType repeattype);

	/**
	 * Loads resource pack to player.
	 * 
	 * @return true if async used.
	 */
	public boolean loadPack(UUID[] playeruuid, String name, boolean update, Consumer<LoadPackResult> resultConsumer);

	/**
	 * Stop sound from loaded pack.
	 * 
	 * @return true if async used.
	 */
	public boolean stopSound(UUID playeruuid, Consumer<Boolean> resultConsumer);
	
	/**
	 * Play sound from loaded pack.
	 * 
	 * @return true if async used.
	 */
	public boolean playSound(UUID playeruuid, String name, Consumer<Boolean> resultConsumer);
	
	/**
	 * Get cached resourcepack info.
	 */
	public ResourcepackInfo getResourcepackInfoCached(String resourcepackname);

	/**
	 * Get resourcepack info.
	 */
	public boolean getResourcepackInfo(String resourcepackname, Consumer<ResourcepackInfo> resultConsumer);
	
	/**
	 * Get cached resourcepack info.
	 */
	public ResourcepackInfo getResourcepackInfoCached(UUID playeruuid);
	
	/**
	 * Get resourcepack info.
	 */
	public boolean getResourcepackInfo(UUID playeruuid, Consumer<ResourcepackInfo> resultConsumer);

	/**
	 * Set resourcepack customdata.
	 */
	public boolean setResourcepackCustomData(String resourcepackname, byte[] customdata, Consumer<Boolean> resultConsumer);

	public String[] getListResourcepackInfoCached();
	
	public boolean getListResourcepackInfo(Consumer<String[]> resultConsumer);
	
	public String[] getListResourcepackCached();
	
	public boolean getListResourcepack(Consumer<String[]> resultConsumer);
	
	
	/**
	 * Get not packed soundnames.
	 */
	public String[] getListResourcepackSoundsCached(String resourcepackname);
	
	/**
	 * Get not packed soundnames.
	 *
	 * @return true if async used.
	 */
	public boolean getListResourcepackSounds(String resourcepackname, Consumer<String[]> resultConsumer);
	
	/**
	 * Get loaded pack name.
	 *
	 * @return true if async used.
	 */
	public boolean getPackName(UUID playeruuid, Consumer<String> resultConsumer);

}
