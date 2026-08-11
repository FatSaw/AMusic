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
import me.bomb.amusic.util.LangLoader;
import me.bomb.amusic.util.LangLoader.LangOptions;
import me.bomb.amusic.util.LangLoader.Placeholder;

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
					for(int i = targetarray.length; --i > -1;) {
						amusic.stopSound(targetarray[i]);
					}
					this.lang.sendMsg(sender, LangOptions.playmusic_stop);
					return true;
				}
				
			}
			
			Player target = server.getPlayerExact(args[0]);
			if(target==null) {
				this.lang.sendMsg(sender, LangOptions.playmusic_targetoffline);
				return true;
			}
			amusic.stopSound(target.getUniqueId());
			this.lang.sendMsg(sender, LangOptions.playmusic_stop);
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
				Consumer<String[]> consumer = new Consumer<String[]>() {
					@Override
					public void accept(String[] soundnames) {
						if(soundnames==null) {
							PlaymusicCommand.this.lang.sendMsg(sender, LangOptions.playmusic_noplaylist);
							return;
						}
						Consumer<String> consumerSoundName = new Consumer<String>() {

							@Override
							public void accept(String playing) {
								Consumer<Short> consumerSoundSize = new Consumer<Short>() {
									@Override
									public void accept(Short playingsize) {
										Consumer<Short> consumerSoundRemain = new Consumer<Short>() {
											@Override
											public void accept(Short playingstate) {
												StringBuilder sb = new StringBuilder();
												if(playing!=null) {
													sb.append("Playing: ");
													sb.append(playing);
													sb.append(' ');
												}
												if(playingsize!=-1&&playingstate!=-1) {
													playingstate=(short) (playingsize-playingstate);
													sb.append(Short.toString(playingstate));
													sb.append('/');
													sb.append(Short.toString(playingsize));
													sb.append(' ');
												}
												sb.append("Sounds: ");
												for(String soundname : soundnames) {
													sb.append(soundname);
													sb.append(' ');
												}
												sender.sendMessage(sb.toString());
											}
										};
										amusic.getPlayingSoundRemain(targetuuid, consumerSoundRemain);
									}
								};
								amusic.getPlayingSoundSize(targetuuid, consumerSoundSize);
							}
							
						};
						amusic.getPlayingSoundName(targetuuid, consumerSoundName);
					}
				};
				amusic.getPlaylistSoundnames(targetuuid, false, consumer);
				
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
					UUID[] targetarray = args[0].length() == 2 ? selectorprocessor.getAllGlobal() : selectorprocessor.getSameWorld(sender, args[0].substring(2)); 
					if(targetarray == null) {
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
					String name = args[1];

					Placeholder[] placeholders = new Placeholder[1];
					placeholders[0] = new Placeholder("%soundname%",args[1],true);
					this.executeCommand(name, targetarray);

					this.lang.sendMsg(sender, LangOptions.playmusic_success, placeholders);
					return true;
				}
				
			}
			
			Player target = server.getPlayerExact(args[0]);
			if(target==null) {
				this.lang.sendMsg(sender, LangOptions.playmusic_targetoffline);
				return true;
			}
			Consumer<String[]> consumer = new Consumer<String[]>() {
				@Override
				public void accept(String[] soundnames) {
					if(soundnames==null) {
						PlaymusicCommand.this.lang.sendMsg(sender, LangOptions.playmusic_noplaylist);
						return;
					}
					if(args.length>2) {
						StringBuilder sb = new StringBuilder(args[1]);
						for(int i = 2;i < args.length;++i) {
							sb.append(' ');
							sb.append(args[i]);
						}
						args[1] = sb.toString();
					}
					Placeholder[] placeholders = new Placeholder[1];
					placeholders[0] = new Placeholder("%soundname%",args[1],true);
					for(String soundname : soundnames) {
						if(soundname.equals(args[1])) {
							executeCommand(args[1], target.getUniqueId());
							PlaymusicCommand.this.lang.sendMsg(sender, LangOptions.playmusic_success, placeholders);
							return;
						}
					}
					PlaymusicCommand.this.lang.sendMsg(sender, LangOptions.playmusic_missingtrack, placeholders);
				}
				
			};
			amusic.getPlaylistSoundnames(target.getUniqueId(), false, consumer);
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
		if (args.length > 1 && !args[0].equals("@l") && !args[0].equals("@p") && !args[0].equals("@r") && !args[0].equals("@a")) {
			boolean selfsender = false;
			if (args[0].equals("@s") && sender instanceof Player) {
				args[0] = sender.getName();
				selfsender = true;
			}
			if (selfsender || permissions == null || permissions.contains(AMusicPermission.LOADMUSIC_OTHER)) {
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
	
	private void executeCommand(String soundname, UUID... targetuuids) {
		for(UUID targetuuid : targetuuids) {
			amusic.playSound(targetuuid,soundname);
		}
	}

}
