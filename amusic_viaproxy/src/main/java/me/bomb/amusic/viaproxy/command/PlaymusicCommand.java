package me.bomb.amusic.viaproxy.command;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.viaversion.viaversion.api.connection.UserConnection;

import me.bomb.amusic.api.AMusic;
import me.bomb.amusic.api.ResourcepackInfo;
import me.bomb.amusic.resourcepack.ResourcepackInfoImpl;
import me.bomb.amusic.resourcepack.SoundInfo;
import me.bomb.amusic.util.Logger;

public final class PlaymusicCommand implements Command {
	
	private final AMusic amusic;
	private final ConcurrentHashMap<String, UUID> uuidByPlayername;
	
	public PlaymusicCommand(AMusic amusic, ConcurrentHashMap<String, UUID> uuidByPlayername) {
		this.amusic = amusic;
		this.uuidByPlayername = uuidByPlayername;
	}

	@Override
	public void handleConsole(Logger logger, String[] args) {
		if(args.length==1) {
			if(args[0].equals("@s")) {
				logger.info("This selector unavilable from console");
				return;
			}
			UUID targetuuid = uuidByPlayername.get(args[0]);
			if(targetuuid == null) {
				logger.info("Target player offline");
				return;
			}
			Consumer<Boolean> consumer = new Consumer<Boolean>() {
				@Override
				public void accept(Boolean success) {
					if(success.booleanValue()) {
						logger.info("Stopping playing...");
					}
				}
			};
			amusic.stopSound(targetuuid, consumer);
		} else if(args.length>1) {
			if(args[0].equals("@s")) {
				logger.info("This selector unavilable from console");
				return;
			} else if(args[0].equals("@l")) {
				UUID targetuuid = uuidByPlayername.get(args[1]);
				if(targetuuid == null) {
					logger.info("Target player offline");
					return;
				}
				Consumer<ResourcepackInfo> consumer = new Consumer<ResourcepackInfo>() {
					@Override
					public void accept(ResourcepackInfo resourcepackinfo) {
						if(resourcepackinfo==null) {
							logger.info("Playlist not loaded fot that player");
							return;
						}
						SoundInfo[] soundinfos = ((ResourcepackInfoImpl)resourcepackinfo).sounds();
						StringBuilder sb = new StringBuilder();
						sb.append("Sounds:");
						int i = soundinfos.length;
						while(--i > -1) {
							SoundInfo soundinfo = soundinfos[i];
							sb.append('\n');
							sb.append(soundinfo.name);
						}
						logger.info(sb.toString());
					}
				};
				amusic.getResourcepackInfo(targetuuid, consumer);
				return;
			}
			UUID targetuuid = uuidByPlayername.get(args[0]);
			if(targetuuid == null) {
				logger.info("Target player offline");
				return;
			}
			if(args.length>2) {
				StringBuilder sb = new StringBuilder(args[1]);
				for(int i = 2;i < args.length;++i) {
					sb.append(' ');
					sb.append(args[i]);
				}
				args[1] = sb.toString();
			}
			String soundname = args[1];
			Consumer<Boolean> consumer = new Consumer<Boolean>() {
				@Override
				public void accept(Boolean success) {
					logger.info(success.booleanValue() ? ("Starting playing... ".concat(soundname)) : ("Sound ".concat(args[1]).concat(" not exist")));
				}
			};
			amusic.playSound(targetuuid, soundname, consumer);
			return;
		} else {
			logger.info("Usage: playmusic <player> [soundname]");
		}
		return;
	}

	@Override
	public void handlePlayer(UserConnection connection, String[] args) {
		
	}

}
