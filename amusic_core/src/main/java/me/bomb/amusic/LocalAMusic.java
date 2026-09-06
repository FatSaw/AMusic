package me.bomb.amusic;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import me.bomb.amusic.packedinfo.CustomDatastore;
import me.bomb.amusic.packedinfo.Data;
import me.bomb.amusic.packedinfo.DataEntry;
import me.bomb.amusic.packedinfo.ResourcepackInfo;
import me.bomb.amusic.packedinfo.SoundInfo;
import me.bomb.amusic.packedinfo.SoundSource;
import me.bomb.amusic.packedinfo.SourceEntry;
import me.bomb.amusic.resource.ResourceFactory;
import me.bomb.amusic.resource.StatusReport;
import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.util.Logger;

public class LocalAMusic implements AMusic {
	
	public final Logger logger;
	public final SoundSource<? extends SourceEntry> soundsource;
	public final PositionTracker positiontracker;
	public final ResourceManager resourcemanager;
	public final Data datamanager;
	private final Executor executor;
	
	public LocalAMusic(Logger logger, Executor executor, SoundSource<? extends SourceEntry> soundsource, PositionTracker positiontracker, ResourceManager resourcemanager, Data datamanager) {
		this.logger = logger;
		this.soundsource = soundsource;
		this.positiontracker = positiontracker;
		this.resourcemanager = resourcemanager;
		this.datamanager = datamanager;
		this.executor = executor;
	}
	
	@Override
	public boolean updateCache() {
		return false;
	}
	
	public void enable() {
		positiontracker.start();
		resourcemanager.start();
		datamanager.start();
		datamanager.load();
	}
	
	public void disable() {
		positiontracker.end();
		resourcemanager.end();
		datamanager.end();
	}
	
	@Override
	public void login(UUID playeruuid) {
	}
	
	@Override
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
	
	@Override
	public final boolean getPlaylistSoundnames(UUID playeruuid, boolean useCache, Consumer<String[]> resultConsumer) {
		if(playeruuid == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				ResourcepackInfo info;
				SoundInfo[] soundsinfo;
				if ((info = positiontracker.getResourcepackInfo(playeruuid)) == null || (soundsinfo = info.getSounds()) == null) {
					resultConsumer.accept(null);
					return;
				}
				int i = soundsinfo.length;
				String[] soundnames = new String[i];
				while(--i > -1) {
					soundnames[i] = soundsinfo[i].name;
				}
				resultConsumer.accept(soundnames);
			}
		};
		executor.execute(r);
		return true;
	}
	
	@Override
	public final boolean getPlaylistSoundlengths(UUID playeruuid, boolean useCache, Consumer<short[]> resultConsumer) {
		if(playeruuid == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				ResourcepackInfo info;
				SoundInfo[] soundsinfo;
				if ((info = positiontracker.getResourcepackInfo(playeruuid)) == null || (soundsinfo = info.getSounds()) == null) {
					resultConsumer.accept(null);
					return;
				}
				int i = soundsinfo.length;
				short[] soundlengths = new short[i];
				while(--i > -1) {
					soundlengths[i] = soundsinfo[i].length;
				}
				resultConsumer.accept(soundlengths);
			}
		};
		executor.execute(r);
		return true;
	}
	
	@Override
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
	
	@Override
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
	
	@Override
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
	
	@Override
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
	
	@Override
	public final boolean loadPack(UUID[] playeruuid, String name, boolean update, StatusReport statusreport) {
		Runnable r = new ResourceFactory(name, playeruuid, datamanager, resourcemanager, update, statusreport);
		executor.execute(r);
		return true;
	}
	
	@Override
	public final boolean getPackName(UUID playeruuid, Consumer<String> resultConsumer) {
		if(playeruuid == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				ResourcepackInfo info = positiontracker.getResourcepackInfo(playeruuid);
				resultConsumer.accept(info == null ? null : info.getPackname());
			}
		};
		executor.execute(r);
		return true;
	}
	
	@Override
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
	
	@Override
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
	
	@Override
	public ResourcepackInfo getResourcepackInfoCached(String resourcepackname) {
		return null;
	}
	
	@Override
	public final boolean getResourcepackInfo(String resourcepackname, Consumer<ResourcepackInfo> resultConsumer) {
		if(resourcepackname == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				final DataEntry dataentry = datamanager.getResourcepack(resourcepackname);
				resultConsumer.accept(dataentry.info);
			}
		};
		executor.execute(r);
		return true;
	}
	
	@Override
	public final boolean getResourcepackInfo(UUID playeruuid, Consumer<ResourcepackInfo> resultConsumer) {
		if(playeruuid == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				resultConsumer.accept(positiontracker.getResourcepackInfo(playeruuid));
			}
		};
		executor.execute(r);
		return true;
	}
	
	@Override
	public final boolean setResourcepackCustomData(String resourcepackname, byte[] customdata, Consumer<Boolean> resultConsumer) {
		if(resourcepackname == null || customdata == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				final DataEntry dataentry = datamanager.getResourcepack(resourcepackname);
				if(dataentry == null || !(dataentry instanceof CustomDatastore)) {
					resultConsumer.accept(Boolean.FALSE);
					return;
				}
				resultConsumer.accept(((CustomDatastore)dataentry).updateCustomdata(customdata));
			}
		};
		executor.execute(r);
		return true;
	}
	
	@Override
	public String[] getListResourcepackInfoCached() {
		return null;
	}
	
	@Override
	public final boolean getListResourcepackInfo(Consumer<String[]> resultConsumer) {
		Runnable r = new Runnable() {
			public void run() {
				resultConsumer.accept(datamanager.listResourcepacks());
			}
		};
		executor.execute(r);
		return true;
	}
	
	@Override
	public String[] getListResourcepackCached() {
		return null;
	}
	
	@Override
	public String[] getListResourcepackSoundsCached(String resourcepackname) {
		return null;
	}
	
	@Override
	public boolean getListResourcepackSounds(String resourcepackname, Consumer<String[]> resultConsumer) {
		if(resourcepackname == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				resultConsumer.accept(soundsource.getSounds(resourcepackname));
			}
		};
		executor.execute(r);
		return true;
	}
	
	@Override
	public final boolean getListResourcepack(Consumer<String[]> resultConsumer) {
		Runnable r = new Runnable() {
			public void run() {
				resultConsumer.accept(soundsource.listResourcepacks());
			}
		};
		executor.execute(r);
		return true;
	}
	
}
