package me.bomb.amusic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.bomb.amusic.LangOptions.Placeholders;

final class PositionTracker {
	private static final Map<UUID,Playing> trackers = new HashMap<UUID,Playing>();
	protected PositionTracker(AMusic plugin) {
		plugin.addTask(new BukkitRunnable() {
			@Override
			public void run() {
				for(UUID uuid : trackers.keySet()) {
					Playing playing = trackers.get(uuid);
					playing.remainingf = sectotime(--playing.remaining);
					if(playing.remaining<0) {
						trackers.remove(uuid);
						Repeater.next(uuid, playing.currenttrack);
					}
				}
			}
		}.runTaskTimerAsynchronously(plugin, 20L, 20L));
	}
	private static short sectotime(short sec) {
		if(!ConfigOptions.hasplaceholderapi||sec<1) return 0;
		if(sec>0x7080) {
			return 0x7EFB;
		}
		short res=0;
		byte min=0,hour=0;
		while((res+=60)<sec) {
    		if(++min<60) {
    			continue;
    		}
			min=0;
			++hour;
		}
		res-=59;
		sec-=res;
		//Bukkit.getLogger().warning("SEC: "+sec+" MIN:"+min+" HOUR:"+hour+" RES:"+res);
		sec|=((short)min<<6);
		sec|=((short)hour<<6);
		return sec;
	}
	
	protected static String getPlaying(UUID uuid) {
		if(!trackers.containsKey(uuid)) {
			return null;	
		}
		List<String> activeplaying = ResourcePacked.getPackInfo(uuid).songs;
		if(activeplaying==null) {
			return null;
		}
		Playing playing = trackers.get(uuid);
		
		if(playing.currenttrack<activeplaying.size()) {
			return activeplaying.get(playing.currenttrack);
		}
		return null;
	}
	
	protected static short getPlayingSize(UUID uuid) {
		List<Short> activelengths = ResourcePacked.getPackInfo(uuid).lengths;
		if(!trackers.containsKey(uuid) || activelengths==null) {
			return -1;	
		}
		Playing playing = trackers.get(uuid);
		return activelengths.get(playing.currenttrack);
	}
	
	protected static short getPlayingSizeF(UUID uuid) {
		List<Short> activelengths = ResourcePacked.getPackInfo(uuid).lengths;
		if(!trackers.containsKey(uuid) || activelengths==null) {
			return -1;	
		}
		Playing playing = trackers.get(uuid);
		return sectotime(activelengths.get(playing.currenttrack));
	}
	
	protected static short getPlayingRemain(UUID uuid) {
		if(!trackers.containsKey(uuid)) {
			return -1;	
		}
		Playing playing = trackers.get(uuid);
		return playing.remaining;
	}
	
	protected static short getPlayingRemainF(UUID uuid) {
		if(!trackers.containsKey(uuid)) {
			return -1;	
		}
		Playing playing = trackers.get(uuid);
		return playing.remainingf;
	}
	
	protected static void playMusic(Player player,String name) {
		UUID uuid = player.getUniqueId();
		PackInfo packinfo = ResourcePacked.getPackInfo(uuid);
		List<String> activeplaylist = packinfo.songs;
		List<Short> activelengths = packinfo.lengths;
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
		trackers.put(uuid,playing);
        player.playSound(player.getLocation(), "amusic.music".concat(Byte.toString(id)), 1.0E9f, 1.0f);
    }
	protected static void playMusic(UUID uuid,byte id) {
		if(uuid==null||id<0) {
			return;
		}
		Player player = Bukkit.getPlayer(uuid);
		if(player==null||!player.isOnline()) {
			return;
		}
		PackInfo packinfo = ResourcePacked.getPackInfo(uuid);
		List<String> activeplaylist = packinfo.songs;
		List<Short> activelengths = packinfo.lengths;
		if(activeplaylist==null||activelengths==null) {
			return;
		}
		stopMusic(player);
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
		private short remaining,remainingf;
		
		private Playing(byte currenttrack, short remaining) {
			this.currenttrack = currenttrack;
			this.remaining = remaining;
			this.remainingf = sectotime(remaining);
		}
	}
}
