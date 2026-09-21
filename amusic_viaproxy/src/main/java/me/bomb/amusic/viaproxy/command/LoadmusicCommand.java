package me.bomb.amusic.viaproxy.command;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.viaversion.viaversion.api.connection.UserConnection;

import me.bomb.amusic.api.AMusic;
import me.bomb.amusic.api.LoadPackResult;
import me.bomb.amusic.util.Logger;

public final class LoadmusicCommand implements Command {
	
	private final AMusic amusic;
	private final ConcurrentHashMap<String, UUID> uuidByPlayername;
	
	public LoadmusicCommand(AMusic amusic, ConcurrentHashMap<String, UUID> uuidByPlayername) {
		this.amusic = amusic;
		this.uuidByPlayername = uuidByPlayername;
	}

	@Override
	public void handleConsole(Logger logger, String[] args) {
		if (args.length > 1) {
			UUID targetuuid = null;
			if (!args[0].equals("@n")) {
				if (args[0].equals("@s")) {
					logger.info("This selector unavilable from console");
					return;
				}
				targetuuid = uuidByPlayername.get(args[0]);
				if (targetuuid == null) {
					logger.info("Target player offline");
					return;
				}
			}
			if(args.length>2) {
				StringBuilder sb = new StringBuilder(args[1]);
				for(int i = 2;i < args.length;++i) {
					sb.append(' ');
					sb.append(args[i]);
				}
				args[1] = sb.toString();
			}
			String name = args[1];
			
			Consumer<LoadPackResult> consumer = new Consumer<LoadPackResult>() {
				@Override
				public void accept(LoadPackResult status) {
					if(status == null) {
						return;
					}
					if(status == LoadPackResult.NOTEXSIST) {
						logger.info("Playlist ".concat(name).concat(" not exsist"));
						return;
					}
					if(status == LoadPackResult.REMOVED) {
						logger.info("Removed playlist ".concat(name));
						return;
					}
					if(status == LoadPackResult.PACKED) {
						logger.info("Updated playlist ".concat(name));
						return;
					}
					if(status == LoadPackResult.DISPATCHED) {
						logger.info("Loaded playlist ".concat(name));
						return;
					}
					if(status == LoadPackResult.UNAVILABLE) {
						logger.info("Loader temporarily unavilable");
						return;
					}
				}
			};
			 
			logger.info("Processing ".concat(name));
			amusic.loadPack(targetuuid == null ? null : new UUID[] {targetuuid}, name, targetuuid == null, consumer);
		} else if(args.length == 1 && args[0].equals("@l")) {
			Consumer<String[]> consumer = new Consumer<String[]>() {
				@Override
				public void accept(String[] playlists) {
					StringBuilder sb = new StringBuilder("Playlists: ");
					for(String playlistname : playlists) {
						sb.append(playlistname);
						sb.append(' ');
					}
					logger.info(sb.toString());
				}
				
			};
			amusic.getListResourcepackInfo(consumer);
		} else {
			logger.info("Usage: loadmusic <player> <playlist>");
		}
		return;
	}

	@Override
	public void handlePlayer(UserConnection connection, String[] args) {
		
	}

}
