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
	 * Loads resource pack to player.
	 * 
	 * @return true if async used.
	 */
	public boolean loadResourcepack(UUID[] playeruuid, String name, boolean update, Consumer<LoadPackResult> resultConsumer);
	
	/**
	 * Play sound from loaded pack.
	 * 
	 * @return true if async used.
	 */
	public boolean playSound(UUID playeruuid, String name, Consumer<Boolean> resultConsumer);

	/**
	 * Stop sound from loaded pack.
	 * 
	 * @return true if async used.
	 */
	public boolean stopSound(UUID playeruuid, Consumer<Boolean> resultConsumer);
	
	/**
	 * Set sound repeat mode, null to not repeat.
	 * 
	 * @return true if async used.
	 */
	public boolean setRepeat(UUID playeruuid, RepeatType repeattype);
	
	/**
	 * Set resourcepack customdata.
	 * 
	 * @return true if async used.
	 */
	public boolean setResourcepackCustomdata(String resourcepackname, byte[] customdata, Consumer<Boolean> resultConsumer);
	
	
	/**
	 * Get player uuids that loaded specific resourcepackname.
	 *
	 * @return true if async used.
	 */
	public boolean getLoadedPlayers(String resourcepackname, Consumer<UUID[]> resultConsumer);
	
	/**
	 * Get loaded pack name.
	 *
	 * @return true if async used.
	 */
	public boolean getLoadedResourcepackName(UUID playeruuid, Consumer<String> resultConsumer);
	
	
	/**
	 * Get cached resourcepack info list.
	 */
	public String[] getResourcepackInfoListCached();
	/**
	 * Get resourcepack info list.
	 * 
	 * @return true if async used.
	 */
	public boolean getResourcepackInfoList(Consumer<String[]> resultConsumer);
	
	/**
	 * Get cached resourcepack info.
	 */
	public ResourcepackInfo getResourcepackInfoCached(String resourcepackname);

	/**
	 * Get resourcepack info.
	 * 
	 * @return true if async used.
	 */
	public boolean getResourcepackInfo(String resourcepackname, Consumer<ResourcepackInfo> resultConsumer);
	
	/**
	 * Get cached resourcepack info.
	 */
	public ResourcepackInfo getResourcepackInfoCached(UUID playeruuid);
	
	/**
	 * Get resourcepack info.
	 * 
	 * @return true if async used.
	 */
	public boolean getResourcepackInfo(UUID playeruuid, Consumer<ResourcepackInfo> resultConsumer);

		
	/**
	 * Get not packed resourcepack list.
	 */
	public String[] getSourceResourcepackNameListCached();
	/**
	 * Get not packed resourcepack list.
	 *
	 * @return true if async used.
	 */
	public boolean getSourceResourcepackNameList(Consumer<String[]> resultConsumer);
	
	/**
	 * Get not packed soundnames list.
	 */
	public String[] getSourceSoundnameListCached(String resourcepackname);
	
	/**
	 * Get not packed soundnames list.
	 *
	 * @return true if async used.
	 */
	public boolean getSourceSoundnameList(String resourcepackname, Consumer<String[]> resultConsumer);
}
