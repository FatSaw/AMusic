package me.bomb.amusic.bukkit.command;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;

import me.bomb.amusic.ConfigOptions;
import me.bomb.amusic.Data;
import me.bomb.amusic.PackSender;
import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.ResourceFactory;
import me.bomb.amusic.bukkit.command.LangOptions.Placeholders;
import me.bomb.amusic.resourceserver.ResourceManager;

public final class LoadmusicCommand implements CommandExecutor {
	private final ConfigOptions configuptions;
	private final Data data;
	private final ResourceManager resourcemanager;
	private final PositionTracker positiontracker;
	private final PackSender packsender;
	private final SelectorProcessor selectorprocessor;
	private final Random random;

	public LoadmusicCommand(ConfigOptions configuptions, Data data, ResourceManager resourcemanager, PositionTracker positiontracker, PackSender packsender, SelectorProcessor selectorprocessor, Random random) {
		LangOptions.values();
		this.configuptions = configuptions;
		this.data = data;
		this.resourcemanager = resourcemanager;
		this.positiontracker = positiontracker;
		this.packsender = packsender;
		this.selectorprocessor = selectorprocessor;
		this.random = random;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!sender.hasPermission("amusic.loadmusic")) {
			LangOptions.loadmusic_nopermission.sendMsg(sender);
			return true;
		}
		if (args.length > 1) {
			UUID targetuuid = null;
			if (!args[0].equals("@n") || !sender.hasPermission("amusic.loadmusic.update")) {
				if (args[0].equals("@s")) {
					if (sender instanceof Player) {
						args[0] = ((Player) sender).getName();
					} else {
						LangOptions.loadmusic_noconsoleselector.sendMsg(sender);
						return true;
					}
				} else if (!sender.hasPermission("amusic.loadmusic.other")) {
					LangOptions.loadmusic_nopermissionother.sendMsg(sender);
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
						LangOptions.loadmusic_unavilableselector_near.sendMsg(sender);
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
						LangOptions.loadmusic_unavilableselector_random.sendMsg(sender);
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
						LangOptions.loadmusic_unavilableselector_all.sendMsg(sender);
						return true;
					}
					List<Player> players = executorlocation.getWorld().getPlayers();
					UUID[] targetarray = new UUID[players.size()];
					for(int i = players.size(); --i > -1;) {
						targetarray[i] = players.get(i).getUniqueId();
					}
					
					if(args.length>2) {
						StringBuilder sb = new StringBuilder(args[1]);
						for(int i = 2;i < args.length;++i) {
							sb.append(' ');
							sb.append(args[i]);
						}
						args[1] = sb.toString();
					}
					String name = args[1];
					
					this.executeCommand(sender, name, targetarray);
					return true;
				}
				
				
				Player target = Bukkit.getPlayerExact(args[0]);
				if (target == null) {
					LangOptions.loadmusic_targetoffline.sendMsg(sender);
					return true;
				}
				targetuuid = target.getUniqueId();
			}
			if(args.length>2) {
				StringBuilder sb = new StringBuilder(args[1]);
				for(int i = 2;i < args.length;++i) {
					sb.append(' ');
					sb.append(args[i]);
				}
				args[1] = sb.toString();
			}
			String name = args[1];
			if(targetuuid == null) {
				executeCommand(sender, name);
				return true;
			}
			executeCommand(sender, name, targetuuid);
		} else if(args.length == 1 && args[0].equals("@l") && (sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender)) {
			StringBuilder sb = new StringBuilder("Playlists: ");
			for(String playlistname : data.getPlaylists()) {
				sb.append(playlistname);
				sb.append(' ');
			}
			sender.sendMessage(sb.toString());
		} else {
			LangOptions.loadmusic_usage.sendMsg(sender);
		}
		return true;
	}
	
	private void executeCommand(CommandSender sender, String playlistname, UUID... targetuuids) {
		try {
			if (!ResourceFactory.load(configuptions, data, resourcemanager, positiontracker, packsender, targetuuids, playlistname, false)) {
				LangOptions.loadmusic_loaderunavilable.sendMsg(sender);
				return;
			}
			LangOptions.loadmusic_success.sendMsg(sender, new Placeholders("%playlistname%", playlistname));
		} catch (FileNotFoundException e) {
			LangOptions.loadmusic_noplaylist.sendMsg(sender, new Placeholders("%playlistname%", playlistname));
			return;
		}
		
	}
	
}
