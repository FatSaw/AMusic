package me.bomb.amusic;

import java.util.UUID;
import java.util.function.Consumer;

import me.bomb.amusic.resource.StatusReport;

public interface AMusic {
	
	/**
	 * Starts threads.
	 */
	public void enable();
	
	/**
	 * Stops threads.
	 */
	public void disable();
	
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
	 * Get the names of playlists that were loaded at least once.
	 *
	 * @return true if async used.
	 */
	public boolean getPlaylists(boolean packed, boolean useCache, Consumer<String[]> resultConsumer);

	/**
	 * Get the names of sounds in playlist.
	 *
	 * @return true if async used.
	 */
	public boolean getPlaylistSoundnames(String playlistname, boolean packed, boolean useCache, Consumer<String[]> resultConsumer);

	/**
	 * Get the names of sounds in playlist that loaded to player.
	 *
	 * @return true if async used.
	 */
	public boolean getPlaylistSoundnames(UUID playeruuid, boolean useCache, Consumer<String[]> resultConsumer);

	/**
	 * Get the lenghs of sounds in playlist.
	 *
	 * @return true if async used.
	 */
	public boolean getPlaylistSoundlengths(String playlistname, boolean useCache, Consumer<short[]> resultConsumer);

	/**
	 * Get the lenghs of sounds in playlist that loaded to player.
	 *
	 * @return true if async used.
	 */
	public boolean getPlaylistSoundlengths(UUID playeruuid, boolean useCache, Consumer<short[]> resultConsumer);

	/**
	 * Set sound repeat mode, null to not repeat.
	 * 
	 * @return true if async used.
	 */
	public boolean setRepeatMode(UUID playeruuid, RepeatType repeattype);

	/**
	 * Get playing sound name.
	 *
	 * @return true if async used.
	 */
	public boolean getPlayingSoundName(UUID playeruuid, Consumer<String> resultConsumer);

	/**
	 * Get playing sound size in seconds.
	 *
	 * @return true if async used.
	 */
	public boolean getPlayingSoundSize(UUID playeruuid, Consumer<Short> resultConsumer);

	/**
	 * Get playing sound remaining seconds.
	 *
	 * @return true if async used.
	 */
	public boolean getPlayingSoundRemain(UUID playeruuid, Consumer<Short> resultConsumer);

	/**
	 * Loads resource pack to player.
	 * 
	 * @return true if async used.
	 */
	public boolean loadPack(UUID[] playeruuid, String name, boolean update, StatusReport statusreport);

	/**
	 * Get loaded pack name.
	 *
	 * @return true if async used.
	 */
	public boolean getPackName(UUID playeruuid, Consumer<String> resultConsumer);

	/**
	 * Stop sound from loaded pack.
	 * 
	 * @return true if async used.
	 */
	public boolean stopSound(UUID playeruuid);
	
	/**
	 * Stop untrackable sound from loaded pack.
	 * 
	 * @return true if async used.
	 */
	public boolean stopSoundUntrackable(UUID playeruuid);

	/**
	 * Play sound from loaded pack.
	 * 
	 * @return true if async used.
	 */
	public boolean playSound(UUID playeruuid, String name);

	/**
	 * Play untrackable sound from loaded pack.
	 * 
	 * @return true if async used.
	 */
	public boolean playSoundUntrackable(UUID playeruuid, String name);
	
	/**
	 * Open upload session.
	 * 
	 * @return true if async used.
	 */
	public boolean openUploadSession(String playlistname, Consumer<UUID> resultConsumer);
	
	/**
	 * Get upload sessions.
	 * 
	 * @return true if async used.
	 */
	public boolean getUploadSessions(Consumer<UUID[]> resultConsumer);
	
	/**
	 * Close upload session.
	 * 
	 * @return true if async used.
	 */
	public boolean closeUploadSession(UUID token, boolean save, Consumer<Boolean> resultConsumer);

	/**
	 * Close upload session.
	 */
	public void closeUploadSession(UUID token, boolean save);
}
