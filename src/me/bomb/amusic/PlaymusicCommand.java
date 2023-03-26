package me.bomb.amusic;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.bomb.amusic.LangOptions.Placeholders;

class PlaymusicCommand implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(!sender.hasPermission("amusic.playmusic")) {
			LangOptions.playmusic_nopermission.sendMsg(sender);
			return true;
		}
		if(args.length==1) {
			if(args[0].equals("@s")) {
				if(sender instanceof Player) {
					args[0] = sender.getName();
				} else {
					LangOptions.playmusic_noconsoleselector.sendMsg(sender);
					return true;
				}
			} else if(!sender.hasPermission("amusic.playmusic.other")) {
				LangOptions.playmusic_nopermissionother.sendMsg(sender);
				return true;
			}
			Player target = Bukkit.getPlayerExact(args[0]);
			if(target==null) {
				LangOptions.playmusic_targetoffline.sendMsg(sender);
				return true;
			}
			PositionTracker.stopMusic(target);
			LangOptions.playmusic_stop.sendMsg(sender);
			LangOptions.playmusic_stopping.sendMsgActionbar(target);
		} else if(args.length>1) {
			if(args[0].equals("@s")) {
				if(sender instanceof Player) {
					args[0] = sender.getName();
				} else {
					LangOptions.playmusic_noconsoleselector.sendMsg(sender);
					return true;
				}
			}
			Player target = Bukkit.getPlayerExact(args[0]);
			if(target==null) {
				LangOptions.playmusic_targetoffline.sendMsg(sender);
				return true;
			}
			List<String> playlist = ResourcePacked.getActivePlaylist(target.getUniqueId());
			if(playlist==null) {
				LangOptions.playmusic_noplaylist.sendMsg(sender);
				return true;
			}

			Placeholders[] placeholders = new Placeholders[1];
			placeholders[0] = new Placeholders("%soundname%",args[1]);
			if(!playlist.contains(args[1])) {
				LangOptions.playmusic_missingtrack.sendMsg(sender,placeholders);
				return true;
			}
			PositionTracker.playMusic(target,args[1]);
			LangOptions.playmusic_success.sendMsg(sender,placeholders);
		} else {
			LangOptions.playmusic_usage.sendMsg(sender);
		}
		return true;
	}

}
