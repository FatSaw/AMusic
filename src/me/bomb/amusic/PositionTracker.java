package me.bomb.amusic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.bomb.amusic.LangOptions.Placeholders;

final class PositionTracker extends Thread {
	
	private final Map<UUID, Playing> trackers = new HashMap<UUID, Playing>();
	private final HashMap<UUID, RepeatType> repeaters = new HashMap<UUID, RepeatType>();
	private final HashMap<UUID, ArrayList<SoundInfo>> playlistinfo = new HashMap<UUID, ArrayList<SoundInfo>>();
	private final HashMap<UUID, String> loadedplaylistnames = new HashMap<UUID, String>();
	
	protected void setPlaylistInfo(UUID playeruuid, String playlistname, ArrayList<SoundInfo> soundinfo) {
		synchronized(playlistinfo) {
			playlistinfo.put(playeruuid, soundinfo);
		}
		synchronized (loadedplaylistnames) {
			loadedplaylistnames.put(playeruuid, playlistname);
		}
	}
	
	protected String getPlaylistName(UUID playeruuid) {
		synchronized (loadedplaylistnames) {
			return playeruuid == null ? null : loadedplaylistnames.get(playeruuid);
		}
	}
	
	protected ArrayList<SoundInfo> getSoundInfo(UUID playeruuid) {
		synchronized(playlistinfo) {
			return playeruuid == null ? null : playlistinfo.get(playeruuid);
		}
	}
	
	private boolean run = false;
	protected PositionTracker() {
		start();
	}
	
	@Override
	public void start() {
		run = true;
		super.start();
	}
	
	public void end() {
		run = false;
	}
	
	@Override
	public void run() {
		while (run) {
			long time = System.currentTimeMillis();
			synchronized(trackers) {
				for (UUID uuid : trackers.keySet()) {
					Playing playing = trackers.get(uuid);
					playing.remainingf = sectotime(--playing.remaining);
					if (playing.remaining < 0) {
						trackers.remove(uuid);
						RepeatType repeattype;
						synchronized (repeaters) {
							repeattype = repeaters.get(uuid);
						}
						if(repeattype==null) {
							continue;
						}
						playMusic(uuid, repeattype.next(playing.currenttrack, playing.maxid));
					}
				}
			}
			long timedif = System.currentTimeMillis();
			timedif-=time;
			if (timedif < 0) timedif = 0;
			if (timedif > 1000) timedif = 1000;
			short sleep = 1000;
			sleep -= (short) timedif;
			if(sleep>0) {
				try {
					Thread.sleep(sleep);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	private static short sectotime(short sec) {
		if (!ConfigOptions.hasplaceholderapi || sec < 1)
			return 0;
		if (sec > 0x7080) {
			return 0x7EFB;
		}
		short res = 0;
		byte min = 0, hour = 0;
		while ((res += 60) < sec) {
			if (++min < 60) {
				continue;
			}
			min = 0;
			++hour;
		}
		res -= 59;
		sec -= res;
		sec |= ((short) min << 6);
		sec |= ((short) hour << 6);
		return sec;
	}

	protected String getPlaying(UUID uuid) {
		synchronized(trackers) {
			if (!trackers.containsKey(uuid)) {
				return null;
			}
			List<SoundInfo> soundsinfo = getSoundInfo(uuid);
			if (soundsinfo == null) {
				return null;
			}
			Playing playing = trackers.get(uuid);

			if (playing.currenttrack < soundsinfo.size()) {
				return soundsinfo.get(playing.currenttrack).name;
			}
			return null;
		}
	}

	protected short getPlayingSize(UUID uuid) {
		List<SoundInfo> soundsinfo = getSoundInfo(uuid);
		synchronized(trackers) {
			if (!trackers.containsKey(uuid) || soundsinfo == null) {
				return -1;
			}
			Playing playing = trackers.get(uuid);
			return soundsinfo.get(playing.currenttrack).length;
		}
	}

	protected short getPlayingSizeF(UUID uuid) {
		List<SoundInfo> soundsinfo = getSoundInfo(uuid);
		synchronized(trackers) {
			if (!trackers.containsKey(uuid) || soundsinfo == null) {
				return -1;
			}
			Playing playing = trackers.get(uuid);
			return sectotime(soundsinfo.get(playing.currenttrack).length);
		}
	}

	protected short getPlayingRemain(UUID uuid) {
		synchronized(trackers) {
			if (!trackers.containsKey(uuid)) {
				return -1;
			}
			Playing playing = trackers.get(uuid);
			return playing.remaining;
		}
	}

	protected short getPlayingRemainF(UUID uuid) {
		synchronized(trackers) {
			if (!trackers.containsKey(uuid)) {
				return -1;
			}
			Playing playing = trackers.get(uuid);
			return playing.remainingf;
		}
	}

	protected void playMusic(Player player, String name) {
		UUID uuid = player.getUniqueId();
		List<SoundInfo> soundsinfo = getSoundInfo(uuid);
		if (soundsinfo == null) {
			return;
		}
		byte soundssize = (byte) soundsinfo.size(),id = soundssize;
		SoundInfo soundinfo = null;
		while (--id>-1) {
			if((soundinfo = soundsinfo.get(id)).name.equals(name)) break;
		}
		if (id == -1) {
			return;
		}
		stopMusic(player);
		LangOptions.playmusic_playing.sendMsgActionbar(player, new Placeholders("%soundname%", name));
		Playing playing = new Playing(id, soundssize, soundinfo.length);
		synchronized(trackers) {
			trackers.put(uuid, playing);
		}
		player.playSound(player.getLocation(), "amusic.music".concat(Byte.toString(id)), 1.0E9f, 1.0f);
	}

	private void playMusic(UUID uuid, byte id) {
		if (uuid == null || id < 0) {
			return;
		}
		Player player = Bukkit.getPlayer(uuid);
		if (player == null || !player.isOnline()) {
			return;
		}
		List<SoundInfo> soundsinfo = getSoundInfo(uuid);
		if (soundsinfo == null) {
			return;
		}
		byte soundssize = (byte) soundsinfo.size();
		if(id>=soundssize) {
			return;
		}
		SoundInfo soundinfo = soundsinfo.get(id);
		stopMusic(player);
		LangOptions.playmusic_playing.sendMsgActionbar(player, new Placeholders("%soundname%", soundinfo.name));

		Playing playing = new Playing(id, soundssize, soundinfo.length);
		
		trackers.put(player.getUniqueId(), playing);
		
		player.playSound(player.getLocation(), "amusic.music".concat(Byte.toString(id)), 1.0E9f, 1.0f);
	}

	protected void stopMusic(Player player) {
		synchronized(trackers) {
			Playing playing = trackers.remove(player.getUniqueId());
			if (playing == null) {
				for (byte i = 0; i != -128; ++i) {
					player.stopSound("amusic.music".concat(Byte.toString(i)));
				}
				return;
			}
			player.stopSound("amusic.music".concat(Byte.toString(playing.currenttrack)));
		}
	}

	protected void remove(UUID uuid) {
		synchronized(trackers) {
			trackers.remove(uuid);
		}
		synchronized(playlistinfo) {
			playlistinfo.remove(uuid);
		}
		synchronized(repeaters) {
			repeaters.remove(uuid);
		}
		synchronized(loadedplaylistnames) {
			loadedplaylistnames.remove(uuid);
		}
	}
	
	protected void setRepeater(UUID uuid, RepeatType repeattype) {
		synchronized (repeaters) {
			if (repeattype == null) {
				repeaters.remove(uuid);
				return;
			}
			repeaters.put(uuid, repeattype);
		}
	}

	private static class Playing {
		private final byte currenttrack, maxid;
		private short remaining, remainingf;

		private Playing(byte currenttrack, byte maxid, short remaining) {
			this.currenttrack = currenttrack;
			this.maxid = maxid;
			this.remaining = remaining;
			this.remainingf = sectotime(remaining);
		}
	}
}
