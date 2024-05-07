package me.bomb.amusic.bukkit.command;

import java.io.FileNotFoundException;
import java.util.UUID;

import org.bukkit.Bukkit;
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

	public LoadmusicCommand(ConfigOptions configuptions, Data data, ResourceManager resourcemanager, PositionTracker positiontracker, PackSender packsender) {
		LangOptions.values();
		this.configuptions = configuptions;
		this.data = data;
		this.resourcemanager = resourcemanager;
		this.positiontracker = positiontracker;
		this.packsender = packsender;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!sender.hasPermission("amusic.loadmusic")) {
			LangOptions.loadmusic_nopermission.sendMsg(sender);
			return true;
		}
		if (args.length > 1) {
			UUID targetuuid = null;
			if (!args[0].equals("@n") || !sender.hasPermission("amusic.loadmusic.nulltarget")) {
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
				Player target = Bukkit.getPlayerExact(args[0]);
				if (target == null) {
					LangOptions.loadmusic_targetoffline.sendMsg(sender);
					return true;
				}
				targetuuid = target.getUniqueId();
			}
			String name = args[1];
			try {
				if (!ResourceFactory.load(configuptions, data, resourcemanager, positiontracker, packsender, targetuuid, name, args.length > 2 && configuptions.processpack && sender.hasPermission("amusic.loadmusic.update") && args[2].toLowerCase().equals("update"))) {
					LangOptions.loadmusic_loaderunavilable.sendMsg(sender);
					return true;
				}
				Placeholders[] placeholders = new Placeholders[1];
				placeholders[0] = new Placeholders("%playlistname%", name);
				LangOptions.loadmusic_success.sendMsg(sender, placeholders);
			} catch (FileNotFoundException e) {
				Placeholders[] placeholders = new Placeholders[1];
				placeholders[0] = new Placeholders("%playlistname%", name);
				LangOptions.loadmusic_noplaylist.sendMsg(sender, placeholders);
				return true;
			}
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
}
