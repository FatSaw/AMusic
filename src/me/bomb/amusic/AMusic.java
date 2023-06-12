package me.bomb.amusic;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class AMusic extends JavaPlugin implements Listener {
    
    private ResourceServer server;
    private static Data data;
    private final HashSet<BukkitTask> tasks = new HashSet<BukkitTask>(2);
    
    protected void addTask(BukkitTask task) {
    	tasks.add(task);
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
    	UUID playeruuid = event.getPlayer().getUniqueId();
    	ResourcePacked.remove(playeruuid);
    	CachedResource.remove(playeruuid);
    	Repeater.setRepeater(playeruuid, null);
    	PositionTracker.remove(playeruuid);
    }
    @EventHandler
    public void playerRespawn(PlayerRespawnEvent event) {
    	PositionTracker.stopMusic(event.getPlayer());
    }
	
	public void onEnable() {
		new ConfigOptions();
		LangOptions.values();
        server = new ResourceServer(this);
        data = new Data();
        data.load();
        new PositionTracker(this);
        PluginCommand loadmusiccommand = getCommand("loadmusic");
        loadmusiccommand.setExecutor(new LoadmusicCommand(data));
        loadmusiccommand.setTabCompleter(new LoadmusicTabComplete(data));
        PluginCommand playmusiccommand = getCommand("playmusic");
        playmusiccommand.setExecutor(new PlaymusicCommand());
        playmusiccommand.setTabCompleter(new PlaymusicTabComplete());
        PluginCommand repeatcommand = getCommand("repeat");
        repeatcommand.setExecutor(new RepeatCommand());
        repeatcommand.setTabCompleter(new RepeatTabComplete());
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new PackStatusListener(), this);
        if(ConfigOptions.hasplaceholderapi) {
            new AMusicPlaceholderExpansion().register();
        }
    }
	
	public void onDisable() {
        server.close();
        for(BukkitTask task : tasks) {
        	task.cancel();
        }
        while (server.isAlive()) {}
    }
	
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
		return ResourcePacked.getPackInfo(player.getUniqueId()).songs;
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
		return ResourcePacked.getPackInfo(player.getUniqueId()).lengths;
	}
	
	/**
	   * Set sound repeat mode, null to not repeat.
	*/
	public static void setRepeatMode(Player player,RepeatType repeattype) {
		Repeater.setRepeater(player.getUniqueId(), repeattype);
	}
	
	/**
	   * Get playing sound name.
	   *
	   * @return playing sound name.
	*/
	public static String getPlayingSoundName(Player player) {
		return PositionTracker.getPlaying(player.getUniqueId());
	}
	
	/**
	   * Get playing sound size in seconds.
	   *
	   * @return playing sound size in seconds.
	*/
	public static short getPlayingSoundSize(Player player) {
		return PositionTracker.getPlayingSize(player.getUniqueId());
	}
	
	/**
	   * Get playing sound remaining seconds.
	   *
	   * @return playing sound remaining seconds.
	*/
	public static int getPlayingSoundRemain(Player player) {
		return PositionTracker.getPlayingRemain(player.getUniqueId());
	}
	
	/**
	   * Loads resource pack to player.
	*/
	public static void loadPack(Player player, String name, boolean update) {
		ResourcePacked.load(player, data, name, update);
	}
	
	/**
	   * Stop sound from loaded pack.
	*/
	public static void stopSound(Player player) {
		PositionTracker.stopMusic(player);
	}
	
	/**
	   * Play sound from loaded pack.
	*/
	public static void playSound(Player player,String name) {
		PositionTracker.playMusic(player, name);
	}
	
}
