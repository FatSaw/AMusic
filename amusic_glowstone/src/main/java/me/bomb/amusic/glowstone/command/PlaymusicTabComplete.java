package me.bomb.amusic.glowstone.command;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import me.bomb.amusic.AMusic;

public final class PlaymusicTabComplete implements TabCompleter {
	private final Server server;
	private final AMusic amusic;
	public PlaymusicTabComplete(Server server, AMusic amusic) {
		this.server = server;
		this.amusic = amusic;
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
		//TODO: Suggest with space limit for pre 1.13 clients to avoid wrong values
		if (args.length > 1 && !args[0].equals("@l") && !args[0].equals("@p") && !args[0].equals("@r") && !args[0].equals("@a")) {
			boolean selfsender = false;
			if (args[0].equals("@s") && sender instanceof Player) {
				args[0] = sender.getName();
				selfsender = true;
			}
			if (selfsender || !(sender instanceof Player) || sender.hasPermission("amusic.playmusic.other")) {
				Player target = server.getPlayerExact(args[0]);
				if (target != null) {
					Consumer<String[]> consumer = new Consumer<String[]>() {
						@Override
						public void accept(String[] soundnames) {
							if (soundnames != null) {
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
									for (String soundname : soundnames) {
										if (soundname.startsWith(args[1]) && soundname.indexOf(0xA7) == -1) {
											tabcomplete.add(soundname);
										}
									}
								} else {
									for (String soundname : soundnames) {
										if (lastspace < soundname.length() && soundname.startsWith(args[1]) && soundname.indexOf(0xA7) == -1) {
											soundname = soundname.substring(lastspace);
											tabcomplete.add(soundname);
										}
									}
								}
							}
							synchronized (tabcomplete) {
								tabcomplete.notify();
							}
						}
					};
					boolean async = amusic.getPlaylistSoundnames(target.getUniqueId(), true, consumer);
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
		}
		return tabcomplete;
	}

}
