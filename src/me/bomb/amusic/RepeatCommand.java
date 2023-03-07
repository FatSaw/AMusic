package me.bomb.amusic;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

class RepeatCommand implements CommandExecutor {
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(!sender.hasPermission("amusic.repeat")) {
			LangOptions.repeat_nopermission.sendMsg(sender);
			return true;
		}
		if(args.length>1) {
			if(args[0].equals("@s")) {
				if(sender instanceof Player) {
					args[0] = ((Player)sender).getName();
				} else {
					LangOptions.repeat_noconsoleselector.sendMsg(sender);
					return true;
				}
			} else if(!sender.hasPermission("amusic.repeat.other")) {
				LangOptions.repeat_nopermissionother.sendMsg(sender);
				return true;
			}
			Player target = Bukkit.getPlayerExact(args[0]);
			if(target==null) {
				LangOptions.repeat_targetoffline.sendMsg(sender);
				return true;
			}
			switch(args[1].toLowerCase()) {
			case "repeatone":
				Repeater.setRepeater(target.getUniqueId(), true, true);
				LangOptions.repeat_repeatone.sendMsg(target);
			return true;
			case "repeatall":
				Repeater.setRepeater(target.getUniqueId(), true, false);
				LangOptions.repeat_repeatall.sendMsg(target);
			return true;
			case "playone":
				Repeater.setRepeater(target.getUniqueId(), false, true);
				LangOptions.repeat_playone.sendMsg(target);
			return true;
			case "playall":
				Repeater.setRepeater(target.getUniqueId(), false, false);
				LangOptions.repeat_playall.sendMsg(target);
			return true;
			default:
				LangOptions.repeat_unknownrepeattype.sendMsg(target);
			}
		} else {
			LangOptions.repeat_usage.sendMsg(sender);
		}
		return true;
	}
}
