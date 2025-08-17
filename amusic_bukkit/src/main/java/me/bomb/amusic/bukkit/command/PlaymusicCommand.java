package me.bomb.amusic.bukkit.command;

import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.util.LangOptions;
import me.bomb.amusic.util.LangOptions.Placeholder;

public final class PlaymusicCommand implements CommandExecutor {
	private final Server server;
	private final AMusic amusic;
	private final SelectorProcessor selectorprocessor;
	private final boolean trackable;
	public PlaymusicCommand(Server server, AMusic amusic, SelectorProcessor selectorprocessor, boolean trackable) {
		this.server = server;
		this.amusic = amusic;
		this.selectorprocessor = selectorprocessor;
		this.trackable = trackable;
	}
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(!sender.hasPermission("amusic.playmusic")) {
			LangOptions.playmusic_nopermission.sendMsg(sender);
			return true;
		}
		if(args.length==1) {
			if(args[0].equals("@s")) {
				if(sender instanceof Player) {
					args[0] = sender.getName();
				} else {
					LangOptions.playmusic_noconsoleselector.sendMsg(sender);
					return true;
				}
			} else if(!sender.hasPermission("amusic.playmusic.other")) {
				LangOptions.playmusic_nopermissionother.sendMsg(sender);
				return true;
			} else {
				if (args[0].startsWith("@p")) {
					String closestplayername = selectorprocessor.getNearest(sender, args[0].substring(2));
					
					if(closestplayername == null) {
						LangOptions.playmusic_unavilableselector_near.sendMsg(sender);
						return true;
					}
					args[0] = closestplayername;
				}
				
				if (args[0].startsWith("@r")) {
					String randomplayername = selectorprocessor.getRandom(sender, args[0].substring(2));
					if(randomplayername == null) {
						LangOptions.playmusic_unavilableselector_random.sendMsg(sender);
						return true;
					}
					args[0] = randomplayername;
				}
				if(args[0].startsWith("@a")) {
					UUID[] targetarray = args[0].length() == 2 ? selectorprocessor.getAllGlobal() : selectorprocessor.getSameWorld(sender, args[0].substring(2)); 
					if(targetarray == null) {
						LangOptions.playmusic_unavilableselector_all.sendMsg(sender);
						return true;
					}
					if(trackable) {
						for(int i = targetarray.length; --i > -1;) {
							amusic.stopSound(targetarray[i]);
						}
					} else {
						for(int i = targetarray.length; --i > -1;) {
							amusic.stopSoundUntrackable(targetarray[i]);
						}
					}
					LangOptions.playmusic_stop.sendMsg(sender);
					return true;
				}
				
			}
			
			Player target = server.getPlayerExact(args[0]);
			if(target==null) {
				LangOptions.playmusic_targetoffline.sendMsg(sender);
				return true;
			}
			if(trackable) {
				amusic.stopSound(target.getUniqueId());
			} else {
				amusic.stopSoundUntrackable(target.getUniqueId());
			}
			
			LangOptions.playmusic_stop.sendMsg(sender);
		} else if(args.length>1) {
			if(args[0].equals("@s")) {
				if(sender instanceof Player) {
					args[0] = sender.getName();
				} else {
					LangOptions.playmusic_noconsoleselector.sendMsg(sender);
					return true;
				}
			} else if(args[0].equals("@l") && (sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender)) {
				Player target = server.getPlayerExact(args[1]);
				if(target==null) {
					LangOptions.playmusic_targetoffline.sendMsg(sender);
					return true;
				}
				UUID targetuuid = target.getUniqueId();
				Consumer<String[]> consumer = new Consumer<String[]>() {
					@Override
					public void accept(String[] soundnames) {
						if(soundnames==null) {
							LangOptions.playmusic_noplaylist.sendMsg(sender);
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
				amusic.getPlaylistSoundnames(targetuuid, consumer);
				
				return true;
			} else if(!sender.hasPermission("amusic.playmusic.other")) {
				LangOptions.playmusic_nopermissionother.sendMsg(sender);
				return true;
			} else {
				if (args[0].startsWith("@p")) {
					String closestplayername = selectorprocessor.getNearest(sender, args[0].substring(2));
					
					if(closestplayername == null) {
						LangOptions.playmusic_unavilableselector_near.sendMsg(sender);
						return true;
					}
					args[0] = closestplayername;
				}
				
				if (args[0].startsWith("@r")) {
					String randomplayername = selectorprocessor.getRandom(sender, args[0].substring(2));
					if(randomplayername == null) {
						LangOptions.playmusic_unavilableselector_random.sendMsg(sender);
						return true;
					}
					args[0] = randomplayername;
				}
				if(args[0].startsWith("@a")) {
					UUID[] targetarray = args[0].length() == 2 ? selectorprocessor.getAllGlobal() : selectorprocessor.getSameWorld(sender, args[0].substring(2)); 
					if(targetarray == null) {
						LangOptions.playmusic_unavilableselector_all.sendMsg(sender);
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
					placeholders[0] = new Placeholder("%soundname%",args[1]);
					this.executeCommand(name, targetarray);
					LangOptions.playmusic_success.sendMsg(sender,placeholders);
					return true;
				}
				
			}
			
			Player target = server.getPlayerExact(args[0]);
			if(target==null) {
				LangOptions.playmusic_targetoffline.sendMsg(sender);
				return true;
			}
			Consumer<String[]> consumer = new Consumer<String[]>() {
				@Override
				public void accept(String[] soundnames) {
					if(soundnames==null) {
						LangOptions.playmusic_noplaylist.sendMsg(sender);
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
					placeholders[0] = new Placeholder("%soundname%",args[1]);
					for(String soundname : soundnames) {
						if(soundname.equals(args[1])) {
							executeCommand(args[1], target.getUniqueId());
							LangOptions.playmusic_success.sendMsg(sender,placeholders);
							return;
						}
					}
					LangOptions.playmusic_missingtrack.sendMsg(sender,placeholders);
				}
				
			};
			amusic.getPlaylistSoundnames(target.getUniqueId(), consumer);
			return true;
		} else {
			LangOptions.playmusic_usage.sendMsg(sender);
		}
		return true;
	}
	
	private void executeCommand(String soundname, UUID... targetuuids) {
		if(trackable) {
			for(UUID targetuuid : targetuuids) {
				amusic.playSound(targetuuid,soundname);
			}
		} else {
			for(UUID targetuuid : targetuuids) {
				amusic.playSoundUntrackable(targetuuid,soundname);
			}
		}
		
	}

}
