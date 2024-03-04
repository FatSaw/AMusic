package me.bomb.amusic.bukkit.command;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.SoundInfo;
import me.bomb.amusic.bukkit.command.LangOptions.Placeholders;

public final class PlaymusicCommand implements CommandExecutor {
	private final PositionTracker positiontracker;
	public PlaymusicCommand(PositionTracker positiontracker) {
		this.positiontracker = positiontracker;
	}
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
			positiontracker.stopMusic(target.getUniqueId());
			LangOptions.playmusic_stop.sendMsg(sender);
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
			List<SoundInfo> soundsinfo = positiontracker.getSoundInfo(target.getUniqueId());
			if(soundsinfo==null) {
				LangOptions.playmusic_noplaylist.sendMsg(sender);
				return true;
			}

			Placeholders[] placeholders = new Placeholders[1];
			placeholders[0] = new Placeholders("%soundname%",args[1]);
			for(SoundInfo soundinfo : soundsinfo) {
				if(soundinfo.name.equals(args[1])) {
					positiontracker.playMusic(target.getUniqueId(),args[1]);
					LangOptions.playmusic_success.sendMsg(sender,placeholders);
					return true;
				}
			}
			LangOptions.playmusic_missingtrack.sendMsg(sender,placeholders);
			return true;
		} else {
			LangOptions.playmusic_usage.sendMsg(sender);
		}
		return true;
	}

}
