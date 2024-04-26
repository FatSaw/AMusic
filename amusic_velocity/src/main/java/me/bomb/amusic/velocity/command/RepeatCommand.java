package me.bomb.amusic.velocity.command;

import java.util.Optional;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.RepeatType;

public class RepeatCommand implements SimpleCommand {
	private final ProxyServer server;
	private final PositionTracker positiontracker;
	public RepeatCommand(ProxyServer server, PositionTracker positiontracker) {
		this.server = server;
		this.positiontracker = positiontracker;
	}

	@Override
	public void execute(Invocation invocation) {
		CommandSource sender = invocation.source();
		if (!sender.hasPermission("amusic.repeat")) {
			sender.sendPlainMessage("No permissions");
			//LangOptions.repeat_nopermission.sendMsg(sender);
			return;
		}
		String[] args = invocation.arguments();
		if (args.length > 1) {
			if (args[0].equals("@s")) {
				if (sender instanceof Player) {
					args[0] = ((Player) sender).getUsername();
				} else {
					sender.sendPlainMessage("This selector unavilable from console");
					//LangOptions.repeat_noconsoleselector.sendMsg(sender);
					return;
				}
			} else if (!sender.hasPermission("amusic.repeat.other")) {
				sender.sendPlainMessage("No permissions other");
				//LangOptions.repeat_nopermissionother.sendMsg(sender);
				return;
			}
			Optional<Player> otarget = server.getPlayer(args[0]);
			if (otarget.isEmpty()) {
				sender.sendPlainMessage("Target offline");
				//LangOptions.repeat_targetoffline.sendMsg(sender);
				return;
			}
			Player target = otarget.get();
			switch (args[1].toLowerCase()) {
			case "playone":
				positiontracker.setRepeater(target.getUniqueId(), null);
				sender.sendPlainMessage("Repeat type set to playone");
				//LangOptions.repeat_playone.sendMsg(sender);
				return;
			case "repeatone":
				positiontracker.setRepeater(target.getUniqueId(), RepeatType.REPEATONE);
				sender.sendPlainMessage("Repeat type set to repeatone");
				//LangOptions.repeat_repeatone.sendMsg(sender);
				return;
			case "repeatall":
				positiontracker.setRepeater(target.getUniqueId(), RepeatType.REPEATALL);
				sender.sendPlainMessage("Repeat type set to repeatall");
				//LangOptions.repeat_repeatall.sendMsg(sender);
				return;
			case "playall":
				positiontracker.setRepeater(target.getUniqueId(), RepeatType.PLAYALL);
				sender.sendPlainMessage("Repeat type set to playall");
				//LangOptions.repeat_playall.sendMsg(sender);
				return;
			case "random":
				positiontracker.setRepeater(target.getUniqueId(), RepeatType.RANDOM);
				sender.sendPlainMessage("Repeat type set to random");
				//LangOptions.repeat_random.sendMsg(sender);
				return;
			default:
				sender.sendPlainMessage("Repeat types: playone, repeatone, repeatall, playall, random");
				//LangOptions.repeat_unknownrepeattype.sendMsg(sender);
			}
		} else {
			sender.sendPlainMessage("/repeat <target> <repeattype>");
			//LangOptions.repeat_usage.sendMsg(sender);
		}
	}

}
