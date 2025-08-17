package me.bomb.amusic.sponge.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.util.LangOptions;
import me.bomb.amusic.util.LangOptions.Placeholder;

public final class UploadmusicCommand implements CommandCallable {
	
	private final AMusic amusic;
	private final String uploaderhost;
	private final ConcurrentHashMap<Player, UUID> uploaders = new ConcurrentHashMap<Player, UUID>();
	private final ArrayList<String> emptytab;
	
	public UploadmusicCommand(AMusic amusic, String uploaderhost) {
		this.amusic = amusic;
		this.uploaderhost = uploaderhost;
		emptytab = new ArrayList<String>(0);
	}
	
	public void logoutUploader(Player uploader) {
		final UUID token = uploaders.remove(uploader);
		if(token == null) {
			return;
		}
		amusic.closeUploadSession(token, false);
	}

	@Override
	public CommandResult process(CommandSource source, String arguments) throws CommandException {
		if(uploaderhost == null) {
			LangOptions.uploadmusic_disabled.sendMsg(source);
			return CommandResult.success();
		}
		if(!source.hasPermission("amusic.uploadmusic")) {
			LangOptions.uploadmusic_nopermission.sendMsg(source);
			return CommandResult.success();
		}
		String[] args = arguments.split(" ", 127);
		boolean save = false;
		if(args.length == 1 && (save = "finish".equals(args[0])) || "drop".equals(args[0])) {
			args[0] = args[0].toLowerCase();
			if(!(source instanceof Player)) {
				LangOptions.uploadmusic_finish_player_notplayer.sendMsg(source);
				return CommandResult.success();
			}
			Player player = (Player)source;
			final UUID token = uploaders.remove(player);
			if(token == null) {
				LangOptions.uploadmusic_finish_player_nosession.sendMsg(source);
				return CommandResult.success();
			}
			Consumer<Boolean> consumer = new Consumer<Boolean>() {
				@Override
				public void accept(Boolean t) {
					if(!t) {
						LangOptions.uploadmusic_finish_player_nosession.sendMsg(source);
						return;
					}
					LangOptions.uploadmusic_finish_player_success.sendMsg(source);
				}
			};
			amusic.closeUploadSession(token, save, consumer);
			return CommandResult.success();
		}
		if(args.length < 2) {
			LangOptions.uploadmusic_usage.sendMsg(source);
			return CommandResult.success();
		}
		args[0] = args[0].toLowerCase();
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
					(source instanceof Player ? LangOptions.uploadmusic_start_url_click : LangOptions.uploadmusic_start_url_show).sendMsg(source, new Placeholder("%url%", url));
					if(!(source instanceof Player)) {
						return;
					}
					Player player = (Player)source;
					uploaders.put(player, token);
				}
			};
			amusic.openUploadSession(args[1], consumer);
			return CommandResult.success();
		}
		if((save = "finish".equals(args[0])) || "drop".equals(args[0])) {
			if(!source.hasPermission("amusic.uploadmusic.token")) {
				LangOptions.uploadmusic_nopermissiontoken.sendMsg(source);
				return CommandResult.success();
			}
			try {
				final UUID token = UUID.fromString(args[1]);
				Consumer<Boolean> consumer = new Consumer<Boolean>() {
					@Override
					public void accept(Boolean t) {
						(t ? LangOptions.uploadmusic_finish_token_success : LangOptions.uploadmusic_finish_token_nosession).sendMsg(source);
					}
				};
				amusic.closeUploadSession(token, save, consumer);
			} catch(IllegalArgumentException ex) {
				LangOptions.uploadmusic_finish_token_invalid.sendMsg(source);
			}
			return CommandResult.success();
		}
		LangOptions.uploadmusic_usage.sendMsg(source);
		return CommandResult.success();
	}

	@Override
	public List<String> getSuggestions(CommandSource source, String arguments, Location<World> targetPosition) throws CommandException {
		if (!source.hasPermission("amusic.uploadmusic")) {
			return emptytab;
		}
		String[] args = arguments.split(" ", 127);
		ArrayList<String> tabcomplete = new ArrayList<String>();
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
		if (args.length == 2 && source.hasPermission("amusic.uploadmusic.token")) {
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

	@Override
	public boolean testPermission(CommandSource source) {
		return true;
	}

	@Override
	public Optional<Text> getShortDescription(CommandSource source) {
		return Optional.empty();
	}

	@Override
	public Optional<Text> getHelp(CommandSource source) {
		return Optional.empty();
	}

	@Override
	public Text getUsage(CommandSource source) {
		return null;
	}
	
}
