package me.bomb.amusic.sponge.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Server;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.command.source.RconSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.util.LangOptions;
import me.bomb.amusic.util.LangOptions.Placeholder;

public final class PlaymusicCommand implements CommandCallable {
	private final Server server;
	private final AMusic amusic;
	private final boolean trackable;
	public PlaymusicCommand(Server server, AMusic amusic, boolean trackable) {
		this.server = server;
		this.amusic = amusic;
		this.trackable = trackable;
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
	
	@Override
	public CommandResult process(CommandSource source, String arguments) throws CommandException {
		String[] args = arguments.split(" ", 127);
		if(!source.hasPermission("amusic.playmusic")) {
			LangOptions.playmusic_nopermission.sendMsg(source);
			return CommandResult.success();
		}
		if(args.length==1) {
			if(args[0].equals("@s")) {
				if(source instanceof Player) {
					args[0] = source.getName();
				} else {
					LangOptions.playmusic_noconsoleselector.sendMsg(source);
					return CommandResult.success();
				}
			} else if(!source.hasPermission("amusic.playmusic.other")) {
				LangOptions.playmusic_nopermissionother.sendMsg(source);
				return CommandResult.success();
			}
			
			Optional<Player> otarget = server.getPlayer(args[0]);
			if(!otarget.isPresent()) {
				LangOptions.playmusic_targetoffline.sendMsg(source);
				return CommandResult.success();
			}
			Player target = otarget.get();
			if(trackable) {
				amusic.stopSound(target.getUniqueId());
			} else {
				amusic.stopSoundUntrackable(target.getUniqueId());
			}
			
			LangOptions.playmusic_stop.sendMsg(source);
		} else if(args.length>1) {
			if(args[0].equals("@s")) {
				if(source instanceof Player) {
					args[0] = source.getName();
				} else {
					LangOptions.playmusic_noconsoleselector.sendMsg(source);
					return CommandResult.success();
				}
			} else if(args[0].equals("@l") && (source instanceof ConsoleSource || source instanceof RconSource)) {
				Optional<Player> otarget = server.getPlayer(args[1]);
				if(!otarget.isPresent()) {
					LangOptions.playmusic_targetoffline.sendMsg(source);
					return CommandResult.success();
				}
				Player target = otarget.get();
				UUID targetuuid = target.getUniqueId();
				String[] soundnames = amusic.getPlaylistSoundnames(targetuuid);
				if(soundnames==null) {
					LangOptions.playmusic_noplaylist.sendMsg(source);
					return CommandResult.success();
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
				source.sendMessage(Text.of(sb.toString()));
				return CommandResult.success();
			} else if(!source.hasPermission("amusic.playmusic.other")) {
				LangOptions.playmusic_nopermissionother.sendMsg(source);
				return CommandResult.success();
			}
			
			Optional<Player> otarget = server.getPlayer(args[0]);
			if(!otarget.isPresent()) {
				LangOptions.playmusic_targetoffline.sendMsg(source);
				return CommandResult.success();
			}
			Player target = otarget.get();
			String[] soundnames = amusic.getPlaylistSoundnames(target.getUniqueId());
			if(soundnames==null) {
				LangOptions.playmusic_noplaylist.sendMsg(source);
				return CommandResult.success();
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
					LangOptions.playmusic_success.sendMsg(source,placeholders);
					return CommandResult.success();
				}
			}
			LangOptions.playmusic_missingtrack.sendMsg(source,placeholders);
			return CommandResult.success();
		} else {
			LangOptions.playmusic_usage.sendMsg(source);
		}
		return CommandResult.success();
	}
	@Override
	public List<String> getSuggestions(CommandSource source, String arguments, Location<World> targetPosition) throws CommandException {
		String[] args = arguments.split(" ", 127);
		ArrayList<String> tabcomplete = new ArrayList<String>();
		if (!source.hasPermission("amusic.playmusic")) {
			return null;
		}
		if (args.length == 1) {
			if (source instanceof Player) {
				tabcomplete.add("@s");
			}
			if (source.hasPermission("amusic.playmusic.other")) {
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
			if (args[0].equals("@s") && source instanceof Player) {
				args[0] = source.getName();
				selfsender = true;
			}
			if (selfsender || !(source instanceof Player) || source.hasPermission("amusic.playmusic.other")) {
				Optional<Player> otarget = server.getPlayer(args[0]);
				if (otarget.isPresent()) {
					Player target = otarget.get();
					String[] soundnames = amusic.getPlaylistSoundnames(target.getUniqueId());
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
								if (soundname.startsWith(args[1]) && soundname.indexOf('§') == -1) {
									tabcomplete.add(soundname);
								}
							}
						} else {
							for (String soundname : soundnames) {
								if (lastspace < soundname.length() && soundname.startsWith(args[1]) && soundname.indexOf('§') == -1) {
									soundname = soundname.substring(lastspace);
									tabcomplete.add(soundname);
								}
							}
						}
					}
				}
			}
		}
		return tabcomplete;
	}
	@Override
	public boolean testPermission(CommandSource source) {
		return false;
	}
	@Override
	public Optional<Text> getShortDescription(CommandSource source) {
		return Optional.empty();
	}
	@Override
	public Optional<Text> getHelp(CommandSource source) {
		return Optional.empty();
	}
	@Override
	public Text getUsage(CommandSource source) {
		return null;
	}

}
