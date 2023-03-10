package me.bomb.amusic;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

class PlaymusicTabComplete implements TabCompleter {
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		ArrayList<String> tabcomplete = new ArrayList<String>();
		if(!sender.hasPermission("amusic.playmusic")) {
			return null;
		}
		if (args.length == 1) {
			if(sender instanceof Player) {
				tabcomplete.add("@s");
			}
			if(sender.hasPermission("amusic.playmusic.other")) {
				for(Player player : Bukkit.getOnlinePlayers()) {
					if(player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
						tabcomplete.add(player.getName());
					}
				}
			}
		}
		if (args.length == 2) {
			if(args[0].equals("@s")&&sender instanceof Player) {
				args[0]=sender.getName();
			}
			Player target = Bukkit.getPlayerExact(args[0]);
			if(target!=null) {
				List<String> playlist = ResourcePacked.getActivePlaylist(target);
				if(playlist!=null) {
					for(String songname:playlist) {
						if(songname.startsWith(args[1])) {
							tabcomplete.add(songname);
						}
					}
				}
			}
		}
		return tabcomplete;
	}

}
