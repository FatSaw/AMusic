package me.bomb.amusic.bukkit.command;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
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
			} else if(args[0].equals("@l") && (sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender)) {
				Player target = Bukkit.getPlayerExact(args[1]);
				if(target==null) {
					LangOptions.playmusic_targetoffline.sendMsg(sender);
					return true;
				}
				UUID targetuuid = target.getUniqueId();
				List<SoundInfo> soundsinfo = positiontracker.getSoundInfo(targetuuid);
				if(soundsinfo==null) {
					LangOptions.playmusic_noplaylist.sendMsg(sender);
					return true;
				}
				String playing = positiontracker.getPlaying(targetuuid);
				short playingsize = positiontracker.getPlayingSize(targetuuid), playingstate = positiontracker.getPlayingRemain(targetuuid);;
				
				StringBuilder sb = new StringBuilder();
				if(playing!=null) {
					sb.append("Playing: ");
					sb.append(playing);
					sb.append(' ');
				}
				if(playingsize!=-1&&playingstate!=-1) {
					playingstate=(short) (playingsize-playingstate);
					sb.append(Short.toString(playingstate));
					sb.append('/');
					sb.append(Short.toString(playingsize));
					sb.append(' ');
				}
				sb.append("Sounds: ");
				for(SoundInfo soundinfo : soundsinfo) {
					sb.append(soundinfo.name);
					sb.append(' ');
				}
				sender.sendMessage(sb.toString());
				return true;
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
			if(args.length>2) {
				StringBuilder sb = new StringBuilder(args[1]);
				for(int i = 2;i < args.length;++i) {
					sb.append(' ');
					sb.append(args[i]);
				}
				args[1] = sb.toString();
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
