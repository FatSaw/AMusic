package me.bomb.amusic.bukkit.command;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import me.bomb.amusic.uploader.UploadManager;

public final class UploadmusicTabComplete implements TabCompleter {
	
	private final UploadManager uploadmanager;
	
	public UploadmusicTabComplete(UploadManager uploadmanager) {
		this.uploadmanager = uploadmanager;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (!sender.hasPermission("amusic.uploadmusic")) {
			return null;
		}
		ArrayList<String> tabcomplete = new ArrayList<String>();
		if (args.length == 1) {
			String arg0 = args[0].toLowerCase();
			if ("start".startsWith(arg0)) {
				tabcomplete.add("start");
			}
			if ("finish".startsWith(arg0)) {
				tabcomplete.add("finish");
			}
		}
		if (args.length == 2 && sender.hasPermission("amusic.uploadmusic.token")) {
			String arg0 = args[0].toLowerCase();
			if ("finish".equals(arg0)) {
				Enumeration<UUID> sessions = uploadmanager.getSessions();
				String arg1 = args[1].toUpperCase();
				while(sessions.hasMoreElements()) {
					String token = sessions.nextElement().toString();
					if(token.startsWith(arg1)) {
						tabcomplete.add(token);
					}
				}
			}
		}
		return tabcomplete;
	}

}
