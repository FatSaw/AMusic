package me.bomb.amusic.sponge.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Server;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.command.source.RconSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.resource.EnumStatus;
import me.bomb.amusic.resource.StatusReport;
import me.bomb.amusic.util.LangOptions;
import me.bomb.amusic.util.LangOptions.Placeholder;

public final class LoadmusicCommand implements CommandCallable {
	private final Server server;
	private final AMusic amusic;
	
	private final ArrayList<String> emptytab;

	public LoadmusicCommand(Server server, AMusic amusic) {
		this.server = server;
		this.amusic = amusic;
		emptytab = new ArrayList<String>(0);
	}
	
	private void executeCommand(CommandSource source, String playlistname, UUID[] targetuuids) {
		Placeholder placeholder = new Placeholder("%playlistname%", playlistname);
		StatusReport statusreport = new StatusReport() {
			@Override
			public void onStatusResponse(EnumStatus status) {
				if(status == null) {
					return;
				}
				(status == EnumStatus.NOTEXSIST ? LangOptions.loadmusic_noplaylist : status == EnumStatus.UNAVILABLE ? LangOptions.loadmusic_loaderunavilable : status == EnumStatus.REMOVED ? LangOptions.loadmusic_success_removed : status == EnumStatus.PACKED ? LangOptions.loadmusic_success_packed : LangOptions.loadmusic_success_dispatched).sendMsg(source, placeholder);
			}
		};
		LangOptions.loadmusic_processing.sendMsg(source, placeholder);
		amusic.loadPack(targetuuids, playlistname, targetuuids == null, statusreport);
	}

	@Override
	public CommandResult process(CommandSource source, String arguments) throws CommandException {
		if (!source.hasPermission("amusic.loadmusic")) {
			LangOptions.loadmusic_nopermission.sendMsg(source);
			return CommandResult.success();
		}
		String[] args = arguments.split(" ", 127);
		if (args.length > 1) {
			UUID targetuuid = null;
			if (!args[0].equals("@n") || !source.hasPermission("amusic.loadmusic.update")) {
				if (args[0].equals("@s")) {
					if (source instanceof Player) {
						args[0] = ((Player) source).getName();
					} else {
						LangOptions.loadmusic_noconsoleselector.sendMsg(source);
						return CommandResult.success();
					}
				} else if (!source.hasPermission("amusic.loadmusic.other")) {
					LangOptions.loadmusic_nopermissionother.sendMsg(source);
					return CommandResult.success();
				}
				
				Optional<Player> otarget = server.getPlayer(args[0]);
				if (!otarget.isPresent()) {
					LangOptions.loadmusic_targetoffline.sendMsg(source);
					return CommandResult.success();
				}
				Player target = otarget.get();
				targetuuid = target.getUniqueId();
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
			if(targetuuid == null) {
				executeCommand(source, name, null);
				return CommandResult.success();
			}
			executeCommand(source, name, new UUID[]{targetuuid});
		} else if(args.length == 1 && args[0].equals("@l") && (source instanceof ConsoleSource || source instanceof RconSource)) {
			StringBuilder sb = new StringBuilder("Playlists: ");
			for(String playlistname : amusic.getPlaylists()) {
				sb.append(playlistname);
				sb.append(' ');
			}
			source.sendMessage(Text.of(sb.toString()));
		} else {
			LangOptions.loadmusic_usage.sendMsg(source);
		}
		return CommandResult.success();
	}

	@Override
	public List<String> getSuggestions(CommandSource source, String arguments, Location<World> targetPosition) throws CommandException {
		if (!source.hasPermission("amusic.loadmusic")) {
			return emptytab;
		}
		List<String> tabcomplete = new ArrayList<String>();
		String[] args = arguments.split(" ", 127);
		if (args.length <= 1) {
			if (source instanceof Player) {
				tabcomplete.add("@s");
			}
			if (source.hasPermission("amusic.loadmusic.other")) {
				for (Player player : server.getOnlinePlayers()) {
					if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
						tabcomplete.add(player.getName());
					}
				}
			}
			return tabcomplete;
		}
		//TODO: Suggest with space limit for pre 1.13 clients to avoid wrong values
		if (args.length > 1 && !args[0].equals("@l")) {
			String[] playlists = amusic.getPlaylists();
			if (playlists != null) {
				int lastspace = -1;
				if(args.length > 2) {
					StringBuilder sb = new StringBuilder(args[1]);
					for(int i = 2;i < args.length;++i) {
						sb.append(' ');
						sb.append(args[i]);
					}
					args[1] = sb.toString();
					lastspace = args[1].lastIndexOf(' ');
				}
				++lastspace;
				if(lastspace == 0) {
					for (String playlist : playlists) {
						if (playlist.startsWith(args[1]) && playlist.indexOf('§') == -1) {
							tabcomplete.add(playlist);
						}
					}
				} else {
					for (String playlist : playlists) {
						if (lastspace < playlist.length() && playlist.startsWith(args[1]) && playlist.indexOf('§') == -1) {
							playlist = playlist.substring(lastspace);
							tabcomplete.add(playlist);
						}
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
