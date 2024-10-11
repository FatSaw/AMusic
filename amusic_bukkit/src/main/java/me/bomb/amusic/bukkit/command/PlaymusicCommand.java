package me.bomb.amusic.bukkit.command;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
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
	private final Random random;
	private final boolean trackable;
	public PlaymusicCommand(PositionTracker positiontracker, SelectorProcessor selectorprocessor, Random random, boolean trackable) {
		this.positiontracker = positiontracker;
		this.selectorprocessor = selectorprocessor;
		this.random = random;
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
			}
			
			if (args[0].equals("@p")) {
				Location executorlocation = null;
				Player senderplayer = null;
				if (sender instanceof BlockCommandSender) {
					BlockCommandSender commandblocksender = (BlockCommandSender) sender;
					executorlocation = commandblocksender.getBlock().getLocation();
				} else if (sender instanceof Player) {
					senderplayer = (Player) sender;
					executorlocation = senderplayer.getLocation();
				}
				if(executorlocation == null) {
					LangOptions.playmusic_unavilableselector_near.sendMsg(sender);
					return true;
				}
				List<Player> players = executorlocation.getWorld().getPlayers();
				Player closestplayer = null;
				double mindistance = Double.MAX_VALUE;
				for(Player player : players) {
					double distance = executorlocation.distance(player.getLocation());
					if(player == senderplayer || distance > mindistance) {
						continue;
					}
					mindistance = distance;
					closestplayer = player;
				}
				args[0] = closestplayer.getName();
			}
			if (args[0].equals("@r")) {
				Location executorlocation = null;
				Player senderplayer = null;
				if (sender instanceof BlockCommandSender) {
					BlockCommandSender commandblocksender = (BlockCommandSender) sender;
					executorlocation = commandblocksender.getBlock().getLocation();
				} else if (sender instanceof Player) {
					senderplayer = (Player) sender;
					executorlocation = senderplayer.getLocation();
				}
				if(executorlocation == null) {
					LangOptions.playmusic_unavilableselector_random.sendMsg(sender);
					return true;
				}
				List<Player> players = executorlocation.getWorld().getPlayers();
				int index = random.nextInt(players.size());
				Player randomplayer = players.get(index);
				args[0] = randomplayer.getName();
			}
			if (args[0].equals("@a")) {
				Location executorlocation = null;
				Player senderplayer = null;
				if (sender instanceof BlockCommandSender) {
					BlockCommandSender commandblocksender = (BlockCommandSender) sender;
					executorlocation = commandblocksender.getBlock().getLocation();
				} else if (sender instanceof Player) {
					senderplayer = (Player) sender;
					executorlocation = senderplayer.getLocation();
				}
				if(executorlocation == null) {
					LangOptions.playmusic_unavilableselector_all.sendMsg(sender);
					return true;
				}
				List<Player> players = executorlocation.getWorld().getPlayers();
				if(trackable) {
					for(int i = players.size(); --i > -1;) {
						positiontracker.stopMusic(players.get(i).getUniqueId());
					}
				} else {
					for(int i = players.size(); --i > -1;) {
						positiontracker.stopMusicUntrackable(players.get(i).getUniqueId());
					}
				}
				
				return true;
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
			}
			
			if (args[0].equals("@p")) {
				Location executorlocation = null;
				Player senderplayer = null;
				if (sender instanceof BlockCommandSender) {
					BlockCommandSender commandblocksender = (BlockCommandSender) sender;
					executorlocation = commandblocksender.getBlock().getLocation();
				} else if (sender instanceof Player) {
					senderplayer = (Player) sender;
					executorlocation = senderplayer.getLocation();
				}
				if(executorlocation == null) {
					LangOptions.playmusic_unavilableselector_near.sendMsg(sender);
					return true;
				}
				List<Player> players = executorlocation.getWorld().getPlayers();
				Player closestplayer = null;
				double mindistance = Double.MAX_VALUE;
				for(Player player : players) {
					double distance = executorlocation.distance(player.getLocation());
					if(player == senderplayer || distance > mindistance) {
						continue;
					}
					mindistance = distance;
					closestplayer = player;
				}
				args[0] = closestplayer.getName();
			}
			if (args[0].equals("@r")) {
				Location executorlocation = null;
				Player senderplayer = null;
				if (sender instanceof BlockCommandSender) {
					BlockCommandSender commandblocksender = (BlockCommandSender) sender;
					executorlocation = commandblocksender.getBlock().getLocation();
				} else if (sender instanceof Player) {
					senderplayer = (Player) sender;
					executorlocation = senderplayer.getLocation();
				}
				if(executorlocation == null) {
					LangOptions.playmusic_unavilableselector_random.sendMsg(sender);
					return true;
				}
				List<Player> players = executorlocation.getWorld().getPlayers();
				int index = random.nextInt(players.size());
				Player randomplayer = players.get(index);
				args[0] = randomplayer.getName();
			}
			if (args[0].equals("@a")) {
				Location executorlocation = null;
				Player senderplayer = null;
				if (sender instanceof BlockCommandSender) {
					BlockCommandSender commandblocksender = (BlockCommandSender) sender;
					executorlocation = commandblocksender.getBlock().getLocation();
				} else if (sender instanceof Player) {
					senderplayer = (Player) sender;
					executorlocation = senderplayer.getLocation();
				}
				if(executorlocation == null) {
					LangOptions.playmusic_unavilableselector_all.sendMsg(sender);
					return true;
				}
				List<Player> players = executorlocation.getWorld().getPlayers();
				UUID[] targetarray = new UUID[players.size()];
				for(int i = players.size(); --i > -1;) {
					targetarray[i] = players.get(i).getUniqueId();
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
