package me.bomb.amusic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class AMusic extends JavaPlugin implements Listener {
    private Map<UUID, String> lastsound;
    
    private ResourceServer server;
    private static Data data;

    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
    	UUID playeruuid = event.getPlayer().getUniqueId();
    	ResourcePacked.remove(playeruuid);
    	CachedResource.remove(playeruuid);
    	Repeater.removeRepeater(playeruuid);
    	PositionTracker.remove(playeruuid);
    	lastsound.remove(event.getPlayer().getUniqueId());
    }
    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
    	Repeater.setRepeater(event.getPlayer().getUniqueId(), false, true);
    }
	public void onLoad() {
		new ConfigOptions();
        lastsound = new HashMap<UUID, String>();
    }
	
	public void onEnable() {
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
        for(Player player : Bukkit.getOnlinePlayers()) {
        	Repeater.setRepeater(player.getUniqueId(), false, true);
        }
    }
	
	public void onDisable() {
        server.close();
        while (server.isAlive()) {}
    }
	
	public static Set<String> getPlaylists() {
		return data.getPlaylists();
	}
	
	public static List<String> getPlaylistSoundnames(String playlistname) {
		return data.getPlaylist(playlistname).sounds;
	}
	public static List<String> getPlaylistSoundnames(Player player) {
		return ResourcePacked.getActivePlaylist(player);
	}
	
	public static List<Integer> getPlaylistSoundlengths(String playlistname) {
		return data.getPlaylist(playlistname).length;
	}
	public static List<Integer> getPlaylistSoundlengths(Player player) {
		return ResourcePacked.getActiveLengths(player);
	}
	
	public static void loadPack(Player player, String name, boolean update) {
		ResourcePacked.load(player, data, name, update);
	}
	
	public static void setRepeatMode(Player player,boolean repeat,boolean one) {
		Repeater.setRepeater(player.getUniqueId(), repeat, one);
	}
	
	public static void stopSound(Player player) {
		PositionTracker.stopMusic(player);
	}
	
	public static void playSound(Player player,String name) {
		PositionTracker.playMusic(player, name);
	}
	
	public static String getPlayingSoundName(Player player) {
		return PositionTracker.getPlaying(player);
	}
	
}
