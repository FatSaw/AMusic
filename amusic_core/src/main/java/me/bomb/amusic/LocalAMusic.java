package me.bomb.amusic;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import me.bomb.amusic.api.AMusic;
import me.bomb.amusic.api.LoadPackResult;
import me.bomb.amusic.api.RepeatType;
import me.bomb.amusic.api.ResourcepackInfo;
import me.bomb.amusic.resourcepack.CustomDatastore;
import me.bomb.amusic.resourcepack.Data;
import me.bomb.amusic.resourcepack.DataEntry;
import me.bomb.amusic.resourcepack.ResourcepackInfoImpl;
import me.bomb.amusic.resourcepack.SoundSource;
import me.bomb.amusic.resourcepack.SourceEntry;
import me.bomb.amusic.resourcepack.UpdateResult;
import me.bomb.amusic.resourcepack.server.ResourceManager;
import me.bomb.amusic.tracker.PositionTracker;
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
	public boolean loadResourcepack(UUID[] playeruuid, String name, boolean update, Consumer<LoadPackResult> resultConsumer) {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				if(update) {
					UpdateResult result = datamanager.update(name);
					switch(result) {
					case UNAVILABLE:
						if(resultConsumer != null) resultConsumer.accept(LoadPackResult.UNAVILABLE);
						return;
					case DELETED_FAILED:
						if(resultConsumer != null) resultConsumer.accept(LoadPackResult.NOTEXSIST);
						return;
					case DELETED_SUCCESS:
						if(resultConsumer != null) resultConsumer.accept(LoadPackResult.REMOVED);
						return;
					case PACKED_FAILED:
						if(resultConsumer != null) resultConsumer.accept(LoadPackResult.NOTEXSIST);
						return;
					case PACKED_SUCCESS:
						if(playeruuid == null) {
							if(resultConsumer != null) resultConsumer.accept(LoadPackResult.PACKED);
						} else {
							DataEntry dataentry = datamanager.getResourcepack(name);
							if(resourcemanager.dispatch(dataentry, playeruuid)) {
								if(resultConsumer != null) resultConsumer.accept(LoadPackResult.DISPATCHED);
							} else {
								if(resultConsumer != null) resultConsumer.accept(LoadPackResult.UNAVILABLE);
							}
						}
						return;
					}
					return;
				}
				DataEntry dataentry = datamanager.getResourcepack(name);
				if(dataentry == null) {
					if(resultConsumer == null) {
						return;
					}
					resultConsumer.accept(LoadPackResult.NOTEXSIST);
					return;
				}
				if(playeruuid == null) {
					if(resultConsumer == null) {
						return;
					}
					resultConsumer.accept(LoadPackResult.PACKED);
					return;
				}
				if(resourcemanager.dispatch(dataentry, playeruuid)) {
					if(resultConsumer == null) {
						return;
					}
					resultConsumer.accept(LoadPackResult.DISPATCHED);
					return;
				}
				if(resultConsumer == null) {
					return;
				}
				resultConsumer.accept(LoadPackResult.UNAVILABLE);
			}
		};
		executor.execute(r);
		return true;
	}
	
	@Override
	public final boolean playSound(UUID playeruuid, String name, Consumer<Boolean> resultConsumer) {
		if(playeruuid == null || name == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				boolean success = positiontracker.playMusic(playeruuid, name);
				resultConsumer.accept(Boolean.valueOf(success));
			}
		};
		executor.execute(r);
		return true;
	}
	
	@Override
	public final boolean stopSound(UUID playeruuid, Consumer<Boolean> resultConsumer) {
		if(playeruuid == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				boolean success = positiontracker.stopMusic(playeruuid);
				resultConsumer.accept(Boolean.valueOf(success));
			}
		};
		executor.execute(r);
		return true;
	}
	
	@Override
	public final boolean setRepeat(UUID playeruuid, RepeatType repeattype) {
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
	public final boolean setResourcepackCustomdata(String resourcepackname, byte[] customdata, Consumer<Boolean> resultConsumer) {
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
	public final boolean getLoadedPlayers(String resourcepackname, Consumer<UUID[]> resultConsumer) {
		if(resourcepackname == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				resultConsumer.accept(positiontracker.getPlayersLoaded(resourcepackname));
			}
		};
		executor.execute(r);
		return true;
	}
	
	@Override
	public final boolean getLoadedResourcepackName(UUID playeruuid, Consumer<String> resultConsumer) {
		if(playeruuid == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				ResourcepackInfoImpl info = positiontracker.getResourcepackInfo(playeruuid);
				resultConsumer.accept(info == null ? null : info.getPackname());
			}
		};
		executor.execute(r);
		return true;
	}
	
	
	@Override
	public String[] getResourcepackInfoListCached() {
		return null;
	}
	
	@Override
	public final boolean getResourcepackInfoList(Consumer<String[]> resultConsumer) {
		Runnable r = new Runnable() {
			public void run() {
				resultConsumer.accept(datamanager.listResourcepacks());
			}
		};
		executor.execute(r);
		return true;
	}
	
	@Override
	public ResourcepackInfoImpl getResourcepackInfoCached(String resourcepackname) {
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
	public ResourcepackInfoImpl getResourcepackInfoCached(UUID playeruuid) {
		return null;
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
	public String[] getSourceResourcepackNameListCached() {
		return null;
	}
	
	@Override
	public final boolean getSourceResourcepackNameList(Consumer<String[]> resultConsumer) {
		Runnable r = new Runnable() {
			public void run() {
				resultConsumer.accept(soundsource.listResourcepacks());
			}
		};
		executor.execute(r);
		return true;
	}
	
	@Override
	public String[] getSourceSoundnameListCached(String resourcepackname) {
		return null;
	}
	
	@Override
	public boolean getSourceSoundnameList(String resourcepackname, Consumer<String[]> resultConsumer) {
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
	
}
