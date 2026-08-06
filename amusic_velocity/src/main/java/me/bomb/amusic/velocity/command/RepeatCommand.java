package me.bomb.amusic.velocity.command;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.RepeatType;
import me.bomb.amusic.permission.AMusicPermission;
import me.bomb.amusic.util.LangLoader;
import me.bomb.amusic.util.LangLoader.LangOptions;

public final class RepeatCommand implements SimpleCommand {
	
	private final ProxyServer server;
	private final AMusic amusic;
	private final LangLoader lang;
	private final ConcurrentHashMap<UUID, EnumSet<AMusicPermission>> playerspermission;
	private final ArrayList<String> emptytab = new ArrayList<String>(0);
	
	public RepeatCommand(ProxyServer server, AMusic amusic, LangLoader lang, ConcurrentHashMap<UUID, EnumSet<AMusicPermission>> playerspermission) {
		this.server = server;
		this.amusic = amusic;
		this.lang = lang;
		this.playerspermission = playerspermission;
	}

	@Override
	public void execute(Invocation invocation) {
		CommandSource sender = invocation.source();
		EnumSet<AMusicPermission> permissions = null;
		if(sender instanceof Player && (permissions = this.playerspermission.get(((Player) sender).getUniqueId())) == null) {
			this.lang.sendMsg(sender, LangOptions.repeat_nopermission);
			return;
		}
		if(permissions != null && !permissions.contains(AMusicPermission.REPEAT)) {
			this.lang.sendMsg(sender, LangOptions.repeat_nopermission);
			return;
		}
		String[] args = invocation.arguments();
		if (args.length > 1) {
			if (args[0].equals("@s")) {
				if (sender instanceof Player) {
					args[0] = ((Player) sender).getUsername();
				} else {
					this.lang.sendMsg(sender, LangOptions.repeat_noconsoleselector);
					return;
				}
			} else if (permissions != null && !permissions.contains(AMusicPermission.REPEAT_OTHER)) {
				this.lang.sendMsg(sender, LangOptions.repeat_nopermissionother);
				return;
			}
			Optional<Player> otarget = server.getPlayer(args[0]);
			if (otarget.isEmpty()) {
				this.lang.sendMsg(sender, LangOptions.repeat_targetoffline);
				return;
			}
			Player target = otarget.get();
			switch (args[1].toLowerCase()) {
			case "playone":
				amusic.setRepeatMode(target.getUniqueId(), null);
				this.lang.sendMsg(sender, LangOptions.repeat_playone);
				return;
			case "repeatone":
				amusic.setRepeatMode(target.getUniqueId(), RepeatType.REPEATONE);
				this.lang.sendMsg(sender, LangOptions.repeat_repeatone);
				return;
			case "repeatall":
				amusic.setRepeatMode(target.getUniqueId(), RepeatType.REPEATALL);
				this.lang.sendMsg(sender, LangOptions.repeat_repeatall);
				return;
			case "playall":
				amusic.setRepeatMode(target.getUniqueId(), RepeatType.PLAYALL);
				this.lang.sendMsg(sender, LangOptions.repeat_playall);
				return;
			case "random":
				amusic.setRepeatMode(target.getUniqueId(), RepeatType.RANDOM);
				this.lang.sendMsg(sender, LangOptions.repeat_random);
				return;
			default:
				this.lang.sendMsg(sender, LangOptions.repeat_unknownrepeattype);
			}
		} else {
			this.lang.sendMsg(sender, LangOptions.repeat_usage);
		}
	}
	
	@Override
	public List<String> suggest(Invocation invocation) {
		CommandSource sender = invocation.source();
		EnumSet<AMusicPermission> permissions = null;
		if(sender instanceof Player && (permissions = this.playerspermission.get(((Player) sender).getUniqueId())) == null) {
			return emptytab;
		}
		if (permissions != null && !permissions.contains(AMusicPermission.REPEAT)) {
			return emptytab;
		}
		String[] args = invocation.arguments();
		ArrayList<String> tabcomplete = new ArrayList<String>();
		if (args.length <= 1) {
			if (sender instanceof Player) {
				tabcomplete.add("@s");
			}
			if (permissions == null || permissions.contains(AMusicPermission.REPEAT_OTHER)) {
				if(args.length == 0) {
					for (Player player : server.getAllPlayers()) {
						tabcomplete.add(player.getUsername());
					}
				} else {
					for (Player player : server.getAllPlayers()) {
						if (player.getUsername().toLowerCase().startsWith(args[0].toLowerCase())) {
							tabcomplete.add(player.getUsername());
						}
					}
				}
			}
		}
		if (args.length == 2) {
			String arg1 = args[1].toLowerCase();
			if ("repeatall".startsWith(arg1)) {
				tabcomplete.add("repeatall");
			}
			if ("repeatone".startsWith(arg1)) {
				tabcomplete.add("repeatone");
			}
			if ("playone".startsWith(arg1)) {
				tabcomplete.add("playone");
			}
			if ("playall".startsWith(arg1)) {
				tabcomplete.add("playall");
			}
			if ("random".startsWith(arg1)) {
				tabcomplete.add("random");
			}
		}
		return tabcomplete;
	}

}
