package me.bomb.amusic.velocity.command;

import java.io.FileNotFoundException;
import java.util.Optional;
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

public class LoadmusicCommand implements SimpleCommand {
	private final ProxyServer server;
	private final ConfigOptions configoptions;
	private final Data data;
	private final ResourceManager resourcemanager;
	private final PositionTracker positiontracker;
	private final PackSender packsender;
	
	public LoadmusicCommand(ProxyServer server, ConfigOptions configoptions, Data data, ResourceManager resourcemanager, PositionTracker positiontracker, PackSender packsender) {
		this.server = server;
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
			sender.sendPlainMessage("No permissions");
			//LangOptions.loadmusic_nopermission.sendMsg(sender);
			return;
		}
		String[] args = invocation.arguments();
		if (args.length > 1) {
			UUID targetuuid = null;
			if (!args[0].equals("@n") || !sender.hasPermission("amusic.loadmusic.nulltarget")) {
				if (args[0].equals("@s")) {
					if (sender instanceof Player) {
						args[0] = ((Player) sender).getUsername();
					} else {
						sender.sendPlainMessage("This selector unavilable from console");
						//LangOptions.loadmusic_noconsoleselector.sendMsg(sender);
						return;
					}
				} else if (!sender.hasPermission("amusic.loadmusic.other")) {

					sender.sendPlainMessage("No permissions other");
					//LangOptions.loadmusic_nopermissionother.sendMsg(sender);
					return;
				}
				
				Optional<Player> target = server.getPlayer(args[0]);
				if (target.isEmpty()) {
					sender.sendPlainMessage("Target offline");
					//LangOptions.loadmusic_targetoffline.sendMsg(sender);
					return;
				}
				targetuuid = target.get().getUniqueId();
			}
			String name = args[1];
			try {
				if (!ResourceFactory.load(configoptions, data, resourcemanager, positiontracker, packsender, targetuuid, name, args.length > 2 && configoptions.processpack && sender.hasPermission("amusic.loadmusic.update") && args[2].toLowerCase().equals("update"))) {
					//LangOptions.loadmusic_loaderunavilable.sendMsg(sender);
					return;
				}

				sender.sendPlainMessage("Loading music: " + name);
				//Placeholders[] placeholders = new Placeholders[1];
				//placeholders[0] = new Placeholders("%playlistname%", name);
				//LangOptions.loadmusic_success.sendMsg(sender, placeholders);
			} catch (FileNotFoundException e) {
				sender.sendPlainMessage("No playlist: " + name);
				//Placeholders[] placeholders = new Placeholders[1];
				//placeholders[0] = new Placeholders("%playlistname%", name);
				//LangOptions.loadmusic_noplaylist.sendMsg(sender, placeholders);
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
			sender.sendPlainMessage("/loadmusic <target> <playlistname> [update]");
			//LangOptions.loadmusic_usage.sendMsg(sender);
		}
		return;
		
	}
	
	@Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("amusic.loadmusic");
    }

}
