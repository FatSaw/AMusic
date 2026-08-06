package me.bomb.amusic.bukkit.command;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.RepeatType;
import me.bomb.amusic.permission.AMusicPermission;
import me.bomb.amusic.util.LangLoader;
import me.bomb.amusic.util.LangLoader.LangOptions;

public final class RepeatCommand extends Command {
	private final Server server;
	private final AMusic amusic;
	private final LangLoader lang;
	private final ConcurrentHashMap<UUID, EnumSet<AMusicPermission>> playerspermission;
	private final SelectorProcessor selectorprocessor;
	private final ArrayList<String> emptytab = new ArrayList<String>(0);
	
	public RepeatCommand(Server server, AMusic amusic, LangLoader lang, ConcurrentHashMap<UUID, EnumSet<AMusicPermission>> playerspermission, SelectorProcessor selectorprocessor) {
		super("repeat");
		this.server = server;
		this.amusic = amusic;
		this.lang = lang;
		this.playerspermission = playerspermission;
		this.selectorprocessor = selectorprocessor;
	}
	
	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {
		EnumSet<AMusicPermission> permissions = null;
		if(sender instanceof Player && (permissions = this.playerspermission.get(((Player) sender).getUniqueId())) == null) {
			this.lang.sendMsg(sender, LangOptions.repeat_nopermission);
			return true;
		}
		if(permissions != null && !permissions.contains(AMusicPermission.REPEAT)) {
			this.lang.sendMsg(sender, LangOptions.repeat_nopermission);
			return true;
		}
		if (args.length > 1) {
			if (args[0].equals("@s")) {
				if (sender instanceof Player) {
					args[0] = ((Player) sender).getName();
				} else {
					this.lang.sendMsg(sender, LangOptions.repeat_noconsoleselector);
					return true;
				}
			} else if (permissions != null && !permissions.contains(AMusicPermission.REPEAT_OTHER)) {
				this.lang.sendMsg(sender, LangOptions.repeat_nopermissionother);
				return true;
			}
			if (args[0].startsWith("@p")) {
				String closestplayername = selectorprocessor.getNearest(sender, args[0].substring(2));
				
				if(closestplayername == null) {
					this.lang.sendMsg(sender, LangOptions.repeat_unavilableselector_near);
					return true;
				}
				args[0] = closestplayername;
			}
			
			if (args[0].startsWith("@r")) {
				String randomplayername = selectorprocessor.getRandom(sender, args[0].substring(2));
				if(randomplayername == null) {
					this.lang.sendMsg(sender, LangOptions.repeat_unavilableselector_random);
					return true;
				}
				args[0] = randomplayername;
			}
			
			if(args[0].startsWith("@a")) {
				UUID[] targetarray = args[0].length() == 2 ? selectorprocessor.getAllGlobal() : selectorprocessor.getSameWorld(sender, args[0].substring(2)); 
				if(targetarray == null) {
					this.lang.sendMsg(sender, LangOptions.playmusic_unavilableselector_all);
					return true;
				}
				this.executeCommand(sender, args[1].toLowerCase(), targetarray);
				return true;
			}
			
			Player target = server.getPlayerExact(args[0]);
			if (target == null) {
				this.lang.sendMsg(sender, LangOptions.repeat_targetoffline);
				return true;
			}
			this.executeCommand(sender, args[1].toLowerCase(), target.getUniqueId());
			
		} else {
			this.lang.sendMsg(sender, LangOptions.repeat_usage);
		}
		return true;
	}
	
	@Override
	public java.util.List<String> tabComplete(CommandSender sender, String alias, String[] args) throws CommandException, IllegalArgumentException {
		EnumSet<AMusicPermission> permissions = null;
		if(sender instanceof Player && (permissions = this.playerspermission.get(((Player) sender).getUniqueId())) == null) {
			return emptytab;
		}
		if (permissions != null && !permissions.contains(AMusicPermission.REPEAT)) {
			return emptytab;
		}
		ArrayList<String> tabcomplete = new ArrayList<String>();
		if (args.length == 1) {
			if (sender instanceof Player) {
				tabcomplete.add("@s");
			}
			if (permissions == null || permissions.contains(AMusicPermission.REPEAT_OTHER)) {
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
					this.lang.sendMsg(sender, LangOptions.repeat_targetoffline);
					return;
				}
				amusic.setRepeatMode(target, null);
			}
			this.lang.sendMsg(sender, LangOptions.repeat_playone);
			return;
		case "repeatone":
			for(UUID target : targets) {
				if (target == null) {
					this.lang.sendMsg(sender, LangOptions.repeat_targetoffline);
					return;
				}
				amusic.setRepeatMode(target, RepeatType.REPEATONE);
			}
			this.lang.sendMsg(sender, LangOptions.repeat_repeatone);
			return;
		case "repeatall":
			for(UUID target : targets) {
				if (target == null) {
					this.lang.sendMsg(sender, LangOptions.repeat_targetoffline);
					return;
				}
				amusic.setRepeatMode(target, RepeatType.REPEATALL);
			}
			this.lang.sendMsg(sender, LangOptions.repeat_repeatall);
			return;
		case "playall":
			for(UUID target : targets) {
				if (target == null) {
					this.lang.sendMsg(sender, LangOptions.repeat_targetoffline);
					return;
				}
				amusic.setRepeatMode(target, RepeatType.PLAYALL);
			}
			this.lang.sendMsg(sender, LangOptions.repeat_playall);
			return;
		case "random":
			for(UUID target : targets) {
				if (target == null) {
					this.lang.sendMsg(sender, LangOptions.repeat_targetoffline);
					return;
				}
				amusic.setRepeatMode(target, RepeatType.RANDOM);
			}
			this.lang.sendMsg(sender, LangOptions.repeat_random);
			return;
		default:
			this.lang.sendMsg(sender, LangOptions.repeat_unknownrepeattype);
		}
		
	}
}
