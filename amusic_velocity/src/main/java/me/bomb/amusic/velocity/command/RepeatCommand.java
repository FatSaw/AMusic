package me.bomb.amusic.velocity.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.RepeatType;
import me.bomb.amusic.util.LangOptions;

public final class RepeatCommand implements SimpleCommand {
	
	private final ProxyServer server;
	private final AMusic amusic;
	
	public RepeatCommand(ProxyServer server, AMusic amusic) {
		this.server = server;
		this.amusic = amusic;
	}

	@Override
	public void execute(Invocation invocation) {
		CommandSource sender = invocation.source();
		if (!sender.hasPermission("amusic.repeat")) {
			LangOptions.repeat_nopermission.sendMsg(sender);
			return;
		}
		String[] args = invocation.arguments();
		if (args.length > 1) {
			if (args[0].equals("@s")) {
				if (sender instanceof Player) {
					args[0] = ((Player) sender).getUsername();
				} else {
					LangOptions.repeat_noconsoleselector.sendMsg(sender);
					return;
				}
			} else if (!sender.hasPermission("amusic.repeat.other")) {
				LangOptions.repeat_nopermissionother.sendMsg(sender);
				return;
			}
			Optional<Player> otarget = server.getPlayer(args[0]);
			if (otarget.isEmpty()) {
				LangOptions.repeat_targetoffline.sendMsg(sender);
				return;
			}
			Player target = otarget.get();
			switch (args[1].toLowerCase()) {
			case "playone":
				amusic.setRepeatMode(target.getUniqueId(), null);
				LangOptions.repeat_playone.sendMsg(sender);
				return;
			case "repeatone":
				amusic.setRepeatMode(target.getUniqueId(), RepeatType.REPEATONE);
				LangOptions.repeat_repeatone.sendMsg(sender);
				return;
			case "repeatall":
				amusic.setRepeatMode(target.getUniqueId(), RepeatType.REPEATALL);
				LangOptions.repeat_repeatall.sendMsg(sender);
				return;
			case "playall":
				amusic.setRepeatMode(target.getUniqueId(), RepeatType.PLAYALL);
				LangOptions.repeat_playall.sendMsg(sender);
				return;
			case "random":
				amusic.setRepeatMode(target.getUniqueId(), RepeatType.RANDOM);
				LangOptions.repeat_random.sendMsg(sender);
				return;
			default:
				LangOptions.repeat_unknownrepeattype.sendMsg(sender);
			}
		} else {
			LangOptions.repeat_usage.sendMsg(sender);
		}
	}
	
	@Override
	public List<String> suggest(Invocation invocation) {
		CommandSource sender = invocation.source();
		if (!sender.hasPermission("amusic.repeat")) {
			return null;
		}
		String[] args = invocation.arguments();
		ArrayList<String> tabcomplete = new ArrayList<String>();
		if (args.length <= 1) {
			if (sender instanceof Player) {
				tabcomplete.add("@s");
			}
			if (sender.hasPermission("amusic.repeat.other")) {
				for (Player player : server.getAllPlayers()) {
					if (player.getUsername().toLowerCase().startsWith(args[0].toLowerCase())) {
						tabcomplete.add(player.getUsername());
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
