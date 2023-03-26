package me.bomb.amusic;

import java.util.NoSuchElementException;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.bomb.amusic.LangOptions.Placeholders;

class LoadmusicCommand implements CommandExecutor {
	private final Data data;
	protected LoadmusicCommand(Data data) {
		this.data = data;
	}
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(!sender.hasPermission("amusic.loadmusic")) {
			LangOptions.loadmusic_nopermission.sendMsg(sender);
			return true;
		}
		if(args.length>1) {
			if(args[0].equals("@s")) {
				if(sender instanceof Player) {
					args[0] = ((Player)sender).getName();
				} else {
					LangOptions.loadmusic_noconsoleselector.sendMsg(sender);
					return true;
				}
			} else if(!sender.hasPermission("amusic.loadmusic.other")) {
				LangOptions.loadmusic_nopermissionother.sendMsg(sender);
				return true;
			}
			Player target = Bukkit.getPlayerExact(args[0]);
			if(target==null) {
				LangOptions.loadmusic_targetoffline.sendMsg(sender);
				return true;
			}
			
			try {
				if(!ResourcePacked.load(target,data, args[1], sender.hasPermission("amusic.loadmusic.update")&&args.length>2&&args[2].toLowerCase().equals("update"))) {
					LangOptions.loadmusic_loaderunavilable.sendMsg(target);
					return true;
				}
				Placeholders[] placeholders = new Placeholders[1];
				placeholders[0] = new Placeholders("%playlistname%",args[1]);
				LangOptions.loadmusic_success.sendMsg(sender,placeholders);
			} catch (NoSuchElementException e) {
				Placeholders[] placeholders = new Placeholders[1];
				placeholders[0] = new Placeholders("%playlistname%",args[1]);
				LangOptions.loadmusic_noplaylist.sendMsg(sender,placeholders);
				return true;
			}
		} else {
			LangOptions.loadmusic_usage.sendMsg(sender);
		}
		return true;
	}
}
