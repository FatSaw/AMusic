package me.bomb.amusic.bukkit.command;

import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.RepeatType;

public final class RepeatCommand implements CommandExecutor {
	private final PositionTracker positiontracker;
	private final SelectorProcessor selectorprocessor;
	private final Random random;
	public RepeatCommand(PositionTracker positiontracker, SelectorProcessor selectorprocessor, Random random) {
		this.positiontracker = positiontracker;
		this.selectorprocessor = selectorprocessor;
		this.random = random;
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
			if (args[0].equals("@p")) {
				Location executorlocation = null;
				Player senderplayer = null;
				if (sender instanceof BlockCommandSender) {
					BlockCommandSender commandblocksender = (BlockCommandSender) sender;
					executorlocation = commandblocksender.getBlock().getLocation();
				} else if (sender instanceof Player) {
					senderplayer = (Player) sender;
					executorlocation = senderplayer.getLocation();
				}
				if(executorlocation == null) {
					LangOptions.repeat_unavilableselector_near.sendMsg(sender);
					return true;
				}
				List<Player> players = executorlocation.getWorld().getPlayers();
				Player closestplayer = null;
				double mindistance = Double.MAX_VALUE;
				for(Player player : players) {
					double distance = executorlocation.distance(player.getLocation());
					if(player == senderplayer || distance > mindistance) {
						continue;
					}
					mindistance = distance;
					closestplayer = player;
				}
				args[0] = closestplayer.getName();
			}
			if (args[0].equals("@r")) {
				Location executorlocation = null;
				Player senderplayer = null;
				if (sender instanceof BlockCommandSender) {
					BlockCommandSender commandblocksender = (BlockCommandSender) sender;
					executorlocation = commandblocksender.getBlock().getLocation();
				} else if (sender instanceof Player) {
					senderplayer = (Player) sender;
					executorlocation = senderplayer.getLocation();
				}
				if(executorlocation == null) {
					LangOptions.repeat_unavilableselector_random.sendMsg(sender);
					return true;
				}
				List<Player> players = executorlocation.getWorld().getPlayers();
				int index = random.nextInt(players.size());
				Player randomplayer = players.get(index);
				args[0] = randomplayer.getName();
			}
			if (args[0].equals("@a")) {
				Location executorlocation = null;
				Player senderplayer = null;
				if (sender instanceof BlockCommandSender) {
					BlockCommandSender commandblocksender = (BlockCommandSender) sender;
					executorlocation = commandblocksender.getBlock().getLocation();
				} else if (sender instanceof Player) {
					senderplayer = (Player) sender;
					executorlocation = senderplayer.getLocation();
				}
				if(executorlocation == null) {
					LangOptions.repeat_unavilableselector_all.sendMsg(sender);
					return true;
				}
				List<Player> players = executorlocation.getWorld().getPlayers();
				Player[] playersarray = players.toArray(new Player[players.size()]);
				this.executeCommand(sender, args[1].toLowerCase(), playersarray);
				return true;
			}
			Player target = Bukkit.getPlayerExact(args[0]);
			if (target == null) {
				LangOptions.repeat_targetoffline.sendMsg(sender);
				return true;
			}
			this.executeCommand(sender, args[1].toLowerCase(), target);
			
		} else {
			LangOptions.repeat_usage.sendMsg(sender);
		}
		return true;
	}
	
	private void executeCommand(CommandSender sender, String repeattype, Player... targets) {
		switch (repeattype) {
		case "playone":
			for(Player target : targets) {
				if (target == null) {
					LangOptions.repeat_targetoffline.sendMsg(sender);
					return;
				}
				positiontracker.setRepeater(target.getUniqueId(), null);
			}
			LangOptions.repeat_playone.sendMsg(sender);
			return;
		case "repeatone":
			for(Player target : targets) {
				if (target == null) {
					LangOptions.repeat_targetoffline.sendMsg(sender);
					return;
				}
				positiontracker.setRepeater(target.getUniqueId(), RepeatType.REPEATONE);
			}
			LangOptions.repeat_repeatone.sendMsg(sender);
			return;
		case "repeatall":
			for(Player target : targets) {
				if (target == null) {
					LangOptions.repeat_targetoffline.sendMsg(sender);
					return;
				}
				positiontracker.setRepeater(target.getUniqueId(), RepeatType.REPEATALL);
			}
			LangOptions.repeat_repeatall.sendMsg(sender);
			return;
		case "playall":
			for(Player target : targets) {
				if (target == null) {
					LangOptions.repeat_targetoffline.sendMsg(sender);
					return;
				}
				positiontracker.setRepeater(target.getUniqueId(), RepeatType.PLAYALL);
			}
			
			LangOptions.repeat_playall.sendMsg(sender);
			return;
		case "random":
			for(Player target : targets) {
				if (target == null) {
					LangOptions.repeat_targetoffline.sendMsg(sender);
					return;
				}
				positiontracker.setRepeater(target.getUniqueId(), RepeatType.RANDOM);
			}
			LangOptions.repeat_random.sendMsg(sender);
			return;
		default:
			LangOptions.repeat_unknownrepeattype.sendMsg(sender);
		}
		
	}
}
