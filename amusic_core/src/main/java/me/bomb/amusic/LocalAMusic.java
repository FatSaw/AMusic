package me.bomb.amusic;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import me.bomb.amusic.packedinfo.Data;
import me.bomb.amusic.packedinfo.SoundInfo;
import me.bomb.amusic.packedinfo.SoundSource;
import me.bomb.amusic.packedinfo.SourceEntry;
import me.bomb.amusic.resource.ResourceFactory;
import me.bomb.amusic.resource.StatusReport;
import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.uploader.UploadManager;
import me.bomb.amusic.util.Logger;

public class LocalAMusic implements AMusic {
	
	public final Logger logger;
	public final SoundSource<? extends SourceEntry> soundsource;
	public final PositionTracker positiontracker;
	public final ResourceManager resourcemanager;
	public final Data datamanager;
	public final UploadManager uploadermanager;
	private final Executor executor;
	
	public LocalAMusic(Logger logger, Executor executor, SoundSource<? extends SourceEntry> soundsource, PositionTracker positiontracker, ResourceManager resourcemanager, Data datamanager, UploadManager uploadermanager) {
		this.logger = logger;
		this.soundsource = soundsource;
		this.positiontracker = positiontracker;
		this.resourcemanager = resourcemanager;
		this.datamanager = datamanager;
		this.uploadermanager = uploadermanager;
		this.executor = executor;
	}
	
	/*public LocalAMusic(Logger logger, Configuration config, SoundSource<?> soundsource, PackSender packsender, SoundStarter soundstarter, SoundStopper soundstopper, Collection<InetAddress> playerips) {
		this.logger = logger;
		this.soundsource = soundsource;
		this.positiontracker = new PositionTracker(soundstarter, soundstopper);
		this.resourcemanager = new ResourceManager(packsender, this.positiontracker, config.sendpackhost, config.packsizelimit, config.clientcache ? config.tokensalt : null, config.waitacception, config.sendpackstrictaccess ? playerips : null, config.sendpackifip, config.sendpackport, config.sendpackbacklog, config.sendpacktimeout, config.sendpackserverfactory, (short) 2, config.sendpackexecutorchecker, config.sendpackexecutorsender);
		this.datamanager = Data.getNoStorage(!config.processpack, soundsource);
		this.uploadermanager = config.uploaduse ? new UploadManager(config.uploadlifetime, config.uploadlimitsize, config.uploadlimitcount, config.musicdir, config.uploadstrictaccess ? playerips : null, config.uploadifip, config.uploadport, config.uploadbacklog, config.uploadtimeout, config.uploadserverfactory, (short) 2) : null;
		this.executor = config.executor;
	}*/
	
	public void enable() {
		positiontracker.start();
		resourcemanager.start();
		datamanager.start();
		if(this.uploadermanager != null) uploadermanager.start();
		datamanager.load();
	}
	
	public void disable() {
		positiontracker.end();
		resourcemanager.end();
		datamanager.end();
		if(this.uploadermanager != null) uploadermanager.end();
	}
	
	public void logout(UUID playeruuid) {
		positiontracker.remove(playeruuid);
		resourcemanager.remove(playeruuid);
	}
	
	@Override
	public final boolean getPlayersLoaded(String playlistname, Consumer<UUID[]> resultConsumer) {
		if(playlistname == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				resultConsumer.accept(positiontracker.getPlayersLoaded(playlistname));
			}
		};
		executor.execute(r);
		return true;
	}
	
	public final boolean getPlaylists(boolean packed, boolean useCache, Consumer<String[]> resultConsumer) {
		Runnable r = new Runnable() {
			public void run() {
				resultConsumer.accept(packed ? datamanager.getPlaylists() : soundsource.getPlaylists());
			}
		};
		executor.execute(r);
		return true;
	}
	
	public final boolean getPlaylistSoundnames(String playlistname, boolean packed, boolean useCache, Consumer<String[]> resultConsumer) {
		if(playlistname == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				if(packed) {
					SoundInfo[] soundinfos = datamanager.getPlaylist(playlistname).info.sounds;
					if(soundinfos==null) {
						resultConsumer.accept(null);
						return;
					}
					int i = soundinfos.length;
					String[] soundnames = new String[i];
					while(--i > -1) {
						soundnames[i] = soundinfos[i].name;
					}

					resultConsumer.accept(soundnames);
				} else {
					resultConsumer.accept(soundsource.getSounds(playlistname));
				}
			}
		};
		executor.execute(r);
		return true;
	}
	
	public final boolean getPlaylistSoundnames(UUID playeruuid, boolean useCache, Consumer<String[]> resultConsumer) {
		if(playeruuid == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				SoundInfo[] soundinfos = positiontracker.getSoundInfo(playeruuid);
				if(soundinfos==null) {
					resultConsumer.accept(null);
					return;
				}
				int i = soundinfos.length;
				String[] soundnames = new String[i];
				while(--i > -1) {
					soundnames[i] = soundinfos[i].name;
				}
				resultConsumer.accept(soundnames);
			}
		};
		executor.execute(r);
		return true;
	}
	
	public final boolean getPlaylistSoundlengths(String playlistname, boolean useCache, Consumer<short[]> resultConsumer) {
		if(playlistname == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				SoundInfo[] soundinfos = datamanager.getPlaylist(playlistname).info.sounds;
				if(soundinfos==null) {
					resultConsumer.accept(null);
					return;
				}
				int i = soundinfos.length;
				short[] soundlengths = new short[i];
				while(--i > -1) {
					soundlengths[i] = soundinfos[i].length;
				}
				resultConsumer.accept(soundlengths);
			}
		};
		executor.execute(r);
		return true;
	}
	
	public final boolean getPlaylistSoundlengths(UUID playeruuid, boolean useCache, Consumer<short[]> resultConsumer) {
		if(playeruuid == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				SoundInfo[] soundinfos = positiontracker.getSoundInfo(playeruuid);
				if(soundinfos==null) {
					resultConsumer.accept(null);
					return;
				}
				int i = soundinfos.length;
				short[] soundlengths = new short[i];
				while(--i > -1) {
					soundlengths[i] = soundinfos[i].length;
				}
				resultConsumer.accept(soundlengths);
			}
		};
		executor.execute(r);
		return true;
	}
	
	public final boolean setRepeatMode(UUID playeruuid, RepeatType repeattype) {
		if(playeruuid == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				positiontracker.setRepeater(playeruuid, repeattype);
			}
		};
		executor.execute(r);
		return true;
	}
	
	public final boolean getPlayingSoundName(UUID playeruuid, Consumer<String> resultConsumer) {
		if(playeruuid == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				resultConsumer.accept(positiontracker.getPlaying(playeruuid));
			}
		};
		executor.execute(r);
		return true;
	}
	
	public final boolean getPlayingSoundSize(UUID playeruuid, Consumer<Short> resultConsumer) {
		if(playeruuid == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				resultConsumer.accept(positiontracker.getPlayingSize(playeruuid));
			}
		};
		executor.execute(r);
		return true;
	}
	
	public final boolean getPlayingSoundRemain(UUID playeruuid, Consumer<Short> resultConsumer) {
		if(playeruuid == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				resultConsumer.accept(positiontracker.getPlayingRemain(playeruuid));
			}
		};
		executor.execute(r);
		return true;
	}
	
	public final boolean loadPack(UUID[] playeruuid, String name, boolean update, StatusReport statusreport) {
		Runnable r = new ResourceFactory(name, playeruuid, datamanager, resourcemanager, update, statusreport);
		executor.execute(r);
		return true;
	}
	
	public final boolean getPackName(UUID playeruuid, Consumer<String> resultConsumer) {
		if(playeruuid == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				resultConsumer.accept(positiontracker.getPlaylistName(playeruuid));
			}
		};
		executor.execute(r);
		return true;
	}
	
	public final boolean stopSound(UUID playeruuid) {
		if(playeruuid == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				positiontracker.stopMusic(playeruuid);
			}
		};
		executor.execute(r);
		return true;
	}
	
	public final boolean stopSoundUntrackable(UUID playeruuid) {
		if(playeruuid == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				positiontracker.stopMusicUntrackable(playeruuid);
			}
		};
		executor.execute(r);
		return true;
	}
	
	public final boolean playSound(UUID playeruuid, String name) {
		if(playeruuid == null || name == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				positiontracker.playMusic(playeruuid, name);
			}
		};
		executor.execute(r);
		return true;
	}
	
	public final boolean playSoundUntrackable(UUID playeruuid, String name, double x, double y, double z, float volume, float pitch) {
		if(playeruuid == null || name == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				positiontracker.playMusicUntrackable(playeruuid, name, x, y, z, volume, pitch);
			}
		};
		executor.execute(r);
		return true;
	}
	
	public final boolean openUploadSession(String playlistname, Consumer<UUID> resultConsumer) {
		if(playlistname == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				resultConsumer.accept(uploadermanager == null ? null : uploadermanager.startSession(playlistname));
			}
		};
		executor.execute(r);
		return true;
	}
	
	public final boolean getUploadSessions(Consumer<UUID[]> resultConsumer) {
		Runnable r = new Runnable() {
			public void run() {
				resultConsumer.accept(uploadermanager == null ? null : uploadermanager.getSessions());
			}
		};
		executor.execute(r);
		return true;
	}
	
	public final boolean closeUploadSession(UUID token, boolean save, Consumer<Boolean> resultConsumer) {
		if(token == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				resultConsumer.accept(uploadermanager == null ? false : uploadermanager.endSession(token, save));
			}
		};
		executor.execute(r);
		return true;
	}
	
	public final void closeUploadSession(UUID token, boolean save) {
		if(token == null) {
			return;
		}
		Runnable r = new Runnable() {
			public void run() {
				if(uploadermanager ==null) {
					return;
				}
				uploadermanager.endSession(token, save);
			}
		};
		r.run();
	}
	
}
