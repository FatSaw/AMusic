package me.bomb.amusic.viaproxy.command;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Logger;

import com.viaversion.viaversion.api.connection.UserConnection;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.RepeatType;

public final class RepeatCommand implements Command {
	
	private final AMusic amusic;
	private final ConcurrentHashMap<String, UUID> uuidByPlayername;
	
	public RepeatCommand(AMusic amusic, ConcurrentHashMap<String, UUID> uuidByPlayername) {
		this.amusic = amusic;
		this.uuidByPlayername = uuidByPlayername;
	}

	@Override
	public void handleConsole(Logger logger, String[] args) {
		if (args.length > 1) {
			if (args[0].equals("@s")) {
				logger.info("This selector unavilable from console");
				return;
			}
			UUID targetplayer = uuidByPlayername.get(args[0]);
			if (targetplayer == null) {
				logger.info("Target player offline");
				return;
			}
			switch (args[1].toLowerCase()) {
			case "playone":
				amusic.setRepeatMode(targetplayer, null);
				logger.info("Repeat mode set to: play one");
				return;
			case "repeatone":
				amusic.setRepeatMode(targetplayer, RepeatType.REPEATONE);
				logger.info("Repeat mode set to: repeat one");
				return;
			case "repeatall":
				amusic.setRepeatMode(targetplayer, RepeatType.REPEATALL);
				logger.info("Repeat mode set to: repeat all");
				return;
			case "playall":
				amusic.setRepeatMode(targetplayer, RepeatType.PLAYALL);
				logger.info("Repeat mode set to: play all");
				return;
			case "random":
				amusic.setRepeatMode(targetplayer, RepeatType.RANDOM);
				logger.info("Repeat mode set to: random");
				return;
			default:
				logger.info("Unknown repeat mode");
			}
		} else {
			logger.info("Usage: repeat <player> <repeatmode>");
		}
	}

	@Override
	public void handlePlayer(UserConnection connection, String[] args) {
		
	}

}
