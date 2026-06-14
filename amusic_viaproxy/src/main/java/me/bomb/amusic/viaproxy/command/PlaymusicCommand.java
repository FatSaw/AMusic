package me.bomb.amusic.viaproxy.command;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;

import com.viaversion.viaversion.api.connection.UserConnection;

import me.bomb.amusic.AMusic;

public final class PlaymusicCommand implements Command {
	
	private final AMusic amusic;
	private final ConcurrentHashMap<String, UUID> uuidByPlayername;
	private final boolean trackable;
	
	public PlaymusicCommand(AMusic amusic, ConcurrentHashMap<String, UUID> uuidByPlayername, boolean trackable) {
		this.amusic = amusic;
		this.uuidByPlayername = uuidByPlayername;
		this.trackable = trackable;
	}

	@Override
	public void handleConsole(Logger logger, String[] args) {
		if(args.length==1) {
			if(args[0].equals("@s")) {
				logger.info("This selector unavilable from console");
				return;
			}
			UUID targetuuid = uuidByPlayername.get(args[0]);
			if(targetuuid == null) {
				logger.info("Target player offline");
				return;
			}
			if(trackable) {
				amusic.stopSound(targetuuid);
			} else {
				amusic.stopSoundUntrackable(targetuuid);
			}
			logger.info("Stopping playing...");
		} else if(args.length>1) {
			if(args[0].equals("@s")) {
				logger.info("This selector unavilable from console");
				return;
			} else if(args[0].equals("@l")) {
				UUID targetuuid = uuidByPlayername.get(args[1]);
				if(targetuuid == null) {
					logger.info("Target player offline");
					return;
				}
				Consumer<String[]> consumer = new Consumer<String[]>() {
					@Override
					public void accept(String[] soundnames) {
						if(soundnames==null) {
							logger.info("Playlist not loaded fot that player");
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
												logger.info(sb.toString());
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
			UUID targetuuid = uuidByPlayername.get(args[0]);
			if(targetuuid == null) {
				logger.info("Target player offline");
				return;
			}
			Consumer<String[]> consumer = new Consumer<String[]>() {

				@Override
				public void accept(String[] soundnames) {
					if(soundnames==null) {
						logger.info("Playlist not loaded fot that player");
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
					for(String soundname : soundnames) {
						if(soundname.equals(args[1])) {
							if(trackable) {
								amusic.playSound(targetuuid,args[1]);
							} else {
								amusic.playSoundUntrackable(targetuuid,args[1],0d,0d,0d,1.0f,1.0f);
							}
							logger.info("Starting playing... ".concat(args[1]));
							return;
						}
					}
					logger.info("Sound ".concat(args[1]).concat(" not exist"));
				}
				
			};
			amusic.getPlaylistSoundnames(targetuuid, false, consumer);
			return;
		} else {
			logger.info("Usage: playmusic <player> [soundname]");
		}
		return;
	}

	@Override
	public void handlePlayer(UserConnection connection, String[] args) {
		
	}

}
