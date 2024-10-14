package me.bomb.amusic.bukkit.command;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;

import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.SoundInfo;
import me.bomb.amusic.bukkit.command.LangOptions.Placeholders;

public final class PlaymusicCommand implements CommandExecutor {
	private final PositionTracker positiontracker;
	private final SelectorProcessor selectorprocessor;
	private final boolean trackable;
	public PlaymusicCommand(PositionTracker positiontracker, SelectorProcessor selectorprocessor, boolean trackable) {
		this.positiontracker = positiontracker;
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
				if (args[0].equals("@a")) {
					Collection<? extends Player> players = Bukkit.getOnlinePlayers();
					int i = players.size();
					UUID[] targetarray = new UUID[i];
					Iterator<? extends Player> playersiterator = players.iterator();
					while(playersiterator.hasNext()) {
						targetarray[--i] = playersiterator.next().getUniqueId();
					}
					if(trackable) {
						for(i = targetarray.length; --i > -1;) {
							positiontracker.stopMusic(targetarray[i]);
						}
					} else {
						for(i = targetarray.length; --i > -1;) {
							positiontracker.stopMusicUntrackable(targetarray[i]);
						}
					}
					
					return true;
				} else if (args[0].startsWith("@a")) {
					UUID[] targetarray = selectorprocessor.getSameWorld(sender, args[0].substring(2));
					if(targetarray == null) {
						LangOptions.playmusic_unavilableselector_all.sendMsg(sender);
						return true;
					}
					if(trackable) {
						for(int i = targetarray.length; --i > -1;) {
							positiontracker.stopMusic(targetarray[i]);
						}
					} else {
						for(int i = targetarray.length; --i > -1;) {
							positiontracker.stopMusicUntrackable(targetarray[i]);
						}
					}
					return true;
				}
			}
			
			Player target = Bukkit.getPlayerExact(args[0]);
			if(target==null) {
				LangOptions.playmusic_targetoffline.sendMsg(sender);
				return true;
			}
			if(trackable) {
				positiontracker.stopMusic(target.getUniqueId());
			} else {
				positiontracker.stopMusicUntrackable(target.getUniqueId());
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
				Player target = Bukkit.getPlayerExact(args[1]);
				if(target==null) {
					LangOptions.playmusic_targetoffline.sendMsg(sender);
					return true;
				}
				UUID targetuuid = target.getUniqueId();
				List<SoundInfo> soundsinfo = positiontracker.getSoundInfo(targetuuid);
				if(soundsinfo==null) {
					LangOptions.playmusic_noplaylist.sendMsg(sender);
					return true;
				}
				String playing = positiontracker.getPlaying(targetuuid);
				short playingsize = positiontracker.getPlayingSize(targetuuid), playingstate = positiontracker.getPlayingRemain(targetuuid);;
				
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
				for(SoundInfo soundinfo : soundsinfo) {
					sb.append(soundinfo.name);
					sb.append(' ');
				}
				sender.sendMessage(sb.toString());
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
				if (args[0].equals("@a")) {
					Collection<? extends Player> players = Bukkit.getOnlinePlayers();
					int i = players.size();
					UUID[] targetarray = new UUID[i];
					Iterator<? extends Player> playersiterator = players.iterator();
					while(playersiterator.hasNext()) {
						targetarray[--i] = playersiterator.next().getUniqueId();
					}
					
					if(args.length>2) {
						StringBuilder sb = new StringBuilder(args[1]);
						for(i = 2;i < args.length;++i) {
							sb.append(' ');
							sb.append(args[i]);
						}
						args[1] = sb.toString();
					}
					String name = args[1];
					
					this.executeCommand(name, targetarray);
					return true;
				} else if (args[0].startsWith("@a")) {
					UUID[] targetarray = selectorprocessor.getSameWorld(sender, args[0].substring(2));
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
					
					this.executeCommand(name, targetarray);
					return true;
				}
			}
			
			Player target = Bukkit.getPlayerExact(args[0]);
			if(target==null) {
				LangOptions.playmusic_targetoffline.sendMsg(sender);
				return true;
			}
			List<SoundInfo> soundsinfo = positiontracker.getSoundInfo(target.getUniqueId());
			if(soundsinfo==null) {
				LangOptions.playmusic_noplaylist.sendMsg(sender);
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
			Placeholders[] placeholders = new Placeholders[1];
			placeholders[0] = new Placeholders("%soundname%",args[1]);
			for(SoundInfo soundinfo : soundsinfo) {
				if(soundinfo.name.equals(args[1])) {
					executeCommand(args[1], target.getUniqueId());
					LangOptions.playmusic_success.sendMsg(sender,placeholders);
					return true;
				}
			}
			LangOptions.playmusic_missingtrack.sendMsg(sender,placeholders);
			return true;
		} else {
			LangOptions.playmusic_usage.sendMsg(sender);
		}
		return true;
	}
	
	private void executeCommand(String soundname, UUID... targetuuids) {
		if(trackable) {
			for(UUID targetuuid : targetuuids) {
				positiontracker.playMusic(targetuuid,soundname);
			}
		} else {
			for(UUID targetuuid : targetuuids) {
				positiontracker.playMusicUntrackable(targetuuid,soundname);
			}
		}
		
	}

}
