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

import me.bomb.amusic.api.AMusic;
import me.bomb.amusic.api.ResourcepackInfo;
import me.bomb.amusic.lang.LangLoader;
import me.bomb.amusic.lang.LangLoader.LangOptions;
import me.bomb.amusic.lang.LangLoader.Placeholder;
import me.bomb.amusic.permission.AMusicPermission;
import me.bomb.amusic.resourcepack.ResourcepackInfoImpl;
import me.bomb.amusic.resourcepack.SoundInfo;

public final class PlaymusicCommand extends Command {
	private final Server server;
	private final AMusic amusic;
	private final LangLoader lang;
	private final ConcurrentHashMap<UUID, EnumSet<AMusicPermission>> playerspermission;
	private final SelectorProcessor selectorprocessor;
	private final ArrayList<String> emptytab = new ArrayList<String>(0);
	
	public PlaymusicCommand(Server server, AMusic amusic, LangLoader lang, ConcurrentHashMap<UUID, EnumSet<AMusicPermission>> playerspermission, SelectorProcessor selectorprocessor) {
		super("playmusic");
		this.server = server;
		this.amusic = amusic;
		this.lang = lang;
		this.playerspermission = playerspermission;
		this.selectorprocessor = selectorprocessor;
	}
	
	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {
		EnumSet<AMusicPermission> permissions = null;
		if(sender instanceof Player && (permissions = this.playerspermission.get(((Player) sender).getUniqueId())) == null) {
			this.lang.sendMsg(sender, LangOptions.playmusic_nopermission);
			return true;
		}
		if(permissions != null && !permissions.contains(AMusicPermission.PLAYMUSIC)) {
			this.lang.sendMsg(sender, LangOptions.playmusic_nopermission);
			return true;
		}
		if(args.length==1) {
			if(args[0].equals("@s")) {
				if(sender instanceof Player) {
					args[0] = sender.getName();
				} else {
					this.lang.sendMsg(sender, LangOptions.playmusic_noconsoleselector);
					return true;
				}
			} else if(permissions != null && !permissions.contains(AMusicPermission.PLAYMUSIC_OTHER)) {
				this.lang.sendMsg(sender, LangOptions.playmusic_nopermissionother);
				return true;
			} else {
				if (args[0].startsWith("@p")) {
					String closestplayername = selectorprocessor.getNearest(sender, args[0].substring(2));
					
					if(closestplayername == null) {
						this.lang.sendMsg(sender, LangOptions.playmusic_unavilableselector_near);
						return true;
					}
					args[0] = closestplayername;
				}
				
				if (args[0].startsWith("@r")) {
					String randomplayername = selectorprocessor.getRandom(sender, args[0].substring(2));
					if(randomplayername == null) {
						this.lang.sendMsg(sender, LangOptions.playmusic_unavilableselector_random);
						return true;
					}
					args[0] = randomplayername;
				}
				if(args[0].startsWith("@a")) {
					UUID[] targetarray = args[0].length() == 2 ? selectorprocessor.getAllGlobal() : selectorprocessor.getSameWorld(sender, args[0].substring(2)); 
					if(targetarray == null) {
						this.lang.sendMsg(sender, LangOptions.playmusic_unavilableselector_all);
						return true;
					}
					Consumer<Boolean> consumer = new Consumer<Boolean>() {
						@Override
						public void accept(Boolean success) {
							PlaymusicCommand.this.lang.sendMsg(sender, (success.booleanValue() ? LangOptions.playmusic_stop_success : LangOptions.playmusic_stop_fail));
						}
					};
					for(int i = targetarray.length; --i > -1;) {
						amusic.stopSound(targetarray[i], consumer);
					}
					return true;
				}
				
			}
			
			Player target = server.getPlayerExact(args[0]);
			if(target==null) {
				this.lang.sendMsg(sender, LangOptions.playmusic_targetoffline);
				return true;
			}
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
					args[0] = sender.getName();
				} else {
					this.lang.sendMsg(sender, LangOptions.playmusic_noconsoleselector);
					return true;
				}
			} else if(args[0].equals("@l") && (sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender)) {
				Player target = server.getPlayerExact(args[1]);
				if(target==null) {
					this.lang.sendMsg(sender, LangOptions.playmusic_targetoffline);
					return true;
				}
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
						sender.sendMessage(sb.toString());
					}
				};
				amusic.getResourcepackInfo(targetuuid, consumer);
				return true;
			} else if(permissions != null && !permissions.contains(AMusicPermission.LOADMUSIC_OTHER)) {
				this.lang.sendMsg(sender, LangOptions.playmusic_nopermissionother);
				return true;
			} else {
				if (args[0].startsWith("@p")) {
					String closestplayername = selectorprocessor.getNearest(sender, args[0].substring(2));
					
					if(closestplayername == null) {
						this.lang.sendMsg(sender, LangOptions.playmusic_unavilableselector_near);
						return true;
					}
					args[0] = closestplayername;
				}
				
				if (args[0].startsWith("@r")) {
					String randomplayername = selectorprocessor.getRandom(sender, args[0].substring(2));
					if(randomplayername == null) {
						this.lang.sendMsg(sender, LangOptions.playmusic_unavilableselector_random);
						return true;
					}
					args[0] = randomplayername;
				}
				if(args[0].startsWith("@a")) {
					UUID[] targetuuids = args[0].length() == 2 ? selectorprocessor.getAllGlobal() : selectorprocessor.getSameWorld(sender, args[0].substring(2)); 
					if(targetuuids == null) {
						this.lang.sendMsg(sender, LangOptions.playmusic_unavilableselector_all);
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
					String soundname = args[1];
					Consumer<Boolean> consumer = new Consumer<Boolean>() {
						private final Placeholder[] placeholders = new Placeholder[] {new Placeholder("%soundname%", soundname, true)};
						@Override
						public void accept(Boolean success) {
							PlaymusicCommand.this.lang.sendMsg(sender, success.booleanValue() ? LangOptions.playmusic_play_success : LangOptions.playmusic_play_fail, placeholders);
						}
					};
					for(UUID targetuuid : targetuuids) {
						amusic.playSound(targetuuid, soundname, consumer);
					}
					//this.lang.sendMsg(sender, LangOptions.playmusic_success, placeholders);
					return true;
				}
				
			}
			
			Player target = server.getPlayerExact(args[0]);
			if(target==null) {
				this.lang.sendMsg(sender, LangOptions.playmusic_targetoffline);
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
			return true;
		} else {
			PlaymusicCommand.this.lang.sendMsg(sender, LangOptions.playmusic_usage);
		}
		return true;
	}
	
	@Override
	public java.util.List<String> tabComplete(CommandSender sender, String alias, String[] args) throws CommandException, IllegalArgumentException {
		EnumSet<AMusicPermission> permissions = null;
		if(sender instanceof Player && (permissions = this.playerspermission.get(((Player) sender).getUniqueId())) == null) {
			return emptytab;
		}
		if (permissions != null && !permissions.contains(AMusicPermission.PLAYMUSIC)) {
			return emptytab;
		}
		if (args.length == 1) {
			ArrayList<String> tabcomplete = new ArrayList<String>();
			if (sender instanceof Player) {
				tabcomplete.add("@s");
			}
			if (permissions == null || permissions.contains(AMusicPermission.LOADMUSIC_OTHER)) {
				args[0] = args[0].toLowerCase();
				for (Player player : server.getOnlinePlayers()) {
					if (player.getName().toLowerCase().startsWith(args[0])) {
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
			Player target;
			if ((selfsender || permissions == null || permissions.contains(AMusicPermission.LOADMUSIC_OTHER)) && (target = server.getPlayerExact(args[0])) != null) {
				ArrayList<String> tabcomplete = new ArrayList<String>();
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
				UUID targetuuid = target.getUniqueId();
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
