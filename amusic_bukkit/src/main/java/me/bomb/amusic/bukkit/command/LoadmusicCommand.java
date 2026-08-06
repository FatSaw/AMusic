package me.bomb.amusic.bukkit.command;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.permission.AMusicPermission;
import me.bomb.amusic.resource.EnumStatus;
import me.bomb.amusic.resource.StatusReport;
import me.bomb.amusic.util.LangLoader;
import me.bomb.amusic.util.LangLoader.LangOptions;
import me.bomb.amusic.util.LangLoader.Placeholder;

public final class LoadmusicCommand extends Command {
	
	private final Server server;
	private final AMusic amusic;
	private final LangLoader lang;
	private final ConcurrentHashMap<UUID, EnumSet<AMusicPermission>> playerspermission;
	private final SelectorProcessor selectorprocessor;
	private final ArrayList<String> emptytab = new ArrayList<String>(0);

	public LoadmusicCommand(Server server, AMusic amusic, LangLoader lang, ConcurrentHashMap<UUID, EnumSet<AMusicPermission>> playerspermission, SelectorProcessor selectorprocessor) {
		super("loadmusic");
		this.server = server;
		this.lang = lang;
		this.amusic = amusic;
		this.playerspermission = playerspermission;
		this.selectorprocessor = selectorprocessor;
	}

	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {
		EnumSet<AMusicPermission> permissions = null;
		if(sender instanceof Player && (permissions = this.playerspermission.get(((Player) sender).getUniqueId())) == null) {
			this.lang.sendMsg(sender, LangOptions.loadmusic_nopermission);
			return true;
		}
		if(permissions != null && !permissions.contains(AMusicPermission.LOADMUSIC)) {
			this.lang.sendMsg(sender, LangOptions.loadmusic_nopermission);
			return true;
		}
		if (args.length > 1) {
			UUID targetuuid = null;
			
			if (!args[0].equals("@n") || permissions != null && !permissions.contains(AMusicPermission.LOADMUSIC_UPDATE)) {
				if (args[0].equals("@s")) {
					if (sender instanceof Player) {
						args[0] = ((Player) sender).getName();
					} else {
						this.lang.sendMsg(sender, LangOptions.loadmusic_noconsoleselector);
						return true;
					}
				} else if (permissions != null && !permissions.contains(AMusicPermission.LOADMUSIC_OTHER)) {
					this.lang.sendMsg(sender, LangOptions.loadmusic_nopermissionother);
					return true;
				} else {
					if (args[0].startsWith("@p")) {
						String closestplayername = selectorprocessor.getNearest(sender, args[0].substring(2));
						
						if(closestplayername == null) {
							this.lang.sendMsg(sender, LangOptions.loadmusic_unavilableselector_near);
							return true;
						}
						args[0] = closestplayername;
					}
					
					if (args[0].startsWith("@r")) {
						String randomplayername = selectorprocessor.getRandom(sender, args[0].substring(2));
						if(randomplayername == null) {
							this.lang.sendMsg(sender, LangOptions.loadmusic_unavilableselector_random);
							return true;
						}
						args[0] = randomplayername;
					}
					if(args[0].startsWith("@a")) {
						UUID[] targetarray = args[0].length() == 2 ? selectorprocessor.getAllGlobal() : selectorprocessor.getSameWorld(sender, args[0].substring(2)); 
						if(targetarray == null) {
							this.lang.sendMsg(sender, LangOptions.loadmusic_unavilableselector_all);
							return true;
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
						this.executeCommand(sender, name, targetarray);
						return true;
					}
				}
				
				Player target = server.getPlayerExact(args[0]);
				if (target == null) {
					this.lang.sendMsg(sender, LangOptions.loadmusic_targetoffline);
					return true;
				}
				targetuuid = target.getUniqueId();
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
			if(targetuuid == null) {
				executeCommand(sender, name, null);
				return true;
			}
			executeCommand(sender, name, new UUID[]{targetuuid});
		} else if(args.length == 1 && args[0].equals("@l") && (sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender)) {
			
			Consumer<String[]> consumer = new Consumer<String[]>() {
				@Override
				public void accept(String[] playlists) {
					StringBuilder sb = new StringBuilder("Playlists: ");
					for(String playlistname : playlists) {
						sb.append(playlistname);
						sb.append(' ');
					}
					sender.sendMessage(sb.toString());
				}
				
			};
			amusic.getPlaylists(true, false, consumer);
			
		} else {
			this.lang.sendMsg(sender, LangOptions.loadmusic_usage);
		}
		return true;
	}
	
	@Override
	public java.util.List<String> tabComplete(CommandSender sender, String alias, String[] args) throws CommandException, IllegalArgumentException {
		EnumSet<AMusicPermission> permissions = null;
		if(sender instanceof Player && (permissions = this.playerspermission.get(((Player) sender).getUniqueId())) == null) {
			return emptytab;
		}
		if(permissions != null && !permissions.contains(AMusicPermission.LOADMUSIC)) {
			return emptytab;
		}
		ArrayList<String> tabcomplete = new ArrayList<String>();
		if (args.length == 1) {
			if (sender instanceof Player) {
				tabcomplete.add("@s");
			}
			if (permissions == null || permissions.contains(AMusicPermission.LOADMUSIC_OTHER)) {
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
			Consumer<String[]> consumer = new Consumer<String[]>() {
				@Override
				public void accept(String[] playlists) {
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
								if (playlist.startsWith(args[1]) && playlist.indexOf(0xA7) == -1) {
									tabcomplete.add(playlist);
								}
							}
						} else {
							for (String playlist : playlists) {
								if (lastspace < playlist.length() && playlist.startsWith(args[1]) && playlist.indexOf(0xA7) == -1) {
									playlist = playlist.substring(lastspace);
									tabcomplete.add(playlist);
								}
							}
						}
					}
					synchronized (tabcomplete) {
						tabcomplete.notify();
					}
				}
			};
			boolean async = amusic.getPlaylists(!args[0].equals("@n") || permissions != null && !permissions.contains(AMusicPermission.LOADMUSIC_UPDATE), true, consumer);
			if(async) {
				try {
					synchronized (tabcomplete) {
						tabcomplete.wait(200);
					}
				} catch (InterruptedException e) {
				}
			}
		}
		return tabcomplete;
	}
	
	private void executeCommand(CommandSender sender, String playlistname, UUID[] targetuuids) {
		Placeholder placeholder = new Placeholder("%playlistname%", playlistname, true);
		StatusReport statusreport = new StatusReport() {
			@Override
			public void onStatusResponse(EnumStatus status) {
				if(status == null) {
					return;
				}
				LoadmusicCommand.this.lang.sendMsg(sender, (status == EnumStatus.NOTEXSIST ? LangOptions.loadmusic_noplaylist : status == EnumStatus.UNAVILABLE ? LangOptions.loadmusic_loaderunavilable : status == EnumStatus.REMOVED ? LangOptions.loadmusic_success_removed : status == EnumStatus.PACKED ? LangOptions.loadmusic_success_packed : LangOptions.loadmusic_success_dispatched), placeholder);
			}
		};
		this.lang.sendMsg(sender, LangOptions.loadmusic_processing, placeholder);
		amusic.loadPack(targetuuids, playlistname, targetuuids == null, statusreport);
	}
	
}
