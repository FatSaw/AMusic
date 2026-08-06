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

import me.bomb.amusic.AMusic;
import me.bomb.amusic.permission.AMusicPermission;
import me.bomb.amusic.util.LangLoader;
import me.bomb.amusic.util.LangLoader.LangOptions;
import me.bomb.amusic.util.LangLoader.Placeholder;

public final class PlaymusicCommand implements SimpleCommand  {

	private final ProxyServer server;
	private final AMusic amusic;
	private final LangLoader lang;
	private final ConcurrentHashMap<UUID, EnumSet<AMusicPermission>> playerspermission;
	private final boolean trackable;
	private final ArrayList<String> emptytab = new ArrayList<String>(0);
	
	public PlaymusicCommand(ProxyServer server, AMusic amusic, LangLoader lang, ConcurrentHashMap<UUID, EnumSet<AMusicPermission>> playerspermission, boolean trackable) {
		this.server = server;
		this.amusic = amusic;
		this.lang = lang;
		this.playerspermission = playerspermission;
		this.trackable = trackable;
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
			if(trackable) {
				amusic.stopSound(target.getUniqueId());
			} else {
				amusic.stopSoundUntrackable(target.getUniqueId());
			}
			this.lang.sendMsg(sender, LangOptions.playmusic_stop);
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
												sender.sendPlainMessage(sb.toString());
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
				return;
			}
			Optional<Player> otarget = server.getPlayer(args[0]);
			if(otarget.isEmpty()) {
				this.lang.sendMsg(sender, LangOptions.playmusic_targetoffline);
				return;
			}
			Player target = otarget.get();
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
							if(trackable) {
								amusic.playSound(target.getUniqueId(),args[1]);
							} else {
								amusic.playSoundUntrackable(target.getUniqueId(),args[1],0d,0d,0d,1.0f,1.0f);
							}
							PlaymusicCommand.this.lang.sendMsg(sender, LangOptions.playmusic_success, placeholders);
							return;
						}
					}
					PlaymusicCommand.this.lang.sendMsg(sender, LangOptions.playmusic_missingtrack, placeholders);
				}
				
			};
			amusic.getPlaylistSoundnames(target.getUniqueId(), false, consumer);
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
			boolean selfsender = false;
			if (args[0].equals("@s") && sender instanceof Player) {
				args[0] = ((Player)sender).getUsername();
				selfsender = true;
			}
			if (selfsender || permissions == null || permissions.contains(AMusicPermission.LOADMUSIC_OTHER)) {
				Optional<Player> otarget = server.getPlayer(args[0]);
				if(otarget.isEmpty()) {
					return null;
				}
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
				boolean async = amusic.getPlaylistSoundnames(otarget.get().getUniqueId(), true, consumer);
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
		return tabcomplete;
		
	}

}
