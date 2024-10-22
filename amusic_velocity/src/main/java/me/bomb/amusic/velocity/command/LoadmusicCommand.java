package me.bomb.amusic.velocity.command;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import me.bomb.amusic.ConfigOptions;
import me.bomb.amusic.Data;
import me.bomb.amusic.PackSender;
import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.ResourceFactory;
import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.source.SoundSource;
import me.bomb.amusic.velocity.command.LangOptions.Placeholders;

public class LoadmusicCommand implements SimpleCommand {
	private final ProxyServer server;
	private final SoundSource source;
	private final ConfigOptions configoptions;
	private final Data data;
	private final ResourceManager resourcemanager;
	private final PositionTracker positiontracker;
	private final PackSender packsender;
	
	public LoadmusicCommand(ProxyServer server, SoundSource source, ConfigOptions configoptions, Data data, ResourceManager resourcemanager, PositionTracker positiontracker, PackSender packsender) {
		this.server = server;
		this.source = source;
		this.configoptions = configoptions;
		this.data = data;
		this.resourcemanager = resourcemanager;
		this.positiontracker = positiontracker;
		this.packsender = packsender;
	}

	@Override
	public void execute(Invocation invocation) {
		CommandSource sender = invocation.source();
		if (!sender.hasPermission("amusic.loadmusic")) {
			LangOptions.loadmusic_nopermission.sendMsg(sender);
			return;
		}
		String[] args = invocation.arguments();
		if (args.length > 1) {
			UUID targetuuid = null;
			if (!args[0].equals("@n") || !sender.hasPermission("amusic.loadmusic.update")) {
				if (args[0].equals("@s")) {
					if (sender instanceof Player) {
						args[0] = ((Player) sender).getUsername();
					} else {
						LangOptions.loadmusic_noconsoleselector.sendMsg(sender);
						return;
					}
				} else if (!sender.hasPermission("amusic.loadmusic.other")) {

					LangOptions.loadmusic_nopermissionother.sendMsg(sender);
					return;
				}
				
				Optional<Player> target = server.getPlayer(args[0]);
				if (target.isEmpty()) {
					LangOptions.loadmusic_targetoffline.sendMsg(sender);
					return;
				}
				targetuuid = target.get().getUniqueId();
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
			try {
				if (!ResourceFactory.load(source, configoptions, data, resourcemanager, positiontracker, packsender, targetuuid == null ? null : new UUID[] {targetuuid}, name, false)) {
					LangOptions.loadmusic_loaderunavilable.sendMsg(sender);
					return;
				}

				Placeholders[] placeholders = new Placeholders[1];
				placeholders[0] = new Placeholders("%playlistname%", name);
				LangOptions.loadmusic_success.sendMsg(sender, placeholders);
			} catch (FileNotFoundException e) {
				Placeholders[] placeholders = new Placeholders[1];
				placeholders[0] = new Placeholders("%playlistname%", name);
				LangOptions.loadmusic_noplaylist.sendMsg(sender, placeholders);
				return;
			}
		} else if(args.length == 1 && args[0].equals("@l") && sender instanceof ConsoleCommandSource) {
			StringBuilder sb = new StringBuilder("Playlists: ");
			for(String playlistname : data.getPlaylists()) {
				sb.append(playlistname);
				sb.append(' ');
			}
			sender.sendPlainMessage(sb.toString());
		} else {
			LangOptions.loadmusic_usage.sendMsg(sender);
		}
		return;
		
	}
	
	@Override
	public List<String> suggest(Invocation invocation) {
		CommandSource sender = invocation.source();
		if (!sender.hasPermission("amusic.loadmusic")) {
			return null;
		}
		String[] args = invocation.arguments();
		List<String> tabcomplete = new ArrayList<String>();
		if (args.length <= 1) {
			if (sender instanceof Player) {
				tabcomplete.add("@s");
			}
			if (sender.hasPermission("amusic.loadmusic.other")) {
				for (Player player : server.getAllPlayers()) {
					if (player.getUsername().toLowerCase().startsWith(args[0].toLowerCase())) {
						tabcomplete.add(player.getUsername());
					}
				}
			}
			return tabcomplete;
		}
		if (args.length > 1 && !args[0].equals("@l")) {
			Set<String> playlists = data.getPlaylists();
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
