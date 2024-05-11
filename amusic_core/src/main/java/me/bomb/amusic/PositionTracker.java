package me.bomb.amusic;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PositionTracker extends Thread {

	private final ConcurrentHashMap<UUID, Playing> trackers = new ConcurrentHashMap<UUID, Playing>();
	private final ConcurrentHashMap<UUID, RepeatType> repeaters = new ConcurrentHashMap<UUID, RepeatType>();
	private final ConcurrentHashMap<UUID, ArrayList<SoundInfo>> playlistinfo = new ConcurrentHashMap<UUID, ArrayList<SoundInfo>>();
	private final ConcurrentHashMap<UUID, String> loadedplaylistnames = new ConcurrentHashMap<UUID, String>();
	private final SoundStarter soundstarter;
	private final SoundStopper soundstopper;
	private final boolean processformatedtime;
	
	public void setPlaylistInfo(UUID playeruuid, String playlistname, ArrayList<SoundInfo> soundinfo) {
		playlistinfo.put(playeruuid, soundinfo);
		loadedplaylistnames.put(playeruuid, playlistname);
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

	public ArrayList<SoundInfo> getSoundInfo(UUID playeruuid) {
		return playeruuid == null ? null : playlistinfo.get(playeruuid);
	}

	private boolean run = false;

	public PositionTracker(SoundStarter soundstarter, SoundStopper soundstopper, boolean processformatedtime) {
		this.soundstarter = soundstarter;
		this.soundstopper = soundstopper;
		this.processformatedtime = processformatedtime;
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
						playing.remainingf = playing.sectotime(--playing.remaining);
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
		return playing.sectotime(soundsinfo.get(playing.currenttrack).length);
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

	public void playMusic(UUID uuid, String name) {
		List<SoundInfo> soundsinfo = getSoundInfo(uuid);
		if (soundsinfo == null) {
			return;
		}
		short soundssize = (short) soundsinfo.size(), id = soundssize;
		SoundInfo soundinfo = null;
		while (--id > -1) {
			if ((soundinfo = soundsinfo.get(id)).name.equals(name))
				break;
		}
		if (id == -1) {
			return;
		}
		if(trackers.containsKey(uuid)) {
			soundstopper.stopSound(uuid, trackers.get(uuid).currenttrack);
		}
		Playing playing = new Playing(id, soundssize, soundinfo.length, processformatedtime);
		trackers.put(uuid, playing);

		soundstarter.startSound(uuid, id);
	}

	public void playMusic(UUID uuid, short id) {
		if (uuid == null || id < 0) {
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
		if(trackers.containsKey(uuid)) {
			soundstopper.stopSound(uuid, trackers.get(uuid).currenttrack);
		}

		Playing playing = new Playing(id, soundssize, soundinfo.length, processformatedtime);

		trackers.put(uuid, playing);
		soundstarter.startSound(uuid, id);
	}

	public void stopMusic(UUID uuid) {
		Playing playing = trackers.remove(uuid);
		try {
			if (playing == null) {
				for (byte i = 0; i != -128; ++i) {
					soundstopper.stopSound(uuid, i);
				}
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
