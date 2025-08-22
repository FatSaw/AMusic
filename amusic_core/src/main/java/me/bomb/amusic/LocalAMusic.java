package me.bomb.amusic;

import java.net.InetAddress;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

import me.bomb.amusic.packedinfo.Data;
import me.bomb.amusic.packedinfo.SoundInfo;
import me.bomb.amusic.resource.ResourceDispatcher;
import me.bomb.amusic.resource.ResourceFactory;
import me.bomb.amusic.resource.StatusReport;
import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.source.PackSource;
import me.bomb.amusic.source.SoundSource;
import me.bomb.amusic.uploader.UploadManager;

public class LocalAMusic implements AMusic {
	public final SoundSource soundsource;
	public final PackSource packsource;
	public final PositionTracker positiontracker;
	public final ResourceManager resourcemanager;
	public final ResourceDispatcher dispatcher;
	public final Data datamanager;
	public final UploadManager uploadermanager;
	
	public LocalAMusic(Configuration config, SoundSource soundsource, PackSource packsource, PackSender packsender, SoundStarter soundstarter, SoundStopper soundstopper, Collection<InetAddress> playerips) {
		this.soundsource = soundsource;
		this.packsource = packsource;
		this.resourcemanager = new ResourceManager(config.packsizelimit, config.servercache, config.clientcache ? config.tokensalt : null, config.waitacception, config.sendpackstrictaccess ? playerips : null, config.sendpackifip, config.sendpackport, config.sendpackbacklog, config.sendpacktimeout, config.sendpackserverfactory, (short) 2);
		this.positiontracker = new PositionTracker(soundstarter, soundstopper);
		this.dispatcher = new ResourceDispatcher(packsender, resourcemanager, positiontracker, config.sendpackhost);
		this.datamanager = Data.getDefault(config.packeddir, !config.processpack);
		this.uploadermanager = config.uploaduse ? new UploadManager(config.uploadlifetime, config.uploadlimitsize, config.uploadlimitcount, config.musicdir, config.uploadstrictaccess ? playerips : null, config.uploadifip, config.uploadport, config.uploadbacklog, config.uploadtimeout, config.uploadserverfactory, (short) 2) : null;
	}
	
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
		r.run();
		return false;
	}
	
	public final boolean getPlaylists(boolean packed, boolean useCache, Consumer<String[]> resultConsumer) {
		Runnable r = new Runnable() {
			public void run() {
				resultConsumer.accept(packed ? datamanager.getPlaylists() : soundsource.getPlaylists());
			}
		};
		r.run();
		return false;
	}
	
	public final boolean getPlaylistSoundnames(String playlistname, boolean packed, boolean useCache, Consumer<String[]> resultConsumer) {
		if(playlistname == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				if(packed) {
					SoundInfo[] soundinfos = datamanager.getPlaylist(playlistname).sounds;
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
		r.run();
		return false;
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
		r.run();
		return false;
	}
	
	public final boolean getPlaylistSoundlengths(String playlistname, boolean useCache, Consumer<short[]> resultConsumer) {
		if(playlistname == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				SoundInfo[] soundinfos = datamanager.getPlaylist(playlistname).sounds;
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
		r.run();
		return false;
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
		r.run();
		return false;
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
		r.run();
		return false;
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
		r.run();
		return false;
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
		r.run();
		return false;
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
		r.run();
		return false;
	}
	
	public final boolean loadPack(UUID[] playeruuid, String name, boolean update, StatusReport statusreport) {
		Runnable r = new Runnable() {
			public void run() {
				new ResourceFactory(name, playeruuid, datamanager, dispatcher, soundsource, packsource, update, statusreport, true);
			}
		};
		r.run();
		return false;
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
		r.run();
		return false;
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
		r.run();
		return false;
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
		r.run();
		return false;
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
		r.run();
		return false;
	}
	
	public final boolean playSoundUntrackable(UUID playeruuid, String name) {
		if(playeruuid == null || name == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				positiontracker.playMusicUntrackable(playeruuid, name);
			}
		};
		r.run();
		return false;
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
		r.run();
		return false;
	}
	
	public final boolean getUploadSessions(Consumer<UUID[]> resultConsumer) {
		Runnable r = new Runnable() {
			public void run() {
				resultConsumer.accept(uploadermanager == null ? null : uploadermanager.getSessions());
			}
		};
		r.run();
		return false;
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
		r.run();
		return false;
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
