package me.bomb.amusic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class AMusic extends JavaPlugin {

	private static ResourceServer server;
	private static Data data;
	private static PositionTracker positiontracker;
	//PLUGIN INIT START
	public void onEnable() {
		new ConfigOptions();
		LangOptions.values();
		server = new ResourceServer(this);
		data = new Data();
		data.load();
		positiontracker = new PositionTracker();
		
		PluginCommand loadmusiccommand = getCommand("loadmusic");
		loadmusiccommand.setExecutor(new LoadmusicCommand(data));
		loadmusiccommand.setTabCompleter(new LoadmusicTabComplete(data));
		PluginCommand playmusiccommand = getCommand("playmusic");
		playmusiccommand.setExecutor(new PlaymusicCommand(positiontracker));
		playmusiccommand.setTabCompleter(new PlaymusicTabComplete(positiontracker));
		PluginCommand repeatcommand = getCommand("repeat");
		repeatcommand.setExecutor(new RepeatCommand(positiontracker));
		repeatcommand.setTabCompleter(new RepeatTabComplete());
		ResourcePacked.positiontracker = positiontracker;
		Bukkit.getPluginManager().registerEvents(new EventListener(positiontracker), this);
		if(ConfigOptions.checkpackstatus) {
			Bukkit.getPluginManager().registerEvents(new PackStatusEventListener(positiontracker), this);
		}
		if (ConfigOptions.hasplaceholderapi) {
			new AMusicPlaceholderExpansion(positiontracker).register();
		}
	}

	public void onDisable() {
		positiontracker.end();
		server.end();
		while (positiontracker.isAlive() || server.isAlive()) { //DONT STOP)
		}
	}
	//PLUGIN INIT END
	
	//API START
	//API SHOULD NOT RETURN ANY AMUSIC PLUGIN CLASS
	//API SHOULD NOT BE USED FROM ANY AMUSIC PLUGIN CLASS

	/**
	 * Get the names of playlists that were loaded at least once.
	 *
	 * @return the names of playlists that were loaded at least once.
	 */
	public static Set<String> getPlaylists() {
		return data.getPlaylists();
	}

	/**
	 * Get the names of sounds in playlist.
	 *
	 * @return the names of sounds in playlist.
	 */
	public static List<String> getPlaylistSoundnames(String playlistname) {
		return data.getPlaylist(playlistname).sounds;
	}

	/**
	 * Get the names of sounds in playlist that loaded to player.
	 *
	 * @return the names of sounds in playlist that loaded to player.
	 */
	public static List<String> getPlaylistSoundnames(Player player) {
		ArrayList<SoundInfo> soundinfos = positiontracker.getSoundInfo(player.getUniqueId());
		if(soundinfos==null) {
			return null;
		}
		int infossize = soundinfos.size();
		List<String> soundnames = new ArrayList<String>(infossize);
		for(int i = 0;i<infossize;++i) {
			soundnames.add(soundinfos.get(i).name);
		}
		return soundnames;
	}

	/**
	 * Get the lenghs of sounds in playlist.
	 *
	 * @return the lenghs of sounds in playlist.
	 */
	public static List<Short> getPlaylistSoundlengths(String playlistname) {
		return data.getPlaylist(playlistname).length;
	}

	/**
	 * Get the lenghs of sounds in playlist that loaded to player.
	 *
	 * @return the lenghs of sounds in playlist that loaded to player.
	 */
	public static List<Short> getPlaylistSoundlengths(Player player) {
		ArrayList<SoundInfo> soundinfos = positiontracker.getSoundInfo(player.getUniqueId());
		if(soundinfos==null) {
			return null;
		}
		int infossize = soundinfos.size();
		List<Short> soundlengths = new ArrayList<Short>(infossize);
		for(int i = 0;i<infossize;++i) {
			soundlengths.add(soundinfos.get(i).length);
		}
		return soundlengths;
	}

	/**
	 * Set sound repeat mode, null to not repeat.
	 */
	public static void setRepeatMode(Player player, RepeatType repeattype) {
		positiontracker.setRepeater(player.getUniqueId(), repeattype);
	}

	/**
	 * Get playing sound name.
	 *
	 * @return playing sound name.
	 */
	public static String getPlayingSoundName(Player player) {
		return positiontracker.getPlaying(player.getUniqueId());
	}

	/**
	 * Get playing sound size in seconds.
	 *
	 * @return playing sound size in seconds.
	 */
	public static short getPlayingSoundSize(Player player) {
		return positiontracker.getPlayingSize(player.getUniqueId());
	}

	/**
	 * Get playing sound remaining seconds.
	 *
	 * @return playing sound remaining seconds.
	 */
	public static int getPlayingSoundRemain(Player player) {
		return positiontracker.getPlayingRemain(player.getUniqueId());
	}

	/**
	 * Loads resource pack to player.
	 */
	public static void loadPack(Player player, String name, boolean update) {
		ResourcePacked.load(player, data, name, update);
	}

	/**
	 * Get loaded pack name.
	 *
	 * @return loaded pack name.
	 */
	public static String getPackName(Player player) {
		return positiontracker.getPlaylistName(player.getUniqueId());
	}

	/**
	 * Stop sound from loaded pack.
	 */
	public static void stopSound(Player player) {
		positiontracker.stopMusic(player);
	}

	/**
	 * Play sound from loaded pack.
	 */
	public static void playSound(Player player, String name) {
		positiontracker.playMusic(player, name);
	}
	
	//API END

}
