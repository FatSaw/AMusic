package me.bomb.amusic.bukkit.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.SoundInfo;

public final class PlaymusicTabComplete implements TabCompleter {
	private final Server server;
	private final PositionTracker positiontracker;
	public PlaymusicTabComplete(Server server, PositionTracker positiontracker) {
		this.server = server;
		this.positiontracker = positiontracker;
	}
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		ArrayList<String> tabcomplete = new ArrayList<String>();
		if (!sender.hasPermission("amusic.playmusic")) {
			return null;
		}
		if (args.length == 1) {
			if (sender instanceof Player) {
				tabcomplete.add("@s");
			}
			if (sender.hasPermission("amusic.playmusic.other")) {
				for (Player player : server.getOnlinePlayers()) {
					if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
						tabcomplete.add(player.getName());
					}
				}
			}
			return tabcomplete;
		}
		if (args.length == 2 && !args[0].equals("@l") && !args[0].equals("@p") && !args[0].equals("@r") && !args[0].equals("@a")) {
			boolean selfsender = false;
			if (args[0].equals("@s") && sender instanceof Player) {
				args[0] = sender.getName();
				selfsender = true;
			}
			if (selfsender || !(sender instanceof Player) || sender.hasPermission("amusic.playmusic.other")) {
				Player target = server.getPlayerExact(args[0]);
				if (target != null) {
					SoundInfo[] soundsinfo = positiontracker.getSoundInfo(target.getUniqueId());
					if (soundsinfo != null) {
						for (SoundInfo soundinfo : soundsinfo) {
							String soundname = soundinfo.name;
							if (soundname.startsWith(args[1])) {
								tabcomplete.add(soundname);
							}
						}
					}
				}
			}
		}
		return tabcomplete;
	}

}
