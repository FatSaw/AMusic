package me.bomb.amusic.bukkit.command;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import me.bomb.amusic.AMusic;

public final class UploadmusicTabComplete implements TabCompleter {
	
	private final AMusic amusic;
	
	public UploadmusicTabComplete(AMusic amusic) {
		this.amusic = amusic;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (!sender.hasPermission("amusic.uploadmusic")) {
			return null;
		}
		ArrayList<String> tabcomplete = new ArrayList<String>();
		if (args.length == 0) {
			tabcomplete.add("start");
			tabcomplete.add("finish");
			tabcomplete.add("drop");
		}
		if (args.length == 1) {
			String arg0 = args[0].toLowerCase();
			if ("start".startsWith(arg0)) {
				tabcomplete.add("start");
			}
			if ("finish".startsWith(arg0)) {
				tabcomplete.add("finish");
			}
			if ("drop".startsWith(arg0)) {
				tabcomplete.add("drop");
			}
		}
		if (args.length == 2 && sender.hasPermission("amusic.uploadmusic.token")) {
			String arg0 = args[0].toLowerCase();
			if ("finish".equals(arg0) || "drop".equals(arg0)) {
				Consumer<UUID[]> consumer = new Consumer<UUID[]>() {
					@Override
					public void accept(UUID[] sessions) {
						String arg1 = args[1].toUpperCase();
						for(UUID token : sessions) {
							String tokenstr = token.toString();
							if(tokenstr.startsWith(arg1)) {
								tabcomplete.add(tokenstr);
							}
						}
						synchronized (tabcomplete) {
							tabcomplete.notify();
						}
					}
				};
				boolean async = amusic.getUploadSessions(consumer);
				if(async) {
					try {
						synchronized (tabcomplete) {
							tabcomplete.wait(200);
						}
					} catch (InterruptedException e) {
					}
				}
			}
		}
		return tabcomplete;
	}

}
