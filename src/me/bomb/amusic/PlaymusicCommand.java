package me.bomb.amusic;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.bomb.amusic.LangOptions.Placeholders;

final class PlaymusicCommand implements CommandExecutor {
	private final PositionTracker positiontracker;
	protected PlaymusicCommand(PositionTracker positiontracker) {
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
			positiontracker.stopMusic(target);
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
			List<SoundInfo> soundsinfo = ResourcePacked.getSoundInfo(target.getUniqueId());
			if(soundsinfo==null) {
				LangOptions.playmusic_noplaylist.sendMsg(sender);
				return true;
			}

			Placeholders[] placeholders = new Placeholders[1];
			placeholders[0] = new Placeholders("%soundname%",args[1]);
			for(SoundInfo soundinfo : soundsinfo) {
				if(soundinfo.name.equals(args[1])) {
					positiontracker.playMusic(target,args[1]);
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
