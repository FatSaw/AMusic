package me.bomb.amusic;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.bomb.amusic.LangOptions.Placeholders;
import me.bomb.amusic.legacy.LegacySoundStopper;

public final class PositionTracker extends Thread {

	private final ConcurrentHashMap<UUID, Playing> trackers = new ConcurrentHashMap<UUID, Playing>();
	private final ConcurrentHashMap<UUID, RepeatType> repeaters = new ConcurrentHashMap<UUID, RepeatType>();
	private final ConcurrentHashMap<UUID, ArrayList<SoundInfo>> playlistinfo = new ConcurrentHashMap<UUID, ArrayList<SoundInfo>>();
	private final ConcurrentHashMap<UUID, String> loadedplaylistnames = new ConcurrentHashMap<UUID, String>();
	
	protected void setPlaylistInfo(UUID playeruuid, String playlistname, ArrayList<SoundInfo> soundinfo) {
		playlistinfo.put(playeruuid, soundinfo);
		loadedplaylistnames.put(playeruuid, playlistname);
	}

	public String getPlaylistName(UUID playeruuid) {
		return playeruuid == null ? null : loadedplaylistnames.get(playeruuid);
	}

	public ArrayList<SoundInfo> getSoundInfo(UUID playeruuid) {
		return playeruuid == null ? null : playlistinfo.get(playeruuid);
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
		long overtime = 0;
		long time = System.currentTimeMillis();
		while (run) {
			int trackerssize = 0;
			trackerssize = trackers.size();
			if (trackerssize > 0) {
				int sleept = 500;
				if (500 < overtime) {
					sleept = 0;
				} else if(overtime>0) {
					sleept -= overtime / trackerssize;
				}
				sleept /= trackerssize;
				if (sleept < 0) {
					sleept = 0;
				}
				Iterator<Entry<UUID, Playing>> entrysiterator = trackers.entrySet().iterator();
				while(entrysiterator.hasNext()) {
					try {
						Entry<UUID, Playing> entry = entrysiterator.next();
						UUID uuid = entry.getKey();
						Playing playing = entry.getValue();
						playing.remainingf = sectotime(--playing.remaining);
						if (playing.remaining < 0) {
							entrysiterator.remove();
							RepeatType repeattype;
							repeattype = repeaters.get(uuid);
							if (repeattype != null) {
								playMusic(uuid, repeattype.next(playing.currenttrack, playing.maxid));
							}
						}
						if (sleept > 0) {
							try {
								Thread.sleep(sleept);
							} catch (InterruptedException e) {
							}
						}
					} catch (ConcurrentModificationException e) {
						e.printStackTrace();
					}
				}
			}
			
			long curtime = System.currentTimeMillis(),timedif = curtime;
			timedif -= time;
			time = curtime;
			if (timedif < 0)
				timedif = 0;
			if (timedif > 1000) {
				overtime += timedif - 1000;
			} else {
				short sleep = 1000;
				sleep -= (short) timedif;
				if (overtime > 1000) {
					overtime -= sleep;
				} else {
					overtime -= sleep;
					if(overtime<0) {
						overtime=0;
					}
					sleep -= overtime;
					if (sleep > 0) {
						try {
							Thread.sleep(sleep);
							time+=sleep;
						} catch (InterruptedException e) {
						}
					}
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

	public String getPlaying(UUID uuid) {
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

	public short getPlayingSize(UUID uuid) {
		List<SoundInfo> soundsinfo = getSoundInfo(uuid);
		if (!trackers.containsKey(uuid) || soundsinfo == null) {
			return -1;
		}
		Playing playing = trackers.get(uuid);
		return soundsinfo.get(playing.currenttrack).length;
	}

	public short getPlayingSizeF(UUID uuid) {
		List<SoundInfo> soundsinfo = getSoundInfo(uuid);
		if (!trackers.containsKey(uuid) || soundsinfo == null) {
			return -1;
		}
		Playing playing = trackers.get(uuid);
		return sectotime(soundsinfo.get(playing.currenttrack).length);
	}

	public short getPlayingRemain(UUID uuid) {
		if (!trackers.containsKey(uuid)) {
			return -1;
		}
		Playing playing = trackers.get(uuid);
		return playing.remaining;
	}

	public short getPlayingRemainF(UUID uuid) {
		if (!trackers.containsKey(uuid)) {
			return -1;
		}
		Playing playing = trackers.get(uuid);
		return playing.remainingf;
	}

	public void playMusic(Player player, String name) {
		UUID uuid = player.getUniqueId();
		List<SoundInfo> soundsinfo = getSoundInfo(uuid);
		if (soundsinfo == null) {
			return;
		}
		byte soundssize = (byte) soundsinfo.size(), id = soundssize;
		SoundInfo soundinfo = null;
		while (--id > -1) {
			if ((soundinfo = soundsinfo.get(id)).name.equals(name))
				break;
		}
		if (id == -1) {
			return;
		}
		if(!ConfigOptions.legacystopper || trackers.containsKey(uuid)) {
			stopMusic(player);
		}
		LangOptions.playmusic_playing.sendMsgActionbar(player, new Placeholders("%soundname%", name));
		Playing playing = new Playing(id, soundssize, soundinfo.length);
		trackers.put(uuid, playing);
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
		if (id >= soundssize) {
			return;
		}
		SoundInfo soundinfo = soundsinfo.get(id);
		if(!ConfigOptions.legacystopper) {
			stopMusic(player);
		}
		LangOptions.playmusic_playing.sendMsgActionbar(player, new Placeholders("%soundname%", soundinfo.name));

		Playing playing = new Playing(id, soundssize, soundinfo.length);

		trackers.put(player.getUniqueId(), playing);

		player.playSound(player.getLocation(), "amusic.music".concat(Byte.toString(id)), 1.0E9f, 1.0f);
	}

	public void stopMusic(Player player) {
		Playing playing = trackers.remove(player.getUniqueId());
		if(ConfigOptions.legacystopper) {
			LegacySoundStopper.stopSounds(player);
			return;
		}
		try {
			if (playing == null) {
				for (byte i = 0; i != -128; ++i) {
					player.stopSound("amusic.music".concat(Byte.toString(i)));
				}
				return;
			}
			player.stopSound("amusic.music".concat(Byte.toString(playing.currenttrack)));
		} catch (NoSuchMethodError e) {
		}
	}

	protected void remove(UUID uuid) {
		trackers.remove(uuid);
		playlistinfo.remove(uuid);
		repeaters.remove(uuid);
		loadedplaylistnames.remove(uuid);
	}

	public void setRepeater(UUID uuid, RepeatType repeattype) {
		if (repeattype == null) {
			repeaters.remove(uuid);
			return;
		}
		repeaters.put(uuid, repeattype);
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
