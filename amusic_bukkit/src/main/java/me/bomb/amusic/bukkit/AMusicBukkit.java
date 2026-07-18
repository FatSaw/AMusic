package me.bomb.amusic.bukkit;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.Configuration;
import me.bomb.amusic.GeyserHook;
import me.bomb.amusic.LocalAMusic;
import me.bomb.amusic.MessageSender;
import me.bomb.amusic.PackSender;
import me.bomb.amusic.SoundStarter;
import me.bomb.amusic.ClientAMusic;
import me.bomb.amusic.SoundStopper;
import me.bomb.amusic.util.AMusicLogger;
import me.bomb.amusic.util.LangOptions;
import me.bomb.amusic.bukkit.command.LoadmusicCommand;
import me.bomb.amusic.bukkit.command.PlaymusicCommand;
import me.bomb.amusic.bukkit.command.RepeatCommand;
import me.bomb.amusic.bukkit.command.SelectorProcessor;
import me.bomb.amusic.bukkit.command.UploadmusicCommand;
import me.bomb.amusic.bukkit.event.PlayerChangedWorldHandler;
import me.bomb.amusic.bukkit.event.PlayerJoinHandler;
import me.bomb.amusic.bukkit.event.PlayerQuitHandler;
import me.bomb.amusic.bukkit.event.PlayerResourcePackStatusHandler;
import me.bomb.amusic.bukkit.event.PlayerRespawnHandler;
import me.bomb.amusic.bukkit.legacy.LegacyMessageSender_1_7_R4;
import me.bomb.amusic.bukkit.legacy.LegacyMessageSender_1_8_R3;
import me.bomb.amusic.bukkit.legacy.LegacyMessageSender_1_9_R2;
import me.bomb.amusic.bukkit.legacy.LegacyMessageSender_1_10_R1;
import me.bomb.amusic.bukkit.legacy.LegacyMessageSender_1_11_R1;
import me.bomb.amusic.bukkit.legacy.LegacyPackSender_1_7_R4;
import me.bomb.amusic.bukkit.legacy.LegacyPackSender_1_8_R3;
import me.bomb.amusic.bukkit.legacy.LegacyPackSender_1_9_R2;
import me.bomb.amusic.bukkit.legacy.LegacyPackSender_1_10_R1;
//import me.bomb.amusic.bukkit.legacy.LegacySoundStopper_1_7_R4;
//import me.bomb.amusic.bukkit.legacy.LegacySoundStopper_1_8_R3;
//import me.bomb.amusic.bukkit.legacy.LegacySoundStarter_1_9_R2;
//import me.bomb.amusic.bukkit.legacy.LegacySoundStarter_1_10_R1;
import me.bomb.amusic.bukkit.legacy.LegacySoundStopper_1_9_R2;
import me.bomb.amusic.bukkit.legacy.LegacySoundStopper_1_10_R1;
import me.bomb.amusic.source.LocalConvertedSource;
import me.bomb.amusic.source.LocalUnconvertedSource;
import me.bomb.amusic.source.MusicdirFStaticPackSource;
import me.bomb.amusic.source.MusicdirPackSource;
import me.bomb.amusic.source.SoundSource;
import me.bomb.amusic.source.PackSource;
import me.bomb.amusic.source.StaticPackSource;


public final class AMusicBukkit extends JavaPlugin {
	
	private static AMusic instance = null;
	
	private final AMusic amusic;
	private final ConcurrentHashMap<Object,InetAddress> playerips;
	private final boolean usecmd;
	private GeyserHook geyserhook = null;
	
	private final SimpleCommandMap commandmap;
	private final HashMap<String, Command> mapcommand;
	
	private final Command loadmusiccmd, playmusiccmd, playmusicuntrackablecmd, repeatcmd, uploadmusiccmd;
	
	private final PlayerJoinHandler playerjoin;
	private final PlayerQuitHandler playerquit;
	private final PlayerChangedWorldHandler playerchangedworld;
	private final PlayerRespawnHandler playerrespawn;
	private final PlayerResourcePackStatusHandler playerresourcepackstatus;

	public AMusicBukkit() {
		me.bomb.amusic.util.Logger logger = new me.bomb.amusic.util.Logger() {
			java.util.logging.Logger logger = AMusicBukkit.this.getLogger();
			@Override
			public void warn(String msg) {
				logger.warning(msg);
			}
			
			@Override
			public void info(String msg) {
				logger.info(msg);
			}
			
			@Override
			public void error(String msg) {
				logger.severe(msg);
			}
		};
		AMusicLogger.setLogger(logger);
		final Server server = this.getServer();
		byte ver = 127;
		try {
			String nmsversion = server.getClass().getPackage().getName().substring(23);
			ver = Byte.valueOf(nmsversion.split("_", 3)[1]);
		} catch (StringIndexOutOfBoundsException | NumberFormatException e) {
		}
		
		Path plugindir = this.getDataFolder().toPath(), configfile = plugindir.resolve("config.yml"), langfile = plugindir.resolve("lang.yml"), defaultresourcepackfile = plugindir.resolve("resourcepack.zip"), musicdir = plugindir.resolve("Music"), packeddir = plugindir.resolve("Packed");
		FileSystem fs = plugindir.getFileSystem();
		FileSystemProvider fsp = fs.provider();
		try {
			fsp.createDirectory(plugindir);
		} catch (IOException e) {
		}
		boolean waitacception = ver == 7 ? false : true;
		Configuration config = new Configuration(plugindir.getFileSystem(), configfile, musicdir, packeddir, waitacception, true);
		String configerrors = config.errors;
		if(!configerrors.isEmpty()) {
			throw new IllegalStateException("AMusic config initialization errors: \n".concat(configerrors));
		}
		SimpleCommandMap commandmap = null;
		HashMap<String, Command> mapcommand = null;
		LoadmusicCommand loadmusiccmd = null;
		PlaymusicCommand playmusiccmd = null;
		PlaymusicCommand playmusicuntrackablecmd = null;
		RepeatCommand repeatcmd = null;
		UploadmusicCommand uploadmusiccmd = null;
		if(config.use) {
			try {
				fsp.createDirectory(musicdir);
			} catch (IOException e) {
			}
			try {
				fsp.createDirectory(packeddir);
			} catch (IOException e) {
			}
			this.usecmd = config.usecmd;
			if(this.usecmd) {
				try {
					{
						PluginManager pluginmanager = server.getPluginManager();
						Field field = pluginmanager.getClass().getDeclaredField("commandMap");
						field.setAccessible(true);
						commandmap = (SimpleCommandMap) field.get(pluginmanager);
					}
					try {
						Method method = commandmap.getClass().getDeclaredMethod("getKnownCommands");
						mapcommand = (HashMap<String, Command>) method.invoke(commandmap);
					} catch (NoSuchMethodException | InvocationTargetException | SecurityException | IllegalArgumentException | IllegalAccessException e2) {
						try {
							Field field = commandmap.getClass().getDeclaredField("knownCommands");
							field.setAccessible(true);
							mapcommand = (HashMap<String, Command>) field.get(commandmap);
						} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e3) {
							e3.printStackTrace();
						}
						e2.printStackTrace();
					}
				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e1) {
					e1.printStackTrace();
				}
				
			}
			
			if(config.connectuse) {
				this.playerips = null;
				ClientAMusic amusic = new ClientAMusic(config);
				this.amusic = amusic;
				this.playerjoin = null;
				this.playerquit = null;
				this.playerchangedworld = null;
				this.playerrespawn = null;
				this.playerresourcepackstatus = null;
				if(this.usecmd) {
					SelectorProcessor selectorprocessor = new SelectorProcessor(server, new Random());
					loadmusiccmd = new LoadmusicCommand(server, amusic, selectorprocessor);
					playmusiccmd = new PlaymusicCommand(server, amusic, selectorprocessor, true);
					playmusicuntrackablecmd = new PlaymusicCommand(server, amusic, selectorprocessor, false);
					repeatcmd = new RepeatCommand(server, amusic, selectorprocessor);
					uploadmusiccmd = new UploadmusicCommand(amusic, config.uploadhost);
				}
			} else {
				PackSender packsender;
				SoundStarter soundstarter;
				SoundStopper soundstopper;
				switch (ver) {
				case 7:
					packsender = new LegacyPackSender_1_7_R4(server);
					soundstarter = new BukkitLegacySoundStarter(server);
					//soundstopper = new LegacySoundStopper_1_7_R4(server);
					soundstopper = new BukkitSoundSilenceLockStopper(server);
				break;
				case 8:
					packsender = new LegacyPackSender_1_8_R3(server);
					soundstarter = new BukkitLegacySoundStarter(server);
					//soundstopper = new LegacySoundStopper_1_8_R3(server);
					soundstopper = new BukkitSoundSilenceLockStopper(server);
				break;
				case 9:
					packsender = new LegacyPackSender_1_9_R2(server);
					//soundstarter = new LegacySoundStarter_1_9_R2(server);
					soundstarter = new BukkitLegacySoundStarter(server);
					soundstopper = new LegacySoundStopper_1_9_R2(server);
				break;
				case 10:
					packsender = new LegacyPackSender_1_10_R1(server);
					//soundstarter = new LegacySoundStarter_1_10_R1(server);
					soundstarter = new BukkitLegacySoundStarter(server);
					soundstopper = new LegacySoundStopper_1_10_R1(server);
				break;
				case 11: case 12:
					packsender = new BukkitPackSender(server);
					soundstarter = new BukkitLegacySoundStarter(server);
					soundstopper = new BukkitLegacySoundStopper(server);
				break;
				default:
					packsender = new BukkitPackSender(server);
					soundstarter = new BukkitSoundStarter(server);
					soundstopper = new BukkitSoundStopper(server);
				break;
				}
				waitacception = config.waitacception;
				playerips = config.sendpackstrictaccess || config.uploadstrictaccess ? new ConcurrentHashMap<Object,InetAddress>(16,0.75f,1) : null;
				Runtime runtime = Runtime.getRuntime();
				SoundSource soundsource = config.encoderuse ? new LocalUnconvertedSource(runtime, config.musicdir, config.packsizelimit, config.encoderbinary, config.encoderbitrate, config.encoderchannels, config.encodersamplingrate, config.packthreadcoefficient, config.packthreadlimitcount) : new LocalConvertedSource(config.musicdir, config.packsizelimit, config.packthreadcoefficient, config.packthreadlimitcount);
				PackSource packsource = new MusicdirFStaticPackSource(new MusicdirPackSource(musicdir, config.packsizelimit), new StaticPackSource(defaultresourcepackfile, config.packsizelimit));
				LocalAMusic amusic = new LocalAMusic(logger, config, soundsource, packsource, packsender, soundstarter, soundstopper, playerips == null ? null : playerips.values());
				this.amusic = amusic;
				if(this.usecmd) {
					SelectorProcessor selectorprocessor = new SelectorProcessor(server, new Random());
					loadmusiccmd = new LoadmusicCommand(server, amusic, selectorprocessor);
					playmusiccmd = new PlaymusicCommand(server, amusic, selectorprocessor, true);
					playmusicuntrackablecmd = new PlaymusicCommand(server, amusic, selectorprocessor, false);
					repeatcmd = new RepeatCommand(server, amusic, selectorprocessor);
					uploadmusiccmd = new UploadmusicCommand(amusic, config.uploadhost);
				}
				PlayerJoinHandler playerjoin = null;
				PlayerQuitHandler playerquit = null;
				PlayerChangedWorldHandler playerchangedworld = null;
				PlayerRespawnHandler playerrespawn = null;
				PlayerResourcePackStatusHandler playerresourcepackstatus = null;
				try {
					playerjoin = new PlayerJoinHandler(this, amusic, playerips, config.joinplaylist);
				} catch (NoClassDefFoundError e) {
				}
				try {
					playerquit = new PlayerQuitHandler(this, amusic, playerips, uploadmusiccmd);
				} catch (NoClassDefFoundError e) {
				}
				try {
					playerchangedworld = new PlayerChangedWorldHandler(this, amusic.positiontracker);
				} catch (NoClassDefFoundError e) {
				}
				try {
					playerrespawn = new PlayerRespawnHandler(this, amusic.positiontracker);
				} catch (NoClassDefFoundError e) {
				}
				if(waitacception) {
					try {
						playerresourcepackstatus = new PlayerResourcePackStatusHandler(this, amusic.resourcemanager);
					} catch (NoClassDefFoundError e) {
					}
				}
				this.playerjoin = playerjoin;
				this.playerquit = playerquit;
				this.playerchangedworld = playerchangedworld;
				this.playerrespawn = playerrespawn;
				this.playerresourcepackstatus = playerresourcepackstatus;
			}
			MessageSender messagesender;
			switch (ver) {
			case 7:
				messagesender = new LegacyMessageSender_1_7_R4();
			break;
			case 8:
				messagesender = new LegacyMessageSender_1_8_R3();
			break;
			case 9:
				messagesender = new LegacyMessageSender_1_9_R2();
			break;
			case 10:
				messagesender = new LegacyMessageSender_1_10_R1();
			break;
			case 11:
				messagesender = new LegacyMessageSender_1_11_R1();
			break;
			default:
				messagesender = new SpigotMessageSender();
			break;
			}
			LangOptions.loadLang(messagesender, langfile, ver > 15);
			if(AMusicBukkit.instance == null) {
				AMusicBukkit.instance = this.amusic;
			}
		} else {
			this.usecmd = false;
			this.playerips = null;
			this.amusic = null;
			this.playerjoin = null;
			this.playerquit = null;
			this.playerchangedworld = null;
			this.playerrespawn = null;
			this.playerresourcepackstatus = null;
		}
		this.commandmap = commandmap;
		this.mapcommand = mapcommand;
		this.loadmusiccmd = loadmusiccmd;
		this.playmusiccmd = playmusiccmd;
		this.playmusicuntrackablecmd = playmusicuntrackablecmd;
		this.repeatcmd = repeatcmd;
		this.uploadmusiccmd = uploadmusiccmd;
	}
	
	public final static AMusic API() {
		return instance;
	}

	//PLUGIN INIT START
	public void onEnable() {
		final Server server = this.getServer();
		Logger logger = this.getLogger();
		if(this.amusic == null) {
			return;
		}
		if(this.mapcommand != null) {
			final String prefix = "amusic:";
			if(this.loadmusiccmd != null) {
				String cmdname = this.loadmusiccmd.getName();
				this.mapcommand.put(prefix.concat(cmdname), this.loadmusiccmd);
				this.mapcommand.put(cmdname, this.loadmusiccmd);
				this.loadmusiccmd.register(commandmap);
			}
			if(this.playmusiccmd != null) {
				String cmdname = this.playmusiccmd.getName();
				this.mapcommand.put(prefix.concat(cmdname), this.playmusiccmd);
				this.mapcommand.put(cmdname, this.playmusiccmd);
				this.playmusiccmd.register(commandmap);
			}
			if(this.playmusicuntrackablecmd != null) {
				String cmdname = this.playmusicuntrackablecmd.getName();
				this.mapcommand.put(prefix.concat(cmdname), this.playmusicuntrackablecmd);
				this.mapcommand.put(cmdname, this.playmusicuntrackablecmd);
				this.playmusicuntrackablecmd.register(commandmap);
			}
			if(this.repeatcmd != null) {
				String cmdname = this.repeatcmd.getName();
				this.mapcommand.put(prefix.concat(cmdname), this.repeatcmd);
				this.mapcommand.put(cmdname, this.repeatcmd);
				this.repeatcmd.register(commandmap);
			}
			if(this.uploadmusiccmd != null) {
				String cmdname = this.uploadmusiccmd.getName();
				this.mapcommand.put(prefix.concat(cmdname), this.uploadmusiccmd);
				this.mapcommand.put(cmdname, this.uploadmusiccmd);
				this.uploadmusiccmd.register(commandmap);
			}
		}
		if(this.amusic instanceof LocalAMusic) {
			if(this.playerjoin != null) this.playerjoin.register();
			if(this.playerquit != null) this.playerquit.register();
			if(this.playerchangedworld != null) this.playerchangedworld.register();
			if(this.playerrespawn != null) this.playerrespawn.register();
			if(this.playerresourcepackstatus != null) this.playerresourcepackstatus.register();
		}
		if(playerips != null) {
			playerips.clear();
			for(Player player : server.getOnlinePlayers()) {
				playerips.put(player, player.getAddress().getAddress());
			}
		}
		this.amusic.enable();
		if(this.amusic instanceof LocalAMusic) {
			try {
				this.geyserhook = new GeyserHook(this, ((LocalAMusic) this.amusic).datamanager);
				logger.info("Geyser hook loaded");
			} catch (NoClassDefFoundError e) {
			}
		}
	}

	public void onDisable() {
		if(this.geyserhook != null) {
			this.geyserhook.unregister();
		}
		if(this.amusic == null) {
			return;
		}
		if(this.mapcommand != null) {
			final String prefix = "amusic:";
			if(this.loadmusiccmd != null) {
				String cmdname = this.loadmusiccmd.getName();
				this.mapcommand.remove(prefix.concat(cmdname), this.loadmusiccmd);
				this.mapcommand.remove(cmdname, this.loadmusiccmd);
				this.loadmusiccmd.unregister(commandmap);
			}
			if(this.playmusiccmd != null) {
				String cmdname = this.playmusiccmd.getName();
				this.mapcommand.remove(prefix.concat(cmdname), this.playmusiccmd);
				this.mapcommand.remove(cmdname, this.playmusiccmd);
				this.playmusiccmd.unregister(commandmap);
			}
			if(this.playmusicuntrackablecmd != null) {
				String cmdname = this.playmusicuntrackablecmd.getName();
				this.mapcommand.remove(prefix.concat(cmdname), this.playmusicuntrackablecmd);
				this.mapcommand.remove(cmdname, this.playmusicuntrackablecmd);
				this.playmusicuntrackablecmd.unregister(commandmap);
			}
			if(this.repeatcmd != null) {
				String cmdname = this.repeatcmd.getName();
				this.mapcommand.remove(prefix.concat(cmdname), this.repeatcmd);
				this.mapcommand.remove(cmdname, this.repeatcmd);
				this.repeatcmd.unregister(commandmap);
			}
			if(this.uploadmusiccmd != null) {
				String cmdname = this.uploadmusiccmd.getName();
				this.mapcommand.remove(prefix.concat(cmdname), this.uploadmusiccmd);
				this.mapcommand.remove(cmdname, this.uploadmusiccmd);
				this.uploadmusiccmd.unregister(commandmap);
			}
		}
		if(this.amusic instanceof LocalAMusic) {
			if(this.playerjoin != null) this.playerjoin.unregister();
			if(this.playerquit != null) this.playerquit.unregister();
			if(this.playerchangedworld != null) this.playerchangedworld.unregister();
			if(this.playerrespawn != null) this.playerrespawn.unregister();
			if(this.playerresourcepackstatus != null) this.playerresourcepackstatus.unregister();
		}
		this.amusic.disable();
	}
	//PLUGIN INIT END

}
