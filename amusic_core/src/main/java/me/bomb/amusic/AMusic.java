package me.bomb.amusic;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AMusic {
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
	public List<String> getPlaylistSoundnames(UUID player);

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
	public List<Short> getPlaylistSoundlengths(UUID player);

	/**
	 * Set sound repeat mode, null to not repeat.
	 */
	public void setRepeatMode(UUID player, RepeatType repeattype);

	/**
	 * Get playing sound name.
	 *
	 * @return playing sound name.
	 */
	public String getPlayingSoundName(UUID player);

	/**
	 * Get playing sound size in seconds.
	 *
	 * @return playing sound size in seconds.
	 */
	public short getPlayingSoundSize(UUID player);

	/**
	 * Get playing sound remaining seconds.
	 *
	 * @return playing sound remaining seconds.
	 */
	public int getPlayingSoundRemain(UUID player);

	/**
	 * Loads resource pack to player.
	 */
	public void loadPack(UUID player, String name, boolean update);

	/**
	 * Get loaded pack name.
	 *
	 * @return loaded pack name.
	 */
	public String getPackName(UUID player);

	/**
	 * Stop sound from loaded pack.
	 */
	public void stopSound(UUID player);

	/**
	 * Play sound from loaded pack.
	 */
	public void playSound(UUID player, String name);
}
