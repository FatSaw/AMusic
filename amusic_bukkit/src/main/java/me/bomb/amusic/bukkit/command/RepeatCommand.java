package me.bomb.amusic.bukkit.command;

import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.RepeatType;
import me.bomb.amusic.util.LangOptions;

public final class RepeatCommand extends Command {
	private final Server server;
	private final AMusic amusic;
	private final SelectorProcessor selectorprocessor;
	public RepeatCommand(Server server, AMusic amusic, SelectorProcessor selectorprocessor) {
		super("repeat");
		this.server = server;
		this.amusic = amusic;
		this.selectorprocessor = selectorprocessor;
	}
	
	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {
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
	
	@Override
	public java.util.List<String> tabComplete(CommandSender sender, String alias, String[] args) throws CommandException, IllegalArgumentException {
		if (!sender.hasPermission("amusic.repeat")) {
			return null;
		}
		ArrayList<String> tabcomplete = new ArrayList<String>();
		if (args.length == 1) {
			if (sender instanceof Player) {
				tabcomplete.add("@s");
			}
			if (sender.hasPermission("amusic.repeat.other")) {
				for (Player player : server.getOnlinePlayers()) {
					if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
						tabcomplete.add(player.getName());
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
	
	private void executeCommand(CommandSender sender, String repeattype, UUID... targets) {
		switch (repeattype) {
		case "playone":
			for(UUID target : targets) {
				if (target == null) {
					LangOptions.repeat_targetoffline.sendMsg(sender);
					return;
				}
				amusic.setRepeatMode(target, null);
			}
			LangOptions.repeat_playone.sendMsg(sender);
			return;
		case "repeatone":
			for(UUID target : targets) {
				if (target == null) {
					LangOptions.repeat_targetoffline.sendMsg(sender);
					return;
				}
				amusic.setRepeatMode(target, RepeatType.REPEATONE);
			}
			LangOptions.repeat_repeatone.sendMsg(sender);
			return;
		case "repeatall":
			for(UUID target : targets) {
				if (target == null) {
					LangOptions.repeat_targetoffline.sendMsg(sender);
					return;
				}
				amusic.setRepeatMode(target, RepeatType.REPEATALL);
			}
			LangOptions.repeat_repeatall.sendMsg(sender);
			return;
		case "playall":
			for(UUID target : targets) {
				if (target == null) {
					LangOptions.repeat_targetoffline.sendMsg(sender);
					return;
				}
				amusic.setRepeatMode(target, RepeatType.PLAYALL);
			}
			
			LangOptions.repeat_playall.sendMsg(sender);
			return;
		case "random":
			for(UUID target : targets) {
				if (target == null) {
					LangOptions.repeat_targetoffline.sendMsg(sender);
					return;
				}
				amusic.setRepeatMode(target, RepeatType.RANDOM);
			}
			LangOptions.repeat_random.sendMsg(sender);
			return;
		default:
			LangOptions.repeat_unknownrepeattype.sendMsg(sender);
		}
		
	}
}
