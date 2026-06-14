package me.bomb.amusic.viaproxy.command;

import java.util.UUID;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;

import com.viaversion.viaversion.api.connection.UserConnection;

import me.bomb.amusic.AMusic;

public final class UploadmusicCommand implements Command {

	private final AMusic amusic;
	private final String uploaderhost;
	
	public UploadmusicCommand(AMusic amusic, String uploaderhost) {
		this.amusic = amusic;
		this.uploaderhost = uploaderhost;
	}

	@Override
	public void handleConsole(Logger logger, String[] args) {
		if(uploaderhost == null) {
			logger.info("Loader disabled");
			return;
		}
		boolean save = false;
		if(args.length < 1) {
			logger.info("Usage: uploadmusic <start/finish/drop> <playlist>/[token]/[token]");
			return;
		}
		args[0] = args[0].toLowerCase();
		if(args.length == 1 && ((save = "finish".equals(args[0])) || "drop".equals(args[0]))) {
			logger.info(save ? "This command source do not support finish without token" : "This command source do not support drop without token");
			return;
		}
		if(args.length < 2) {
			logger.info("Usage: uploadmusic <start/finish/drop> <playlist>/[token]/[token]");
			return;
		}
		if("start".equals(args[0])) {
			if (args.length > 1) {
				if (args.length > 2) {
					StringBuilder sb = new StringBuilder(args[1]);
					for(int i = 2;i < args.length;++i) {
						sb.append(' ');
						sb.append(args[i]);
					}
					args[1] = sb.toString();
				}
			}
			Consumer<UUID> consumer = new Consumer<UUID>() {
				@Override
				public void accept(UUID token) {
					String url = uploaderhost.concat(token.toString());
					logger.info("Upload session started: ".concat(url));
				}
			};
			amusic.openUploadSession(args[1], consumer);
			return;
		}
		if((save = "finish".equals(args[0])) || "drop".equals(args[0])) {
			try {
				final UUID token = UUID.fromString(args[1]);
				Consumer<Boolean> consumer = save ? new Consumer<Boolean>() {
					@Override
					public void accept(Boolean t) {
						logger.info(t.booleanValue() ? "Changes saved successfully" : "No active upload session");
					}
				} : new Consumer<Boolean>() {
					@Override
					public void accept(Boolean t) {
						logger.info(t.booleanValue() ? "Changes dropped successfully" : "No active upload session");
					}
				};
				amusic.closeUploadSession(token, save, consumer);
			} catch(IllegalArgumentException ex) {
				logger.info(save ? "Token format invalid" : "Token format invalid");
			}
			return;
		}
		logger.info("Usage: uploadmusic <start/finish/drop> <playlist>/[token]/[token]");
	}

	@Override
	public void handlePlayer(UserConnection connection, String[] args) {
		
	}

}
