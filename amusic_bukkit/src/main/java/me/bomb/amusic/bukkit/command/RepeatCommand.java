package me.bomb.amusic.bukkit.command;

import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.RepeatType;

public final class RepeatCommand implements CommandExecutor {
	private final Server server;
	private final PositionTracker positiontracker;
	private final SelectorProcessor selectorprocessor;
	public RepeatCommand(Server server, PositionTracker positiontracker, SelectorProcessor selectorprocessor) {
		this.server = server;
		this.positiontracker = positiontracker;
		this.selectorprocessor = selectorprocessor;
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
			if (args[0].startsWith("@p")) {
				String closestplayername = selectorprocessor.getNearest(sender, args[0].substring(2));
				
				if(closestplayername == null) {
					LangOptions.repeat_unavilableselector_near.sendMsg(sender);
					return true;
				}
				args[0] = closestplayername;
			}
			
			if (args[0].startsWith("@r")) {
				String randomplayername = selectorprocessor.getRandom(sender, args[0].substring(2));
				if(randomplayername == null) {
					LangOptions.repeat_unavilableselector_random.sendMsg(sender);
					return true;
				}
				args[0] = randomplayername;
			}
			
			if(args[0].startsWith("@a")) {
				UUID[] targetarray = args[0].length() == 2 ? selectorprocessor.getAllGlobal() : selectorprocessor.getSameWorld(sender, args[0].substring(2)); 
				if(targetarray == null) {
					LangOptions.playmusic_unavilableselector_all.sendMsg(sender);
					return true;
				}
				this.executeCommand(sender, args[1].toLowerCase(), targetarray);
				return true;
			}
			
			Player target = server.getPlayerExact(args[0]);
			if (target == null) {
				LangOptions.repeat_targetoffline.sendMsg(sender);
				return true;
			}
			this.executeCommand(sender, args[1].toLowerCase(), target.getUniqueId());
			
		} else {
			LangOptions.repeat_usage.sendMsg(sender);
		}
		return true;
	}
	
	private void executeCommand(CommandSender sender, String repeattype, UUID... targets) {
		switch (repeattype) {
		case "playone":
			for(UUID target : targets) {
				if (target == null) {
					LangOptions.repeat_targetoffline.sendMsg(sender);
					return;
				}
				positiontracker.setRepeater(target, null);
			}
			LangOptions.repeat_playone.sendMsg(sender);
			return;
		case "repeatone":
			for(UUID target : targets) {
				if (target == null) {
					LangOptions.repeat_targetoffline.sendMsg(sender);
					return;
				}
				positiontracker.setRepeater(target, RepeatType.REPEATONE);
			}
			LangOptions.repeat_repeatone.sendMsg(sender);
			return;
		case "repeatall":
			for(UUID target : targets) {
				if (target == null) {
					LangOptions.repeat_targetoffline.sendMsg(sender);
					return;
				}
				positiontracker.setRepeater(target, RepeatType.REPEATALL);
			}
			LangOptions.repeat_repeatall.sendMsg(sender);
			return;
		case "playall":
			for(UUID target : targets) {
				if (target == null) {
					LangOptions.repeat_targetoffline.sendMsg(sender);
					return;
				}
				positiontracker.setRepeater(target, RepeatType.PLAYALL);
			}
			
			LangOptions.repeat_playall.sendMsg(sender);
			return;
		case "random":
			for(UUID target : targets) {
				if (target == null) {
					LangOptions.repeat_targetoffline.sendMsg(sender);
					return;
				}
				positiontracker.setRepeater(target, RepeatType.RANDOM);
			}
			LangOptions.repeat_random.sendMsg(sender);
			return;
		default:
			LangOptions.repeat_unknownrepeattype.sendMsg(sender);
		}
		
	}
}
