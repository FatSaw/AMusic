package me.bomb.amusic;

import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import me.bomb.amusic.packedinfo.SoundInfo;

public final class PositionTracker extends Thread {

	private final ConcurrentHashMap<UUID, Playing> trackers = new ConcurrentHashMap<UUID, Playing>();
	private final ConcurrentHashMap<UUID, RepeatType> repeaters = new ConcurrentHashMap<UUID, RepeatType>();
	private final ConcurrentHashMap<UUID, SoundInfo[]> playlistinfo = new ConcurrentHashMap<UUID, SoundInfo[]>();
	private final ConcurrentHashMap<UUID, String> loadedplaylistnames = new ConcurrentHashMap<UUID, String>();
	private final SoundStarter soundstarter;
	private final SoundStopper soundstopper;
	
	public void setPlaylistInfo(UUID playeruuid, String playlistname, SoundInfo[] soundinfo) {
		playlistinfo.put(playeruuid, soundinfo);
		loadedplaylistnames.put(playeruuid, playlistname);
	}
	
	public void removePlaylistInfo(UUID playeruuid) {
		playlistinfo.remove(playeruuid);
		loadedplaylistnames.remove(playeruuid);
	}
	
	public HashSet<UUID> getPlayersLoaded(String playlistname) {
		HashSet<UUID> playersloaded = new HashSet<UUID>();
		for(UUID playeruuid : loadedplaylistnames.keySet(playlistname)) {
			playersloaded.add(playeruuid);
		}
		return playersloaded;
	}

	public String getPlaylistName(UUID playeruuid) {
		return playeruuid == null ? null : loadedplaylistnames.get(playeruuid);
	}

	public SoundInfo[] getSoundInfo(UUID playeruuid) {
		return playeruuid == null ? null : playlistinfo.get(playeruuid);
	}

	private boolean run = false;

	protected PositionTracker(SoundStarter soundstarter, SoundStopper soundstopper) {
		this.soundstarter = soundstarter;
		this.soundstopper = soundstopper;
	}

	@Override
	public void start() {
		run = true;
		super.start();
	}

	protected void end() {
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
						if (--playing.remaining < 0) {
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

	public String getPlaying(UUID uuid) {
		if (!trackers.containsKey(uuid)) {
			return null;
		}
		SoundInfo[] soundsinfo = getSoundInfo(uuid);
		if (soundsinfo == null) {
			return null;
		}
		Playing playing = trackers.get(uuid);

		if (playing.currenttrack < soundsinfo.length) {
			return soundsinfo[playing.currenttrack].name;
		}
		return null;
	}

	public short getPlayingSize(UUID uuid) {
		SoundInfo[] soundsinfo = getSoundInfo(uuid);
		if (!trackers.containsKey(uuid) || soundsinfo == null) {
			return -1;
		}
		Playing playing = trackers.get(uuid);
		return soundsinfo[playing.currenttrack].length;
	}

	public short getPlayingRemain(UUID uuid) {
		if (!trackers.containsKey(uuid)) {
			return -1;
		}
		Playing playing = trackers.get(uuid);
		return playing.remaining;
	}
	
	public void playMusicUntrackable(UUID uuid, String name) {
		SoundInfo[] soundsinfo = getSoundInfo(uuid);
		if (soundsinfo == null) {
			return;
		}
		short soundssize = (short) soundsinfo.length, id = soundssize;
		while (--id > -1) {
			if (soundsinfo[id].name.equals(name))
				break;
		}
		if (id == -1) {
			return;
		}
		if(trackers.containsKey(uuid)) {
			soundstopper.stopSound(uuid, trackers.remove(uuid).currenttrack);
		}

		soundstarter.startSound(uuid, id);
	}
	
	public void stopMusicUntrackable(UUID uuid) {
		SoundInfo[] soundsinfo = getSoundInfo(uuid);
		if (soundsinfo == null) {
			return;
		}
		short soundssize = (short) soundsinfo.length, id = soundssize;
		trackers.remove(uuid);
		while (--id > -1) {
			soundstopper.stopSound(uuid, id);
		}
	}

	public void playMusic(UUID uuid, String name) {
		SoundInfo[] soundsinfo = getSoundInfo(uuid);
		if (soundsinfo == null) {
			return;
		}
		short soundssize = (short) soundsinfo.length, id = soundssize;
		SoundInfo soundinfo = null;
		while (--id > -1) {
			if ((soundinfo = soundsinfo[id]).name.equals(name))
				break;
		}
		if (id == -1) {
			return;
		}
		if(trackers.containsKey(uuid)) {
			soundstopper.stopSound(uuid, trackers.get(uuid).currenttrack);
		}
		Playing playing = new Playing(id, soundssize, soundinfo.length);
		trackers.put(uuid, playing);

		soundstarter.startSound(uuid, id);
	}

	public void playMusic(UUID uuid, short id) {
		if (uuid == null || id < 0) {
			return;
		}
		SoundInfo[] soundsinfo = getSoundInfo(uuid);
		if (soundsinfo == null) {
			return;
		}
		short soundssize = (short) soundsinfo.length;
		if (id >= soundssize) {
			return;
		}
		SoundInfo soundinfo = soundsinfo[id];
		if(trackers.containsKey(uuid)) {
			soundstopper.stopSound(uuid, trackers.get(uuid).currenttrack);
		}

		Playing playing = new Playing(id, soundssize, soundinfo.length);

		trackers.put(uuid, playing);
		soundstarter.startSound(uuid, id);
	}

	public void stopMusic(UUID uuid) {
		Playing playing = trackers.remove(uuid);
		try {
			if (playing == null) {
				return;
			}
			soundstopper.stopSound(uuid, playing.currenttrack);
		} catch (NoSuchMethodError e) {
		}
	}
	
	/**
	 * Removes player from {@link PositionTracker#trackers}, {@link PositionTracker#playlistinfo}, {@link PositionTracker#repeaters}, {@link PositionTracker#loadedplaylistnames},.
	 */
	public void remove(UUID uuid) {
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
}
