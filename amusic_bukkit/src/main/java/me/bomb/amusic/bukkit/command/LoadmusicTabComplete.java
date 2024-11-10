package me.bomb.amusic.bukkit.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import me.bomb.amusic.packedinfo.DataStorage;

public final class LoadmusicTabComplete implements TabCompleter {
	private final Server server;
	private final DataStorage datamanager;

	public LoadmusicTabComplete(Server server, DataStorage datamanager) {
		this.server = server;
		this.datamanager = datamanager;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (!sender.hasPermission("amusic.loadmusic")) {
			return null;
		}
		ArrayList<String> tabcomplete = new ArrayList<String>();
		if (args.length == 1) {
			if (sender instanceof Player) {
				tabcomplete.add("@s");
			}
			if (sender.hasPermission("amusic.loadmusic.other")) {
				for (Player player : server.getOnlinePlayers()) {
					if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
						tabcomplete.add(player.getName());
					}
				}
			}
			return tabcomplete;
		}
		
		//TODO: Suggest with space limit for pre 1.13 clients to avoid wrong values
		if (args.length > 1 && !args[0].equals("@l")) {
			Set<String> playlists = datamanager.getPlaylists();
			if (playlists != null) {
				int lastspace = -1;
				if(args.length > 2) {
					StringBuilder sb = new StringBuilder(args[1]);
					for(int i = 2;i < args.length;++i) {
						sb.append(' ');
						sb.append(args[i]);
					}
					args[1] = sb.toString();
					lastspace = args[1].lastIndexOf(' ');
				}
				++lastspace;
				if(lastspace == 0) {
					for (String playlist : playlists) {
						if (playlist.startsWith(args[1])) {
							tabcomplete.add(playlist);
						}
					}
				} else {
					for (String playlist : playlists) {
						if (lastspace < playlist.length() && playlist.startsWith(args[1])) {
							playlist = playlist.substring(lastspace);
							tabcomplete.add(playlist);
						}
					}
				}
			}
		}
		return tabcomplete;
	}

}
