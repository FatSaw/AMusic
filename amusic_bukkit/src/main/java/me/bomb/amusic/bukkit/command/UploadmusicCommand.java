package me.bomb.amusic.bukkit.command;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.permission.AMusicPermission;
import me.bomb.amusic.util.LangLoader;
import me.bomb.amusic.util.LangLoader.LangOptions;
import me.bomb.amusic.util.LangLoader.Placeholder;

public final class UploadmusicCommand extends Command {
	
	private final AMusic amusic;
	private final LangLoader lang;
	private final ConcurrentHashMap<UUID, EnumSet<AMusicPermission>> playerspermission;
	private final String uploaderhost;
	private final ConcurrentHashMap<Player, UUID> uploaders = new ConcurrentHashMap<Player, UUID>();
	private final ArrayList<String> emptytab = new ArrayList<String>(0);
	
	public UploadmusicCommand(AMusic amusic, LangLoader lang, ConcurrentHashMap<UUID, EnumSet<AMusicPermission>> playerspermission, String uploaderhost) {
		super("uploadmusic");
		this.amusic = amusic;
		this.lang = lang;
		this.playerspermission = playerspermission;
		this.uploaderhost = uploaderhost;
	}

	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {
		if(uploaderhost == null) {
			this.lang.sendMsg(sender, LangOptions.uploadmusic_disabled);
			return true;
		}
		EnumSet<AMusicPermission> permissions = null;
		if(sender instanceof Player && (permissions = this.playerspermission.get(((Player) sender).getUniqueId())) == null) {
			this.lang.sendMsg(sender, LangOptions.uploadmusic_nopermission);
			return true;
		}
		if(permissions != null && !permissions.contains(AMusicPermission.UPLOADMUSIC)) {
			this.lang.sendMsg(sender, LangOptions.uploadmusic_nopermission);
			return true;
		}
		boolean save = false;
		if(args.length < 1) {
			this.lang.sendMsg(sender, LangOptions.uploadmusic_usage);
			return true;
		}
		args[0] = args[0].toLowerCase();
		if(args.length == 1 && ((save = "finish".equals(args[0])) || "drop".equals(args[0]))) {
			if(!(sender instanceof Player)) {
				this.lang.sendMsg(sender, save ? LangOptions.uploadmusic_finish_player_notplayer : LangOptions.uploadmusic_drop_player_notplayer);
				return true;
			}
			Player player = (Player)sender;
			final UUID token = uploaders.remove(player);
			if(token == null) {
				this.lang.sendMsg(sender, save ? LangOptions.uploadmusic_finish_player_nosession : LangOptions.uploadmusic_drop_player_nosession);
				return true;
			}
			Consumer<Boolean> consumer = save ? new Consumer<Boolean>() {
				@Override
				public void accept(Boolean t) {
					UploadmusicCommand.this.lang.sendMsg(sender, t.booleanValue() ? LangOptions.uploadmusic_finish_player_success : LangOptions.uploadmusic_finish_player_nosession);
				}
			} : new Consumer<Boolean>() {
				@Override
				public void accept(Boolean t) {
					UploadmusicCommand.this.lang.sendMsg(sender, t.booleanValue() ? LangOptions.uploadmusic_drop_player_success : LangOptions.uploadmusic_drop_player_nosession);
				}
			};
			amusic.closeUploadSession(token, save, consumer);
			return true;
		}
		if(args.length < 2) {
			this.lang.sendMsg(sender, LangOptions.uploadmusic_usage);
			return true;
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
					UploadmusicCommand.this.lang.sendMsg(sender, sender instanceof Player ? LangOptions.uploadmusic_start_url_click : LangOptions.uploadmusic_start_url_show, new Placeholder("%url%", url, false));
					if(!(sender instanceof Player)) {
						return;
					}
					Player player = (Player)sender;
					uploaders.put(player, token);
				}
			};
			amusic.openUploadSession(args[1], consumer);
			return true;
		}
		if((save = "finish".equals(args[0])) || "drop".equals(args[0])) {
			if(permissions != null && !permissions.contains(AMusicPermission.UPLOADMUSIC_TOKEN)) {
				this.lang.sendMsg(sender, LangOptions.uploadmusic_nopermissiontoken);
				return true;
			}
			try {
				final UUID token = UUID.fromString(args[1]);
				Consumer<Boolean> consumer = save ? new Consumer<Boolean>() {
					@Override
					public void accept(Boolean t) {
						UploadmusicCommand.this.lang.sendMsg(sender, t.booleanValue() ? LangOptions.uploadmusic_finish_token_success : LangOptions.uploadmusic_finish_token_nosession);
					}
				} : new Consumer<Boolean>() {
					@Override
					public void accept(Boolean t) {
						UploadmusicCommand.this.lang.sendMsg(sender, t.booleanValue() ? LangOptions.uploadmusic_drop_token_success : LangOptions.uploadmusic_drop_token_nosession);
					}
				};
				amusic.closeUploadSession(token, save, consumer);
			} catch(IllegalArgumentException ex) {
				this.lang.sendMsg(sender, save ? LangOptions.uploadmusic_finish_token_invalid : LangOptions.uploadmusic_drop_token_invalid);
			}
			return true;
		}
		this.lang.sendMsg(sender, LangOptions.uploadmusic_usage);
		return true;
	}
	
	@Override
	public java.util.List<String> tabComplete(CommandSender sender, String alias, String[] args) throws CommandException, IllegalArgumentException {
		if(uploaderhost == null) {
			return emptytab;
		}
		EnumSet<AMusicPermission> permissions = null;
		if(sender instanceof Player && (permissions = this.playerspermission.get(((Player) sender).getUniqueId())) == null) {
			return emptytab;
		}
		if(permissions != null && !permissions.contains(AMusicPermission.UPLOADMUSIC)) {
			return emptytab;
		}
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
		if (args.length == 2 && (permissions == null || permissions.contains(AMusicPermission.UPLOADMUSIC_TOKEN))) {
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
