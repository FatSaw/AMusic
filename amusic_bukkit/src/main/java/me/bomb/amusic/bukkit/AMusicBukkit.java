package me.bomb.amusic.bukkit;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.PackSender;
import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.RepeatType;
import me.bomb.amusic.SoundInfo;
import me.bomb.amusic.SoundStarter;
import me.bomb.amusic.SoundStopper;
import me.bomb.amusic.bukkit.command.LoadmusicCommand;
import me.bomb.amusic.bukkit.command.LoadmusicTabComplete;
import me.bomb.amusic.bukkit.command.PlaymusicCommand;
import me.bomb.amusic.bukkit.command.PlaymusicTabComplete;
import me.bomb.amusic.bukkit.command.RepeatCommand;
import me.bomb.amusic.bukkit.command.RepeatTabComplete;
import me.bomb.amusic.bukkit.legacy.LegacyPackSender_1_10_R1;
import me.bomb.amusic.bukkit.legacy.LegacyPackSender_1_8_R3;
import me.bomb.amusic.bukkit.legacy.LegacyPackSender_1_9_R2;
import me.bomb.amusic.bukkit.legacy.LegacySoundStopper_1_8_R3;
import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.resourceserver.ResourceServer;

public final class AMusicBukkit extends JavaPlugin implements AMusic {
	
	private static ResourceServer server;
	private Data data;
	private ResourceManager resourcemanager;
	private PositionTracker positiontracker;
	private PackSender packsender;
	private static AMusicBukkit apiimpl;
	
	public AMusicBukkit() {
		AMusicBukkit.apiimpl = this;
	}
	
	//PLUGIN INIT START
	public void onEnable() {
		new ConfigOptions();
		LangOptions.values();
		ConcurrentHashMap<Object,InetAddress> playerips = new ConcurrentHashMap<Object,InetAddress>(16,0.75f,1);
		
		data = new Data();
		data.load();
		SoundStopper soundstopper;
		boolean legacystopper = false;
		String nmsversion = Bukkit.getServer().getClass().getPackage().getName().substring(23);
		switch (nmsversion) {
		case "v1_8_R3":
			legacystopper = true;
			soundstopper = new LegacySoundStopper_1_8_R3();
			packsender = new LegacyPackSender_1_8_R3();
		break;
		case "v1_9_R2":
			soundstopper = new BukkitSoundStopper();
			packsender = new LegacyPackSender_1_9_R2();
		break;
		case "v1_10_R1":
			soundstopper = new BukkitSoundStopper();
			packsender = new LegacyPackSender_1_10_R1();
		break;
		default:
			soundstopper = new BukkitSoundStopper();
			packsender = new BukkitPackSender();
		}
		
		SoundStarter soundstarter = new BukkitSoundStarter();
		resourcemanager = new ResourceManager(ConfigOptions.maxpacksize, ConfigOptions.servercache, ConfigOptions.clientcache);
		positiontracker = new PositionTracker(soundstarter, soundstopper, legacystopper, ConfigOptions.hasplaceholderapi);
		
		server = new ResourceServer(playerips, ConfigOptions.port, resourcemanager);
		
		PluginCommand loadmusiccommand = getCommand("loadmusic");
		loadmusiccommand.setExecutor(new LoadmusicCommand(data, resourcemanager, positiontracker, packsender));
		loadmusiccommand.setTabCompleter(new LoadmusicTabComplete(data));
		PluginCommand playmusiccommand = getCommand("playmusic");
		playmusiccommand.setExecutor(new PlaymusicCommand(positiontracker));
		playmusiccommand.setTabCompleter(new PlaymusicTabComplete(positiontracker));
		PluginCommand repeatcommand = getCommand("repeat");
		repeatcommand.setExecutor(new RepeatCommand(positiontracker));
		repeatcommand.setTabCompleter(new RepeatTabComplete());
		Bukkit.getPluginManager().registerEvents(new EventListener(resourcemanager, positiontracker, playerips), this);
		if(ConfigOptions.checkpackstatus) {
			Bukkit.getPluginManager().registerEvents(new PackStatusEventListener(resourcemanager, positiontracker), this);
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
	
	public static AMusic getApi() {
		return apiimpl;
	}

	//API START
	//API SHOULD NOT RETURN ANY AMUSIC PLUGIN CLASS
	//API SHOULD NOT BE USED FROM ANY AMUSIC PLUGIN CLASS
	
	/**
	 * Get the names of playlists that were loaded at least once.
	 *
	 * @return the names of playlists that were loaded at least once.
	 */
	public Set<String> getPlaylists() {
		return data.getPlaylists();
	}

	/**
	 * Get the names of sounds in playlist.
	 *
	 * @return the names of sounds in playlist.
	 */
	public List<String> getPlaylistSoundnames(String playlistname) {
		return data.getPlaylist(playlistname).sounds;
	}

	/**
	 * Get the names of sounds in playlist that loaded to player.
	 *
	 * @return the names of sounds in playlist that loaded to player.
	 */
	public List<String> getPlaylistSoundnames(UUID playeruuid) {
		ArrayList<SoundInfo> soundinfos = positiontracker.getSoundInfo(playeruuid);
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
	public List<Short> getPlaylistSoundlengths(String playlistname) {
		return data.getPlaylist(playlistname).length;
	}

	/**
	 * Get the lenghs of sounds in playlist that loaded to player.
	 *
	 * @return the lenghs of sounds in playlist that loaded to player.
	 */
	public List<Short> getPlaylistSoundlengths(UUID playeruuid) {
		ArrayList<SoundInfo> soundinfos = positiontracker.getSoundInfo(playeruuid);
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
	public void setRepeatMode(UUID playeruuid, RepeatType repeattype) {
		positiontracker.setRepeater(playeruuid, repeattype);
	}

	/**
	 * Get playing sound name.
	 *
	 * @return playing sound name.
	 */
	public String getPlayingSoundName(UUID playeruuid) {
		return positiontracker.getPlaying(playeruuid);
	}

	/**
	 * Get playing sound size in seconds.
	 *
	 * @return playing sound size in seconds.
	 */
	public short getPlayingSoundSize(UUID playeruuid) {
		return positiontracker.getPlayingSize(playeruuid);
	}

	/**
	 * Get playing sound remaining seconds.
	 *
	 * @return playing sound remaining seconds.
	 */
	public int getPlayingSoundRemain(UUID playeruuid) {
		return positiontracker.getPlayingRemain(playeruuid);
	}

	/**
	 * Loads resource pack to player.
	 */
	public void loadPack(UUID playeruuid, String name, boolean update) {
		ResourcePacked.load(data, resourcemanager, positiontracker, packsender, playeruuid, name, update);
	}

	/**
	 * Get loaded pack name.
	 *
	 * @return loaded pack name.
	 */
	public String getPackName(UUID playeruuid) {
		return positiontracker.getPlaylistName(playeruuid);
	}

	/**
	 * Stop sound from loaded pack.
	 */
	public void stopSound(UUID playeruuid) {
		positiontracker.stopMusic(playeruuid);
	}

	/**
	 * Play sound from loaded pack.
	 */
	public void playSound(UUID playeruuid, String name) {
		positiontracker.playMusic(playeruuid, name);
	}
	
	//API END

}
