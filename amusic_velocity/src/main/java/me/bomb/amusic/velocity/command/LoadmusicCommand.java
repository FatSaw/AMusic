package me.bomb.amusic.velocity.command;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import me.bomb.amusic.api.AMusic;
import me.bomb.amusic.api.LoadPackResult;
import me.bomb.amusic.lang.LangLoader;
import me.bomb.amusic.lang.LangLoader.LangOptions;
import me.bomb.amusic.lang.LangLoader.Placeholder;
import me.bomb.amusic.permission.AMusicPermission;

public final class LoadmusicCommand implements SimpleCommand {
	
	private final ProxyServer server;
	private final AMusic amusic;
	private final LangLoader lang;
	private final ConcurrentHashMap<UUID, EnumSet<AMusicPermission>> playerspermission;
	private final ArrayList<String> emptytab = new ArrayList<String>(0);
	
	public LoadmusicCommand(ProxyServer server, AMusic amusic, LangLoader lang, ConcurrentHashMap<UUID, EnumSet<AMusicPermission>> playerspermission) {
		this.server = server;
		this.amusic = amusic;
		this.lang = lang;
		this.playerspermission = playerspermission;
	}

	@Override
	public void execute(Invocation invocation) {
		CommandSource sender = invocation.source();
		EnumSet<AMusicPermission> permissions = null;
		if(sender instanceof Player && (permissions = this.playerspermission.get(((Player) sender).getUniqueId())) == null) {
			this.lang.sendMsg(sender, LangOptions.loadmusic_nopermission);
			return;
		}
		if(permissions != null && !permissions.contains(AMusicPermission.LOADMUSIC)) {
			this.lang.sendMsg(sender, LangOptions.loadmusic_nopermission);
			return;
		}
		String[] args = invocation.arguments();
		if (args.length > 1) {
			UUID targetuuid = null;
			if (!args[0].equals("@n") || permissions != null && !permissions.contains(AMusicPermission.LOADMUSIC_UPDATE)) {
				if (args[0].equals("@s")) {
					if (sender instanceof Player) {
						args[0] = ((Player) sender).getUsername();
					} else {
						this.lang.sendMsg(sender, LangOptions.loadmusic_noconsoleselector);
						return;
					}
				} else if (permissions != null && !permissions.contains(AMusicPermission.LOADMUSIC_OTHER)) {
					this.lang.sendMsg(sender, LangOptions.loadmusic_nopermissionother);
					return;
				}
				
				Optional<Player> target = server.getPlayer(args[0]);
				if (target.isEmpty()) {
					this.lang.sendMsg(sender, LangOptions.loadmusic_targetoffline);
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
			
			Placeholder placeholder = new Placeholder("%playlistname%", name, true);
			Consumer<LoadPackResult> consumer = new Consumer<LoadPackResult>() {
				@Override
				public void accept(LoadPackResult status) {
					if(status == null) {
						return;
					}
					LoadmusicCommand.this.lang.sendMsg(sender, status == LoadPackResult.NOTEXSIST ? LangOptions.loadmusic_noplaylist : status == LoadPackResult.UNAVILABLE ? LangOptions.loadmusic_loaderunavilable : status == LoadPackResult.REMOVED ? LangOptions.loadmusic_success_removed : status == LoadPackResult.PACKED ? LangOptions.loadmusic_success_packed : LangOptions.loadmusic_success_dispatched, placeholder);
				}
			};
			this.lang.sendMsg(sender, LangOptions.loadmusic_processing, placeholder);
			amusic.loadResourcepack(targetuuid == null ? null : new UUID[] {targetuuid}, name, targetuuid == null, consumer);
		} else if(args.length == 1 && args[0].equals("@l") && sender instanceof ConsoleCommandSource) {
			Consumer<String[]> consumer = new Consumer<String[]>() {
				@Override
				public void accept(String[] playlists) {
					StringBuilder sb = new StringBuilder("Playlists: ");
					for(String playlistname : playlists) {
						sb.append(playlistname);
						sb.append(' ');
					}
					sender.sendPlainMessage(sb.toString());
				}
				
			};
			amusic.getResourcepackInfoList(consumer);
		} else {
			this.lang.sendMsg(sender, LangOptions.loadmusic_usage);
		}
		return;
		
	}
	
	@Override
	public List<String> suggest(Invocation invocation) {
		CommandSource sender = invocation.source();
		EnumSet<AMusicPermission> permissions = null;
		if(sender instanceof Player && (permissions = this.playerspermission.get(((Player) sender).getUniqueId())) == null) {
			return emptytab;
		}
		if(permissions != null && !permissions.contains(AMusicPermission.LOADMUSIC)) {
			return emptytab;
		}
		String[] args = invocation.arguments();
		List<String> tabcomplete = new ArrayList<String>();
		if (args.length <= 1) {
			if (sender instanceof Player) {
				tabcomplete.add("@s");
			}
			if (permissions == null || permissions.contains(AMusicPermission.LOADMUSIC_OTHER)) {
				if(args.length == 0) {
					for (Player player : server.getAllPlayers()) {
						tabcomplete.add(player.getUsername());
					}
				} else {
					for (Player player : server.getAllPlayers()) {
						if (player.getUsername().toLowerCase().startsWith(args[0].toLowerCase())) {
							tabcomplete.add(player.getUsername());
						}
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
			final boolean packed = !args[0].equals("@n") || permissions != null && !permissions.contains(AMusicPermission.LOADMUSIC_UPDATE);
			String[] resourcepacknames = packed ? amusic.getResourcepackInfoListCached() : amusic.getSourceResourcepackNameListCached();
			if(resourcepacknames == null) {
				boolean async = packed ? amusic.getResourcepackInfoList(consumer) : amusic.getSourceResourcepackNameList(consumer);
				if(async) {
					try {
						synchronized (tabcomplete) {
							tabcomplete.wait(200);
						}
					} catch (InterruptedException e) {
					}
				}
			} else {
				consumer.accept(resourcepacknames);
			}
		}
		return tabcomplete;
	}

}
