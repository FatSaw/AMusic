package me.bomb.amusic.velocity.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.util.LangOptions;
import me.bomb.amusic.util.LangOptions.Placeholder;

public final class PlaymusicCommand implements SimpleCommand  {

	private final ProxyServer server;
	private final AMusic amusic;
	private final boolean trackable;
	
	public PlaymusicCommand(ProxyServer server, AMusic amusic, boolean trackable) {
		this.server = server;
		this.amusic = amusic;
		this.trackable = trackable;
	}
	
	@Override
	public void execute(Invocation invocation) {
		CommandSource sender = invocation.source();
		if (!sender.hasPermission("amusic.playmusic")) {
			LangOptions.playmusic_nopermission.sendMsg(sender);
			return;
		}
		String[] args = invocation.arguments();
		if(args.length==1) {
			if(args[0].equals("@s")) {
				if(sender instanceof Player) {
					args[0] = ((Player) sender).getUsername();
				} else {
					LangOptions.playmusic_noconsoleselector.sendMsg(sender);
					return;
				}
			} else if(!sender.hasPermission("amusic.playmusic.other")) {
				LangOptions.playmusic_nopermissionother.sendMsg(sender);
				return;
			}
			Optional<Player> otarget = server.getPlayer(args[0]);
			if(otarget.isEmpty()) {
				LangOptions.playmusic_targetoffline.sendMsg(sender);
				return;
			}
			Player target = otarget.get();
			if(trackable) {
				amusic.stopSound(target.getUniqueId());
			} else {
				amusic.stopSoundUntrackable(target.getUniqueId());
			}
			LangOptions.playmusic_stop.sendMsg(sender);
		} else if(args.length>1) {
			if(args[0].equals("@s")) {
				if(sender instanceof Player) {
					args[0] = ((Player) sender).getUsername();
				} else {
					LangOptions.playmusic_noconsoleselector.sendMsg(sender);
					return;
				}
			} else if(args[0].equals("@l") && sender instanceof ConsoleCommandSource) {
				Optional<Player> otarget = server.getPlayer(args[1]);
				if(otarget.isEmpty()) {
					LangOptions.playmusic_targetoffline.sendMsg(sender);
					return;
				}
				Player target = otarget.get();
				UUID targetuuid = target.getUniqueId();
				String[] soundnames = amusic.getPlaylistSoundnames(targetuuid);
				if(soundnames==null) {
					LangOptions.playmusic_noplaylist.sendMsg(sender);
					return;
				}
				String playing = amusic.getPlayingSoundName(targetuuid);
				short playingsize = amusic.getPlayingSoundSize(targetuuid), playingstate = amusic.getPlayingSoundRemain(targetuuid);;
				
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
				sender.sendPlainMessage(sb.toString());
				return;
			}
			Optional<Player> otarget = server.getPlayer(args[0]);
			if(otarget.isEmpty()) {
				LangOptions.playmusic_targetoffline.sendMsg(sender);
				return;
			}
			Player target = otarget.get();
			String[] soundnames = amusic.getPlaylistSoundnames(target.getUniqueId());
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
					if(trackable) {
						amusic.playSound(target.getUniqueId(),args[1]);
					} else {
						amusic.playSoundUntrackable(target.getUniqueId(),args[1]);
					}
					LangOptions.playmusic_success.sendMsg(sender,placeholders);
					return;
				}
			}
			LangOptions.playmusic_missingtrack.sendMsg(sender,placeholders);
			return;
		} else {
			LangOptions.playmusic_usage.sendMsg(sender);
		}
		return;
	}
	
	@Override
	public List<String> suggest(Invocation invocation) {
		CommandSource sender = invocation.source();
		if (!sender.hasPermission("amusic.playmusic")) {
			return null;
		}
		String[] args = invocation.arguments();
		List<String> tabcomplete = new ArrayList<String>();
		if (args.length <= 1) {
			if (sender instanceof Player) {
				tabcomplete.add("@s");
			}
			if (sender.hasPermission("amusic.playmusic.other")) {
				for (Player player : server.getAllPlayers()) {
					if (player.getUsername().toLowerCase().startsWith(args[0].toLowerCase())) {
						tabcomplete.add(player.getUsername());
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
			if (selfsender || !(sender instanceof Player) || sender.hasPermission("amusic.playmusic.other")) {
				Optional<Player> otarget = server.getPlayer(args[0]);
				if(otarget.isEmpty()) {
					return null;
				}
				String[] soundnames = amusic.getPlaylistSoundnames(otarget.get().getUniqueId());
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
			}
		}
		return tabcomplete;
		
	}

}
