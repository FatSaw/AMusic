package me.bomb.amusic.velocity.command;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.util.LangOptions;
import me.bomb.amusic.util.LangOptions.Placeholder;

public final class UploadmusicCommand implements SimpleCommand {

	private final AMusic amusic;
	private final String uploaderhost;
	private final ConcurrentHashMap<Player, UUID> uploaders = new ConcurrentHashMap<Player, UUID>();
	
	public UploadmusicCommand(AMusic amusic, String uploaderhost) {
		this.amusic = amusic;
		this.uploaderhost = uploaderhost;
	}
	
	@Override
	public void execute(Invocation invocation) {
		CommandSource sender = invocation.source();
		if(uploaderhost == null) {
			LangOptions.uploadmusic_disabled.sendMsg(sender);
			return;
		}
		if(!sender.hasPermission("amusic.uploadmusic")) {
			LangOptions.uploadmusic_nopermission.sendMsg(sender);
			return;
		}
		String[] args = invocation.arguments();
		boolean save = false;
		if(args.length < 1) {
			LangOptions.uploadmusic_usage.sendMsg(sender);
			return;
		}
		args[0] = args[0].toLowerCase();
		if(args.length == 1 && (save = "finish".equals(args[0])) || "drop".equals(args[0])) {
			if(!(sender instanceof Player)) {
				LangOptions.uploadmusic_finish_player_notplayer.sendMsg(sender);
				return;
			}
			Player player = (Player)sender;
			final UUID token = uploaders.remove(player);
			if(token == null) {
				LangOptions.uploadmusic_finish_player_nosession.sendMsg(sender);
				return;
			}
			Consumer<Boolean> consumer = new Consumer<Boolean>() {
				@Override
				public void accept(Boolean t) {
					if(!t) {
						LangOptions.uploadmusic_finish_player_nosession.sendMsg(sender);
						return;
					}
					LangOptions.uploadmusic_finish_player_success.sendMsg(sender);
				}
			};
			amusic.closeUploadSession(token, save, consumer);
			return;
		}
		if(args.length < 2) {
			LangOptions.uploadmusic_usage.sendMsg(sender);
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
					(sender instanceof Player ? LangOptions.uploadmusic_start_url_click : LangOptions.uploadmusic_start_url_show).sendMsg(sender, new Placeholder("%url%", url, false));
					if(!(sender instanceof Player)) {
						return;
					}
					Player player = (Player)sender;
					uploaders.put(player, token);
				}
			};
			amusic.openUploadSession(args[1], consumer);
			return;
		}
		if((save = "finish".equals(args[0])) || "drop".equals(args[0])) {
			if(!sender.hasPermission("amusic.uploadmusic.token")) {
				LangOptions.uploadmusic_nopermissiontoken.sendMsg(sender);
				return;
			}
			try {
				final UUID token = UUID.fromString(args[1]);
				Consumer<Boolean> consumer = new Consumer<Boolean>() {
					@Override
					public void accept(Boolean t) {
						(t ? LangOptions.uploadmusic_finish_token_success : LangOptions.uploadmusic_finish_token_nosession).sendMsg(sender);
					}
				};
				amusic.closeUploadSession(token, save, consumer);
			} catch(IllegalArgumentException ex) {
				LangOptions.uploadmusic_finish_token_invalid.sendMsg(sender);
			}
			return;
		}
		LangOptions.uploadmusic_usage.sendMsg(sender);
		return;
	}
	
	@Override
	public List<String> suggest(Invocation invocation) {
		CommandSource sender = invocation.source();
		if (!sender.hasPermission("amusic.uploadmusic")) {
			return null;
		}
		String[] args = invocation.arguments();
		ArrayList<String> tabcomplete = new ArrayList<String>();
		if (args.length == 0) {
			tabcomplete.add("start");
			tabcomplete.add("finish");
			tabcomplete.add("drop");
		}
		if (args.length == 1) {
			String arg0 = args[0].toLowerCase();
			if ("start".startsWith(arg0)) {
				tabcomplete.add("start");
			}
			if ("finish".startsWith(arg0)) {
				tabcomplete.add("finish");
			}
			if ("drop".startsWith(arg0)) {
				tabcomplete.add("drop");
			}
		}
		if (args.length == 2 && sender.hasPermission("amusic.uploadmusic.token")) {
			String arg0 = args[0].toLowerCase();
			if ("finish".equals(arg0) || "drop".equals(arg0)) {
				Consumer<UUID[]> consumer = new Consumer<UUID[]>() {
					@Override
					public void accept(UUID[] sessions) {
						String arg1 = args[1].toUpperCase();
						for(UUID token : sessions) {
							String tokenstr = token.toString();
							if(tokenstr.startsWith(arg1)) {
								tabcomplete.add(tokenstr);
							}
						}
						synchronized (tabcomplete) {
							tabcomplete.notify();
						}
					}
				};
				boolean async = amusic.getUploadSessions(consumer);
				if(async) {
					try {
						synchronized (tabcomplete) {
							tabcomplete.wait(200);
						}
					} catch (InterruptedException e) {
					}
				}
			}
		}
		return tabcomplete;
	}
	
	public void logoutUploader(Player uploader) {
		final UUID token = uploaders.remove(uploader);
		if(token == null) {
			return;
		}
		amusic.closeUploadSession(token, false);
	}
	
}
