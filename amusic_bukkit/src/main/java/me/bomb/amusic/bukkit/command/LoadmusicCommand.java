package me.bomb.amusic.bukkit.command;

import java.io.FileNotFoundException;
import java.util.UUID;

import org.bukkit.Server;
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
import me.bomb.amusic.source.SoundSource;

public final class LoadmusicCommand implements CommandExecutor {
	private final Server server;
	private final SoundSource source;
	private final ConfigOptions configuptions;
	private final Data data;
	private final ResourceManager resourcemanager;
	private final PositionTracker positiontracker;
	private final PackSender packsender;
	private final SelectorProcessor selectorprocessor;

	public LoadmusicCommand(Server server, SoundSource source, ConfigOptions configuptions, Data data, ResourceManager resourcemanager, PositionTracker positiontracker, PackSender packsender, SelectorProcessor selectorprocessor) {
		LangOptions.values();
		this.server = server;
		this.source = source;
		this.configuptions = configuptions;
		this.data = data;
		this.resourcemanager = resourcemanager;
		this.positiontracker = positiontracker;
		this.packsender = packsender;
		this.selectorprocessor = selectorprocessor;
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
				} else {
					if (args[0].startsWith("@p")) {
						String closestplayername = selectorprocessor.getNearest(sender, args[0].substring(2));
						
						if(closestplayername == null) {
							LangOptions.loadmusic_unavilableselector_near.sendMsg(sender);
							return true;
						}
						args[0] = closestplayername;
					}
					
					if (args[0].startsWith("@r")) {
						String randomplayername = selectorprocessor.getRandom(sender, args[0].substring(2));
						if(randomplayername == null) {
							LangOptions.loadmusic_unavilableselector_random.sendMsg(sender);
							return true;
						}
						args[0] = randomplayername;
					}
					if(args[0].startsWith("@a")) {
						UUID[] targetarray = args[0].length() == 2 ? selectorprocessor.getAllGlobal() : selectorprocessor.getSameWorld(sender, args[0].substring(2)); 
						if(targetarray == null) {
							LangOptions.loadmusic_unavilableselector_all.sendMsg(sender);
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
						String name = args[1];
						this.executeCommand(sender, name, targetarray);
						return true;
					}
				}
				
				Player target = server.getPlayerExact(args[0]);
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
				executeCommand(sender, name, null);
				return true;
			}
			executeCommand(sender, name, new UUID[]{targetuuid});
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
	
	private void executeCommand(CommandSender sender, String playlistname, UUID[] targetuuids) {
		try {
			if (!ResourceFactory.load(source, configuptions, data, resourcemanager, positiontracker, packsender, targetuuids, playlistname, false)) {
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
