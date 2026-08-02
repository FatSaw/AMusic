package me.bomb.amusic;

import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import me.bomb.amusic.packedinfo.ResourcepackInfo;
import me.bomb.amusic.packedinfo.SoundInfo;

public final class PositionTracker implements Runnable {

	private final ConcurrentHashMap<UUID, Playing> trackers = new ConcurrentHashMap<UUID, Playing>();
	private final ConcurrentHashMap<UUID, RepeatType> repeaters = new ConcurrentHashMap<UUID, RepeatType>();
	private final ConcurrentHashMap<UUID, ResourcepackInfo> resourcepackinfo = new ConcurrentHashMap<UUID, ResourcepackInfo>();
	
	private final SoundStarter soundstarter;
	private final SoundStopper soundstopper;
	
	private Thread ticker;
	
	public void setPlaylistInfo(UUID playeruuid, ResourcepackInfo resourcepackinfo) {
		this.resourcepackinfo.put(playeruuid, resourcepackinfo);
	}
	
	public void removePlaylistInfo(UUID playeruuid) {
		this.resourcepackinfo.remove(playeruuid);
	}
	
	public UUID[] getPlayersLoaded(String playlistname) {
		if(playlistname == null) {
			return null;
		}
		HashSet<UUID> playerslist = new HashSet<UUID>(this.resourcepackinfo.size());
		for(Entry<UUID, ResourcepackInfo> entry : this.resourcepackinfo.entrySet()) {
			UUID playeruuid = entry.getKey();
			ResourcepackInfo info = entry.getValue();
			if(info.getPackname().equals(playlistname)) {
				playerslist.add(playeruuid);
			}
		}
		return playerslist.toArray(new UUID[playerslist.size()]);
	}
	
	public ResourcepackInfo getResourcepackInfo(UUID playeruuid) {
		if(playeruuid == null) {
			return null;
		}
		return this.resourcepackinfo.get(playeruuid);
	}

	private volatile boolean run = false;

	public PositionTracker(SoundStarter soundstarter, SoundStopper soundstopper) {
		this.soundstarter = soundstarter;
		this.soundstopper = soundstopper;
	}

	public void start() {
		this.run = true;
		this.ticker = new Thread(this);
		this.ticker.start();
	}

	protected void end() {
		this.run = false;
		Thread ticker = this.ticker;
		if(ticker != null) {
			ticker.interrupt();
			this.ticker = null;
		}
		this.trackers.clear();
		this.repeaters.clear();
		this.resourcepackinfo.clear();
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
							RepeatType repeattype = repeaters.get(uuid);
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
		ResourcepackInfo info;
		SoundInfo[] soundsinfo;
		if ((info = getResourcepackInfo(uuid)) == null || (soundsinfo = info.getSounds()) == null) {
			return null;
		}
		Playing playing = trackers.get(uuid);

		if (playing.currenttrack < soundsinfo.length) {
			return soundsinfo[playing.currenttrack].name;
		}
		return null;
	}

	public short getPlayingSize(UUID uuid) {
		ResourcepackInfo info;
		SoundInfo[] soundsinfo;
		if (!trackers.containsKey(uuid) || (info = getResourcepackInfo(uuid)) == null || (soundsinfo = info.getSounds()) == null) {
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
	
	public void playMusicUntrackable(UUID uuid, String name, double x, double y, double z, float volume, float pitch) {
		ResourcepackInfo info;
		SoundInfo[] soundsinfo;
		if ((info = getResourcepackInfo(uuid)) == null || (soundsinfo = info.getSounds()) == null) {
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
		
		final short fid = id;
		final UUID soundhash = soundinfo.hash;
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				Playing playing = trackers.remove(uuid);
				if(playing != null) {
					soundstopper.stopSound(uuid, playing.soundhash, playing.currenttrack, (byte) 0);
				}
				soundstarter.startSound(uuid, soundhash, fid, (byte) 0, x, y, z, volume, pitch);
			}
		};
		
		if(trackers.containsKey(uuid) && soundstopper.isLock()) {
			new Thread(runnable).start();
		} else {
			runnable.run();
		}
	}
	
	public void stopMusicUntrackable(UUID uuid) {
		ResourcepackInfo info;
		SoundInfo[] soundsinfo;
		if ((info = getResourcepackInfo(uuid)) == null || (soundsinfo = info.getSounds()) == null) {
			return;
		}
		trackers.remove(uuid);
		if(soundstopper.isStopAll()) {
			if(soundstopper.isLock()) {
				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						soundstopper.stopSound(uuid, null, (short)0, (byte) 0);
					}
				};
				new Thread(runnable).start();
				return;
			}
			soundstopper.stopSound(uuid, null, (short)0, (byte) 0);
			return;
		}
		short soundssize = (short) soundsinfo.length, id = soundssize;
		while (--id > -1) {
			soundstopper.stopSound(uuid, soundsinfo[id].hash, id, (byte) 0);
		}
	}

	public void playMusic(UUID uuid, String name) {
		ResourcepackInfo info;
		SoundInfo[] soundsinfo;
		if ((info = getResourcepackInfo(uuid)) == null || (soundsinfo = info.getSounds()) == null) {
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
		final short fid = id, soundlength = soundinfo.length;
		final UUID soundhash = soundinfo.hash;
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				Playing playing = trackers.put(uuid, new Playing(soundhash, fid, soundssize, soundlength));
				if(playing != null) {
					soundstopper.stopSound(uuid, playing.soundhash, playing.currenttrack, (byte) 0);
				}
				soundstarter.startSound(uuid, soundhash, fid, (byte) 0);
			}
		};
		
		if(trackers.containsKey(uuid) && soundstopper.isLock()) {
			new Thread(runnable).start();
		} else {
			runnable.run();
		}
	}

	public void playMusic(UUID uuid, short id) {
		if (uuid == null || id < 0) {
			return;
		}
		ResourcepackInfo info;
		SoundInfo[] soundsinfo;
		if ((info = getResourcepackInfo(uuid)) == null || (soundsinfo = info.getSounds()) == null) {
			return;
		}
		short soundssize = (short) soundsinfo.length;
		if (id >= soundssize) {
			return;
		}
		SoundInfo soundinfo = soundsinfo[id];
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				Playing playing = trackers.put(uuid, new Playing(soundinfo.hash, id, soundssize, soundinfo.length));
				if(playing != null) {
					soundstopper.stopSound(uuid, playing.soundhash, playing.currenttrack, (byte) 0);
				}
				soundstarter.startSound(uuid, soundinfo.hash, id, (byte) 0);
			}
		};
		if(trackers.containsKey(uuid) && soundstopper.isLock()) {
			new Thread(runnable).start();
		} else {
			runnable.run();
		}
	}

	public void stopMusic(UUID uuid) {
		Playing playing = trackers.remove(uuid);
		try {
			if (playing == null) {
				if(soundstopper.isStopAll() && soundstopper.isLock()) {
					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							soundstopper.stopSound(uuid, null, (short)0, (byte) 0);
						}
					};
					new Thread(runnable).start();
				}
				return;
			}
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					soundstopper.stopSound(uuid, playing.soundhash, playing.currenttrack, (byte) 0);
				}
			};
			if(soundstopper.isLock()) {
				new Thread(runnable).start();
			} else {
				runnable.run();
			}
		} catch (NoSuchMethodError e) {
		}
	}
	
	/**
	 * Removes player from {@link PositionTracker#trackers}, {@link PositionTracker#resourcepackinfo}, {@link PositionTracker#repeaters}, {@link PositionTracker#loadedplaylistnames},.
	 */
	public void remove(UUID uuid) {
		trackers.remove(uuid);
		resourcepackinfo.remove(uuid);
		repeaters.remove(uuid);
	}

	public void setRepeater(UUID uuid, RepeatType repeattype) {
		if (repeattype == null) {
			repeaters.remove(uuid);
			return;
		}
		repeaters.put(uuid, repeattype);
	}
}
