package me.bomb.amusic;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class RepeatCommand implements CommandExecutor {
	private final PositionTracker positiontracker;
	protected RepeatCommand(PositionTracker positiontracker) {
		this.positiontracker = positiontracker;
	}
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!sender.hasPermission("amusic.repeat")) {
			LangOptions.repeat_nopermission.sendMsg(sender);
			return true;
		}
		if (args.length > 1) {
			if (args[0].equals("@s")) {
				if (sender instanceof Player) {
					args[0] = ((Player) sender).getName();
				} else {
					LangOptions.repeat_noconsoleselector.sendMsg(sender);
					return true;
				}
			} else if (!sender.hasPermission("amusic.repeat.other")) {
				LangOptions.repeat_nopermissionother.sendMsg(sender);
				return true;
			}
			Player target = Bukkit.getPlayerExact(args[0]);
			if (target == null) {
				LangOptions.repeat_targetoffline.sendMsg(sender);
				return true;
			}
			switch (args[1].toLowerCase()) {
			case "playone":
				positiontracker.setRepeater(target.getUniqueId(), null);
				LangOptions.repeat_playone.sendMsg(sender);
				return true;
			case "repeatone":
				positiontracker.setRepeater(target.getUniqueId(), RepeatType.REPEATONE);
				LangOptions.repeat_repeatone.sendMsg(sender);
				return true;
			case "repeatall":
				positiontracker.setRepeater(target.getUniqueId(), RepeatType.REPEATALL);
				LangOptions.repeat_repeatall.sendMsg(sender);
				return true;
			case "playall":
				positiontracker.setRepeater(target.getUniqueId(), RepeatType.PLAYALL);
				LangOptions.repeat_playall.sendMsg(sender);
				return true;
			case "random":
				positiontracker.setRepeater(target.getUniqueId(), RepeatType.RANDOM);
				LangOptions.repeat_random.sendMsg(sender);
				return true;
			default:
				LangOptions.repeat_unknownrepeattype.sendMsg(sender);
			}
		} else {
			LangOptions.repeat_usage.sendMsg(sender);
		}
		return true;
	}
}
