package me.bomb.amusic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

final class LoadmusicTabComplete implements TabCompleter {
	private final Data data;
	protected LoadmusicTabComplete(Data data) {
		this.data=data;
	}
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if(!sender.hasPermission("amusic.loadmusic")) {
			return null;
		}
		ArrayList<String> tabcomplete = new ArrayList<String>();
		if (args.length == 1) {
			if(sender instanceof Player) {
				tabcomplete.add("@s");
			}
			if(sender.hasPermission("amusic.loadmusic.other")) {
				for(Player player : Bukkit.getOnlinePlayers()) {
					if(player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
						tabcomplete.add(player.getName());
					}
				}
			}
			return tabcomplete;
		}
		if (args.length == 2) {
			Set<String> playlists = data.getPlaylists();
			if(playlists!=null) {
				for(String playlist:playlists) {
					if(playlist.startsWith(args[1])) {
						tabcomplete.add(playlist);
					}
				}
			}
		}
		return tabcomplete;
	};

}
