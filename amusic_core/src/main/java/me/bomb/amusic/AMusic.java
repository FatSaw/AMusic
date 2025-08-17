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
	public boolean getPlayersLoaded(String playlistname, Consumer<UUID[]> resultConsumer);
	
	/**
	 * Get the names of playlists that were loaded at least once.
	 *
	 * @return the names of playlists that were loaded at least once.
	 */
	public boolean getPlaylists(boolean packed, Consumer<String[]> resultConsumer);

	/**
	 * Get the names of sounds in playlist.
	 *
	 * @return the names of sounds in playlist.
	 */
	public boolean getPlaylistSoundnames(String playlistname, boolean packed, Consumer<String[]> resultConsumer);

	/**
	 * Get the names of sounds in playlist that loaded to player.
	 *
	 * @return the names of sounds in playlist that loaded to player.
	 */
	public boolean getPlaylistSoundnames(UUID playeruuid, Consumer<String[]> resultConsumer);

	/**
	 * Get the lenghs of sounds in playlist.
	 *
	 * @return the lenghs of sounds in playlist.
	 */
	public boolean getPlaylistSoundlengths(String playlistname, Consumer<short[]> resultConsumer);

	/**
	 * Get the lenghs of sounds in playlist that loaded to player.
	 *
	 * @return the lenghs of sounds in playlist that loaded to player.
	 */
	public boolean getPlaylistSoundlengths(UUID playeruuid, Consumer<short[]> resultConsumer);

	/**
	 * Set sound repeat mode, null to not repeat.
	 */
	public boolean setRepeatMode(UUID playeruuid, RepeatType repeattype);

	/**
	 * Get playing sound name.
	 *
	 * @return playing sound name.
	 */
	public boolean getPlayingSoundName(UUID playeruuid, Consumer<String> resultConsumer);

	/**
	 * Get playing sound size in seconds.
	 *
	 * @return playing sound size in seconds.
	 */
	public boolean getPlayingSoundSize(UUID playeruuid, Consumer<Short> resultConsumer);

	/**
	 * Get playing sound remaining seconds.
	 *
	 * @return playing sound remaining seconds.
	 */
	public boolean getPlayingSoundRemain(UUID playeruuid, Consumer<Short> resultConsumer);

	/**
	 * Loads resource pack to player.
	 */
	public boolean loadPack(UUID[] playeruuid, String name, boolean update, StatusReport statusreport);

	/**
	 * Get loaded pack name.
	 *
	 * @return loaded pack name.
	 */
	public boolean getPackName(UUID playeruuid, Consumer<String> resultConsumer);

	/**
	 * Stop sound from loaded pack.
	 */
	public boolean stopSound(UUID playeruuid);
	
	/**
	 * Stop untrackable sound from loaded pack.
	 */
	public boolean stopSoundUntrackable(UUID playeruuid);

	/**
	 * Play sound from loaded pack.
	 */
	public boolean playSound(UUID playeruuid, String name);

	/**
	 * Play untrackable sound from loaded pack.
	 */
	public boolean playSoundUntrackable(UUID playeruuid, String name);
	
	/**
	 * Open upload session.
	 * 
	 * @return session token.
	 */
	public boolean openUploadSession(String playlistname, Consumer<UUID> resultConsumer);
	
	/**
	 * Get upload sessions.
	 * 
	 * @return upload sessions.
	 */
	public boolean getUploadSessions(Consumer<UUID[]> resultConsumer);
	
	/**
	 * Close upload session.
	 * 
	 * @return true if session closed successfully.
	 */
	public boolean closeUploadSession(UUID token, boolean save, Consumer<Boolean> resultConsumer);

	/**
	 * Close upload session.
	 * 
	 * @return true if session closed successfully.
	 */
	public void closeUploadSession(UUID token, boolean save);
}
