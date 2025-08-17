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
	 * Get player uuids that loaded specific playlistname.
	 *
	 * @return player uuids that loaded specific playlistname.
	 */
	public void getPlayersLoaded(String playlistname, Consumer<UUID[]> resultConsumer);
	
	/**
	 * Get the names of playlists that were loaded at least once.
	 *
	 * @return the names of playlists that were loaded at least once.
	 */
	public void getPlaylists(boolean packed, Consumer<String[]> resultConsumer);

	/**
	 * Get the names of sounds in playlist.
	 *
	 * @return the names of sounds in playlist.
	 */
	public void  getPlaylistSoundnames(String playlistname, boolean packed, Consumer<String[]> resultConsumer);

	/**
	 * Get the names of sounds in playlist that loaded to player.
	 *
	 * @return the names of sounds in playlist that loaded to player.
	 */
	public void getPlaylistSoundnames(UUID playeruuid, Consumer<String[]> resultConsumer);

	/**
	 * Get the lenghs of sounds in playlist.
	 *
	 * @return the lenghs of sounds in playlist.
	 */
	public void getPlaylistSoundlengths(String playlistname, Consumer<short[]> resultConsumer);

	/**
	 * Get the lenghs of sounds in playlist that loaded to player.
	 *
	 * @return the lenghs of sounds in playlist that loaded to player.
	 */
	public void getPlaylistSoundlengths(UUID playeruuid, Consumer<short[]> resultConsumer);

	/**
	 * Set sound repeat mode, null to not repeat.
	 */
	public void setRepeatMode(UUID playeruuid, RepeatType repeattype);

	/**
	 * Get playing sound name.
	 *
	 * @return playing sound name.
	 */
	public void getPlayingSoundName(UUID playeruuid, Consumer<String> resultConsumer);

	/**
	 * Get playing sound size in seconds.
	 *
	 * @return playing sound size in seconds.
	 */
	public void getPlayingSoundSize(UUID playeruuid, Consumer<Short> resultConsumer);

	/**
	 * Get playing sound remaining seconds.
	 *
	 * @return playing sound remaining seconds.
	 */
	public void getPlayingSoundRemain(UUID playeruuid, Consumer<Short> resultConsumer);

	/**
	 * Loads resource pack to player.
	 */
	public void loadPack(UUID[] playeruuid, String name, boolean update, StatusReport statusreport);

	/**
	 * Get loaded pack name.
	 *
	 * @return loaded pack name.
	 */
	public void getPackName(UUID playeruuid, Consumer<String> resultConsumer);

	/**
	 * Stop sound from loaded pack.
	 */
	public void stopSound(UUID playeruuid);
	
	/**
	 * Stop untrackable sound from loaded pack.
	 */
	public void stopSoundUntrackable(UUID playeruuid);

	/**
	 * Play sound from loaded pack.
	 */
	public void playSound(UUID playeruuid, String name);

	/**
	 * Play untrackable sound from loaded pack.
	 */
	public void playSoundUntrackable(UUID playeruuid, String name);
	
	/**
	 * Open upload session.
	 * 
	 * @return session token.
	 */
	public void openUploadSession(String playlistname, Consumer<UUID> resultConsumer);
	
	/**
	 * Get upload sessions.
	 * 
	 * @return upload sessions.
	 */
	public void getUploadSessions(Consumer<UUID[]> resultConsumer);
	
	/**
	 * Close upload session.
	 * 
	 * @return true if session closed successfully.
	 */
	public void closeUploadSession(UUID token, boolean save, Consumer<Boolean> resultConsumer);

	/**
	 * Close upload session.
	 * 
	 * @return true if session closed successfully.
	 */
	public void closeUploadSession(UUID token, boolean save);
}
