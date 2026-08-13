package me.bomb.amusic.bukkit;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
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
import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.SoundStarter;
import me.bomb.amusic.ClientAMusic;
import me.bomb.amusic.SoundStopper;
import me.bomb.amusic.util.AMusicLogger;
import me.bomb.amusic.util.LangLoader;
import me.bomb.amusic.bukkit.command.LoadmusicCommand;
import me.bomb.amusic.bukkit.command.PlaymusicCommand;
import me.bomb.amusic.bukkit.command.RepeatCommand;
import me.bomb.amusic.bukkit.command.SelectorProcessor;
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
import me.bomb.amusic.packedinfo.Data;
import me.bomb.amusic.packedinfo.LocalConvertedZerocopySource;
import me.bomb.amusic.packedinfo.PackMergeEntryFile;
import me.bomb.amusic.packedinfo.PackMergeSourceLocal;
import me.bomb.amusic.permission.AMusicPermission;
import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.bukkit.legacy.LegacySoundStopper_1_10_R1;


public final class AMusicBukkit extends JavaPlugin {
	
	private static AMusic instance = null;
	
	private final AMusic amusic;
	private final ConcurrentHashMap<UUID, EnumSet<AMusicPermission>> playerspermission;
	private final ConcurrentHashMap<Object,InetAddress> playerips;
	private final boolean usecmd;
	private GeyserHook geyserhook = null;
	
	private final SimpleCommandMap commandmap;
	private final HashMap<String, Command> mapcommand;
	
	private final Command loadmusiccmd, playmusiccmd, repeatcmd;
	
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
		Path plugindir = this.getDataFolder().toPath(), mergezip = plugindir.resolve("resourcepack.zip"), configfile = plugindir.resolve("config.yml"), langfile = plugindir.resolve("lang.yml"), musicdir = plugindir.resolve("Music"), packeddir = plugindir.resolve("Packed");
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
		RepeatCommand repeatcmd = null;
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
						}
					}
				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e1) {
					e1.printStackTrace();
				}
				
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
			LangLoader lang = new LangLoader(langfile, ver > 15 ? "lang_rgb.yml" : "lang_old.yml", messagesender);
			ConcurrentHashMap<UUID, EnumSet<AMusicPermission>> playerspermission = new ConcurrentHashMap<UUID, EnumSet<AMusicPermission>>();
			PlayerJoinHandler playerjoin = null;
			PlayerQuitHandler playerquit = null;
			if(config.connectuse) {
				this.playerips = null;
				ClientAMusic amusic = new ClientAMusic(config.connectifip, config.connectremoteip, config.connectport, config.connectsocketfactory, config.executor);
				this.amusic = amusic;
				this.playerchangedworld = null;
				this.playerrespawn = null;
				this.playerresourcepackstatus = null;
				if(this.usecmd) {
					SelectorProcessor selectorprocessor = new SelectorProcessor(server, new Random());
					loadmusiccmd = new LoadmusicCommand(server, amusic, lang, playerspermission, selectorprocessor);
					playmusiccmd = new PlaymusicCommand(server, amusic, lang, playerspermission, selectorprocessor);
					repeatcmd = new RepeatCommand(server, amusic, lang, playerspermission, selectorprocessor);
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
				playerips = config.sendpackstrictaccess ? new ConcurrentHashMap<Object,InetAddress>(16,0.75f,1) : null;
				LocalConvertedZerocopySource lczs = new LocalConvertedZerocopySource(config.musicdir, config.packsizelimit, config.packsizelimit, config.packthreadcoefficient, config.packthreadlimitcount);
				PositionTracker positiontracker = new PositionTracker(soundstarter, soundstopper);
				ResourceManager resourcemanager = new ResourceManager(packsender, positiontracker, config.sendpackhost, config.packsizelimit, config.tokensalt, config.waitacception, config.sendpackstrictaccess ? playerips.values() : null, config.sendpackifip, config.sendpackport, config.sendpackbacklog, config.sendpacktimeout, config.sendpackserverfactory, (short) 2, config.sendpackexecutorchecker, config.sendpackexecutorsender);
				PackMergeSourceLocal packmergesource = new PackMergeSourceLocal(new PackMergeEntryFile(mergezip, config.packsizelimit), config.musicdir, config.packsizelimit);
				Data datamanager = config.ramcache ? config.diskstore ? Data.getLocalCachedStorage(!config.processpack, lczs, packmergesource, packeddir) : Data.getRamStorage(!config.processpack, lczs, packmergesource) : config.diskstore ? Data.getLocalStorage(!config.processpack, lczs, packmergesource, packeddir) : Data.getNoStorage(!config.processpack, lczs, packmergesource);
				LocalAMusic amusic = new LocalAMusic(logger, config.executor, lczs, positiontracker, resourcemanager, datamanager);
				this.amusic = amusic;
				if(this.usecmd) {
					SelectorProcessor selectorprocessor = new SelectorProcessor(server, new Random());
					loadmusiccmd = new LoadmusicCommand(server, amusic, lang, playerspermission, selectorprocessor);
					playmusiccmd = new PlaymusicCommand(server, amusic, lang, playerspermission, selectorprocessor);
					repeatcmd = new RepeatCommand(server, amusic, lang, playerspermission, selectorprocessor);
				}
				PlayerChangedWorldHandler playerchangedworld = null;
				PlayerRespawnHandler playerrespawn = null;
				PlayerResourcePackStatusHandler playerresourcepackstatus = null;
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
				this.playerchangedworld = playerchangedworld;
				this.playerrespawn = playerrespawn;
				this.playerresourcepackstatus = playerresourcepackstatus;
			}
			try {
				playerjoin = new PlayerJoinHandler(this, amusic, playerspermission, playerips, config.joinplaylist);
			} catch (NoClassDefFoundError e) {
			}
			try {
				playerquit = new PlayerQuitHandler(this, amusic, playerspermission, playerips);
			} catch (NoClassDefFoundError e) {
			}
			this.playerjoin = playerjoin;
			this.playerquit = playerquit;
			this.playerspermission = playerspermission;
			if(AMusicBukkit.instance == null) {
				AMusicBukkit.instance = this.amusic;
			}
		} else {
			this.usecmd = false;
			this.playerspermission = null;
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
		this.repeatcmd = repeatcmd;
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
			if(this.repeatcmd != null) {
				String cmdname = this.repeatcmd.getName();
				this.mapcommand.put(prefix.concat(cmdname), this.repeatcmd);
				this.mapcommand.put(cmdname, this.repeatcmd);
				this.repeatcmd.register(commandmap);
			}
		}
		if(this.playerjoin != null) this.playerjoin.register();
		if(this.playerquit != null) this.playerquit.register();
		if(this.playerchangedworld != null) this.playerchangedworld.register();
		if(this.playerrespawn != null) this.playerrespawn.register();
		if(this.playerresourcepackstatus != null) this.playerresourcepackstatus.register();
		if(this.playerips != null) {
			this.playerips.clear();
			for(Player player : server.getOnlinePlayers()) {
				this.playerips.put(player, player.getAddress().getAddress());
			}
		}
		if(this.playerspermission != null) {
			this.playerspermission.clear();
			for(Player player : server.getOnlinePlayers()) {
				EnumSet<AMusicPermission> permissions = EnumSet.noneOf(AMusicPermission.class);
				for (AMusicPermission permission : AMusicPermission.values()) {
					if(player.hasPermission(permission.permission)) permissions.add(permission);
				}
				this.playerspermission.put(player.getUniqueId(), permissions);
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
			if(this.repeatcmd != null) {
				String cmdname = this.repeatcmd.getName();
				this.mapcommand.remove(prefix.concat(cmdname), this.repeatcmd);
				this.mapcommand.remove(cmdname, this.repeatcmd);
				this.repeatcmd.unregister(commandmap);
			}
		}
		if(this.playerjoin != null) this.playerjoin.unregister();
		if(this.playerquit != null) this.playerquit.unregister();
		if(this.playerchangedworld != null) this.playerchangedworld.unregister();
		if(this.playerrespawn != null) this.playerrespawn.unregister();
		if(this.playerresourcepackstatus != null) this.playerresourcepackstatus.unregister();
		if(this.playerips != null) this.playerips.clear();
		if(this.playerspermission != null) this.playerspermission.clear();
		this.amusic.disable();
	}
	//PLUGIN INIT END

}
