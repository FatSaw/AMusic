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
import me.bomb.amusic.api.ResourcepackInfo;
import me.bomb.amusic.lang.LangLoader;
import me.bomb.amusic.lang.LangLoader.LangOptions;
import me.bomb.amusic.lang.LangLoader.Placeholder;
import me.bomb.amusic.permission.AMusicPermission;
import me.bomb.amusic.resourcepack.ResourcepackInfoImpl;
import me.bomb.amusic.resourcepack.SoundInfo;

public final class PlaymusicCommand implements SimpleCommand  {

	private final ProxyServer server;
	private final AMusic amusic;
	private final LangLoader lang;
	private final ConcurrentHashMap<UUID, EnumSet<AMusicPermission>> playerspermission;
	private final ArrayList<String> emptytab = new ArrayList<String>(0);
	
	public PlaymusicCommand(ProxyServer server, AMusic amusic, LangLoader lang, ConcurrentHashMap<UUID, EnumSet<AMusicPermission>> playerspermission) {
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
			this.lang.sendMsg(sender, LangOptions.playmusic_nopermission);
			return;
		}
		if(permissions != null && !permissions.contains(AMusicPermission.PLAYMUSIC)) {
			this.lang.sendMsg(sender, LangOptions.playmusic_nopermission);
			return;
		}
		String[] args = invocation.arguments();
		if(args.length==1) {
			if(args[0].equals("@s")) {
				if(sender instanceof Player) {
					args[0] = ((Player) sender).getUsername();
				} else {
					this.lang.sendMsg(sender, LangOptions.playmusic_noconsoleselector);
					return;
				}
			} else if(permissions != null && !permissions.contains(AMusicPermission.PLAYMUSIC_OTHER)) {
				this.lang.sendMsg(sender, LangOptions.playmusic_nopermissionother);
				return;
			}
			Optional<Player> otarget = server.getPlayer(args[0]);
			if(otarget.isEmpty()) {
				this.lang.sendMsg(sender, LangOptions.playmusic_targetoffline);
				return;
			}
			Player target = otarget.get();
			Consumer<Boolean> consumer = new Consumer<Boolean>() {
				@Override
				public void accept(Boolean success) {
					PlaymusicCommand.this.lang.sendMsg(sender, (success.booleanValue() ? LangOptions.playmusic_stop_success : LangOptions.playmusic_stop_fail));
				}
			};
			amusic.stopSound(target.getUniqueId(), consumer);
		} else if(args.length>1) {
			if(args[0].equals("@s")) {
				if(sender instanceof Player) {
					args[0] = ((Player) sender).getUsername();
				} else {
					this.lang.sendMsg(sender, LangOptions.playmusic_noconsoleselector);
					return;
				}
			} else if(args[0].equals("@l") && sender instanceof ConsoleCommandSource) {
				Optional<Player> otarget = server.getPlayer(args[1]);
				if(otarget.isEmpty()) {
					this.lang.sendMsg(sender, LangOptions.playmusic_targetoffline);
					return;
				}
				Player target = otarget.get();
				UUID targetuuid = target.getUniqueId();
				Consumer<ResourcepackInfo> consumer = new Consumer<ResourcepackInfo>() {
					@Override
					public void accept(ResourcepackInfo resourcepackinfo) {
						if(resourcepackinfo==null) {
							PlaymusicCommand.this.lang.sendMsg(sender, LangOptions.playmusic_noplaylist);
							return;
						}
						SoundInfo[] soundinfos = ((ResourcepackInfoImpl)resourcepackinfo).sounds();
						StringBuilder sb = new StringBuilder();
						sb.append("Sounds:");
						int i = soundinfos.length;
						while(--i > -1) {
							SoundInfo soundinfo = soundinfos[i];
							sb.append('\n');
							sb.append(soundinfo.name);
						}
						sender.sendPlainMessage(sb.toString());
					}
				};
				amusic.getResourcepackInfo(targetuuid, consumer);
				return;
			}
			Optional<Player> otarget = server.getPlayer(args[0]);
			if(otarget.isEmpty()) {
				this.lang.sendMsg(sender, LangOptions.playmusic_targetoffline);
				return;
			}
			Player target = otarget.get();
			if(args.length>2) {
				StringBuilder sb = new StringBuilder(args[1]);
				for(int i = 2;i < args.length;++i) {
					sb.append(' ');
					sb.append(args[i]);
				}
				args[1] = sb.toString();
			}
			String soundname = args[1];
			Placeholder[] placeholders = new Placeholder[1];
			placeholders[0] = new Placeholder("%soundname%", soundname, true);
			Consumer<Boolean> consumer = new Consumer<Boolean>() {
				@Override
				public void accept(Boolean success) {
					PlaymusicCommand.this.lang.sendMsg(sender, success.booleanValue() ? LangOptions.playmusic_play_success : LangOptions.playmusic_play_fail, placeholders);
				}
			};
			amusic.playSound(target.getUniqueId(), soundname, consumer);
			return;
		} else {
			this.lang.sendMsg(sender, LangOptions.playmusic_usage);
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
		if (permissions != null && !permissions.contains(AMusicPermission.PLAYMUSIC)) {
			return emptytab;
		}
		String[] args = invocation.arguments();
		if (args.length <= 1) {
			List<String> tabcomplete = new ArrayList<String>();
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
			boolean selfsender = false;
			if (args[0].equals("@s") && sender instanceof Player) {
				args[0] = ((Player)sender).getUsername();
				selfsender = true;
			}
			Optional<Player> otarget = server.getPlayer(args[0]);
			if ((selfsender || permissions == null || permissions.contains(AMusicPermission.LOADMUSIC_OTHER)) && otarget.isPresent()) {
				List<String> tabcomplete = new ArrayList<String>();
				Consumer<ResourcepackInfo> consumer = new Consumer<ResourcepackInfo>() {
					@Override
					public void accept(ResourcepackInfo resourcepackinfo) {
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
						SoundInfo[] sounds = ((ResourcepackInfoImpl)resourcepackinfo).sounds();
						int i = sounds.length;
						if(lastspace == 0) {
							while(--i > -1) {
								SoundInfo sound = sounds[i];
								if(sound == null) {
									continue;
								}
								String soundname = sound.name;
								if (soundname.startsWith(args[1]) && soundname.indexOf(0xA7) == -1) {
									tabcomplete.add(soundname);
								}
							}
						} else {
							while(--i > -1) {
								SoundInfo sound = sounds[i];
								if(sound == null) {
									continue;
								}
								String soundname = sound.name;
								if (lastspace < soundname.length() && soundname.startsWith(args[1]) && soundname.indexOf(0xA7) == -1) {
									soundname = soundname.substring(lastspace);
									tabcomplete.add(soundname);
								}
							}
						}
						synchronized (tabcomplete) {
							tabcomplete.notify();
						}
					}
				};
				UUID targetuuid = otarget.get().getUniqueId();
				ResourcepackInfo cachedresourcepackinfo = amusic.getResourcepackInfoCached(targetuuid);
				if(cachedresourcepackinfo == null) {
					boolean async = amusic.getResourcepackInfo(targetuuid, consumer);
					if(async) {
						try {
							synchronized (tabcomplete) {
								tabcomplete.wait(200);
							}
						} catch (InterruptedException e) {
						}
					}
				} else {
					consumer.accept(cachedresourcepackinfo);
				}
				return tabcomplete;
			}
		}
		return emptytab;
	}

}
