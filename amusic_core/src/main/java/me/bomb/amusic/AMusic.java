package me.bomb.amusic;

import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
	 * Set main AMusic instance {@link AMusic#instance}.
	 * Should be used only during AMusic plugin initialization;
	 */
	/*public void setAPI() throws ExceptionInInitializerError {
		if(AMusic.instance!=null) throw new ExceptionInInitializerError("AMusic API already initialized!");
		AMusic.instance = this;
	}
	
	public static AMusic API() {
		return AMusic.instance;
	}*/
	
	/**
	 * Get the names of playlists that were loaded at least once.
	 *
	 * @return the names of playlists that were loaded at least once.
	 */
	public Set<String> getPlaylists();

	/**
	 * Get the names of sounds in playlist.
	 *
	 * @return the names of sounds in playlist.
	 */
	public List<String> getPlaylistSoundnames(String playlistname);

	/**
	 * Get the names of sounds in playlist that loaded to player.
	 *
	 * @return the names of sounds in playlist that loaded to player.
	 */
	public List<String> getPlaylistSoundnames(UUID playeruuid);

	/**
	 * Get the lenghs of sounds in playlist.
	 *
	 * @return the lenghs of sounds in playlist.
	 */
	public List<Short> getPlaylistSoundlengths(String playlistname);

	/**
	 * Get the lenghs of sounds in playlist that loaded to player.
	 *
	 * @return the lenghs of sounds in playlist that loaded to player.
	 */
	public List<Short> getPlaylistSoundlengths(UUID playeruuid);

	/**
	 * Set sound repeat mode, null to not repeat.
	 */
	public void setRepeatMode(UUID playeruuid, RepeatType repeattype);

	/**
	 * Get playing sound name.
	 *
	 * @return playing sound name.
	 */
	public String getPlayingSoundName(UUID playeruuid);

	/**
	 * Get playing sound size in seconds.
	 *
	 * @return playing sound size in seconds.
	 */
	public short getPlayingSoundSize(UUID playeruuid);

	/**
	 * Get playing sound remaining seconds.
	 *
	 * @return playing sound remaining seconds.
	 */
	public short getPlayingSoundRemain(UUID playeruuid);

	/**
	 * Loads resource pack to player.
	 */
	public void loadPack(UUID[] playeruuid, String name, boolean update, StatusReport statusreport);

	/**
	 * Get loaded pack name.
	 *
	 * @return loaded pack name.
	 */
	public String getPackName(UUID playeruuid);

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
	public UUID openUploadSession(String playlistname);
	
	/**
	 * Get upload sessions.
	 * 
	 * @return upload sessions.
	 */
	public Enumeration<UUID> getUploadSessions();
	
	/**
	 * Close upload session.
	 * 
	 * @return true if session closed successfully.
	 */
	public boolean closeUploadSession(UUID token);
}
