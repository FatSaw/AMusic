package me.bomb.amusic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import me.bomb.amusic.LangOptions.Placeholders;

final class PositionTracker {
	private static final Map<UUID,Playing> trackers = new HashMap<UUID,Playing>();
	protected PositionTracker(JavaPlugin plugin) {
		new BukkitRunnable() {
			@Override
			public void run() {
				for(UUID uuid : trackers.keySet()) {
					Playing playing = trackers.get(uuid);
					if(--playing.remaining<0) {
						trackers.remove(uuid);
						Repeater repeater = Repeater.getRepeater(uuid);
						if(repeater==null) {
							continue;
						}
						repeater.next(uuid, playing.currenttrack);
					}
				}
			}
		}.runTaskTimerAsynchronously(plugin, 20L, 20L);
	}
	
	protected static String getPlaying(Player player) {
		if(!trackers.containsKey(player.getUniqueId())) {
			return null;	
		}
		List<String> activeplaying = ResourcePacked.getActivePlaylist(player);
		if(activeplaying==null) {
			return null;
		}
		Playing playing = trackers.get(player.getUniqueId());
		
		if(playing.currenttrack<activeplaying.size()) {
			return activeplaying.get(playing.currenttrack);
		}
		return null;
	}
	
	protected static void playMusic(Player player,String name) {
		List<String> activeplaylist = ResourcePacked.getActivePlaylist(player);
		List<Integer> activelengths = ResourcePacked.getActiveLengths(player);
		if(activeplaylist==null||activelengths==null) {
			return;
		}
		byte id = (byte) activeplaylist.indexOf(name);
		if(id==-1) {
			return;
		}
		stopMusic(player);
		LangOptions.playmusic_playing.sendMsgActionbar(player,new Placeholders("%soundname%",name));
		Playing playing = new Playing(id, activelengths.get(id));
		trackers.put(player.getUniqueId(),playing);
        player.playSound(player.getLocation(), "amusic.music".concat(Byte.toString(id)), 1.0E9f, 1.0f);
    }
	protected static void playMusic(UUID uuid,byte id,boolean cycle) {
		if(uuid==null||id<0) {
			return;
		}
		Player player = Bukkit.getPlayer(uuid);
		if(player==null||!player.isOnline()) {
			return;
		}
		List<String> activeplaylist = ResourcePacked.getActivePlaylist(player);
		List<Integer> activelengths = ResourcePacked.getActiveLengths(player);
		if(activeplaylist==null||activelengths==null) {
			return;
		}
		stopMusic(player);
		if(id>=activelengths.size()) {
			if(cycle) {
				id=0x00;
			} else {
				return;
			}
		}
		LangOptions.playmusic_playing.sendMsgActionbar(player,new Placeholders("%soundname%",activeplaylist.get(id)));
		
		Playing playing = new Playing(id, activelengths.get(id));
		trackers.put(player.getUniqueId(),playing);
        player.playSound(player.getLocation(), "amusic.music".concat(Byte.toString(id)), 1.0E9f, 1.0f);
	}
	protected static void stopMusic(Player player) {
		Playing playing = trackers.remove(player.getUniqueId());
		if(playing==null) {
			for(byte i = 0;i!=-128;++i) {
	        	player.stopSound("amusic.music".concat(Byte.toString(i)));
	    	}
			return;
		}
		player.stopSound("amusic.music".concat(Byte.toString(playing.currenttrack)));
    }
	protected static void remove(UUID uuid) {
		trackers.remove(uuid);
	}
	
	private static class Playing {
		private final byte currenttrack;
		private int remaining;
		
		private Playing(byte currenttrack, int remaining) {
			this.currenttrack = currenttrack;
			this.remaining = remaining;
		}
	}
}
