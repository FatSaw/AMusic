package me.bomb.amusic.bukkit;

import java.io.File;
import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.Configuration;
import me.bomb.amusic.LocalAMusic;
import me.bomb.amusic.MessageSender;
import me.bomb.amusic.PackSender;
import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.SoundStarter;
import me.bomb.amusic.ClientAMusic;
import me.bomb.amusic.SoundStopper;
import me.bomb.amusic.util.LangOptions;
import me.bomb.amusic.bukkit.command.LoadmusicCommand;
import me.bomb.amusic.bukkit.command.LoadmusicTabComplete;
import me.bomb.amusic.bukkit.command.PlaymusicCommand;
import me.bomb.amusic.bukkit.command.PlaymusicTabComplete;
import me.bomb.amusic.bukkit.command.RepeatCommand;
import me.bomb.amusic.bukkit.command.RepeatTabComplete;
import me.bomb.amusic.bukkit.command.SelectorProcessor;
import me.bomb.amusic.bukkit.command.UploadmusicCommand;
import me.bomb.amusic.bukkit.command.UploadmusicTabComplete;
import me.bomb.amusic.bukkit.legacy.LegacyMessageSender_1_10_R1;
import me.bomb.amusic.bukkit.legacy.LegacyMessageSender_1_11_R1;
import me.bomb.amusic.bukkit.legacy.LegacyMessageSender_1_7_R4;
import me.bomb.amusic.bukkit.legacy.LegacyMessageSender_1_8_R3;
import me.bomb.amusic.bukkit.legacy.LegacyMessageSender_1_9_R2;
import me.bomb.amusic.bukkit.legacy.LegacyPackSender_1_10_R1;
import me.bomb.amusic.bukkit.legacy.LegacyPackSender_1_7_R4;
import me.bomb.amusic.bukkit.legacy.LegacyPackSender_1_8_R3;
import me.bomb.amusic.bukkit.legacy.LegacyPackSender_1_9_R2;
import me.bomb.amusic.bukkit.legacy.LegacySoundStarter_1_10_R1;
import me.bomb.amusic.bukkit.legacy.LegacySoundStarter_1_9_R2;
import me.bomb.amusic.bukkit.legacy.LegacySoundStopper_1_10_R1;
import me.bomb.amusic.bukkit.legacy.LegacySoundStopper_1_7_R4;
import me.bomb.amusic.bukkit.legacy.LegacySoundStopper_1_8_R3;
import me.bomb.amusic.bukkit.legacy.LegacySoundStopper_1_9_R2;
import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.source.LocalConvertedSource;
import me.bomb.amusic.source.LocalUnconvertedParallelSource;
import me.bomb.amusic.source.LocalUnconvertedSource;
import me.bomb.amusic.source.SoundSource;


public final class AMusicBukkit extends JavaPlugin {
	
	private static AMusic instance = null;
	
	private final AMusic amusic;
	private final ConcurrentHashMap<Object,InetAddress> playerips;
	private final boolean waitacception;
	private final String configerrors, uploaderhost;
	private final ResourceManager resourcemanager;
	private final PositionTracker positiontracker;

	public AMusicBukkit() {
		byte ver = 127;
		try {
			String nmsversion = Bukkit.getServer().getClass().getPackage().getName().substring(23);
			ver = Byte.valueOf(nmsversion.split("_", 3)[1]);
		} catch (StringIndexOutOfBoundsException | NumberFormatException e) {
		}
		
		File plugindir = this.getDataFolder(), configfile = new File(plugindir, "config.yml"), langfile = new File(plugindir, "lang.yml"), musicdir = new File(plugindir, "Music"), packeddir = new File(plugindir, "Packed");
		if(!plugindir.exists()) {
			plugindir.mkdirs();
		}
		boolean waitacception = ver == 7 ? false : true;
		Configuration config = new Configuration(configfile, musicdir, packeddir, waitacception, true);
		this.configerrors = config.errors;
		if(config.use) {
			if(!musicdir.exists()) {
				musicdir.mkdir();
			}
			if(!packeddir.exists()) {
				packeddir.mkdir();
			}
			if(config.connectuse) {
				this.waitacception = false;
				this.playerips = null;
				this.uploaderhost = null;
				ClientAMusic amusic = new ClientAMusic(config.connectifip, config.connectremoteip, config.connectport);
				this.resourcemanager = null;
				this.positiontracker = null;
				this.amusic = amusic;
			} else {
				PackSender packsender;
				SoundStarter soundstarter;
				SoundStopper soundstopper;
				switch (ver) {
				case 7:
					packsender = new LegacyPackSender_1_7_R4();
					soundstarter = new BukkitLegacySoundStarter();
					//soundstopper = new LegacySoundStopper_1_7_R4();
					soundstopper = new BukkitSoundSilenceLockStopper();
				break;
				case 8:
					packsender = new LegacyPackSender_1_8_R3();
					soundstarter = new BukkitLegacySoundStarter();
					//soundstopper = new LegacySoundStopper_1_8_R3();
					soundstopper = new BukkitSoundSilenceLockStopper();
				break;
				case 9:
					packsender = new LegacyPackSender_1_9_R2();
					//soundstarter = new LegacySoundStarter_1_9_R2();
					soundstarter = new BukkitLegacySoundStarter();
					soundstopper = new LegacySoundStopper_1_9_R2();
				break;
				case 10:
					packsender = new LegacyPackSender_1_10_R1();
					//soundstarter = new LegacySoundStarter_1_10_R1();
					soundstarter = new BukkitLegacySoundStarter();
					soundstopper = new LegacySoundStopper_1_10_R1();
				break;
				case 11: case 12:
					packsender = new BukkitPackSender();
					soundstarter = new BukkitLegacySoundStarter();
					soundstopper = new BukkitSoundStopper();
				break;
				default:
					packsender = new BukkitPackSender();
					soundstarter = new BukkitSoundStarter();
					soundstopper = new BukkitSoundStopper();
				break;
				}
				this.waitacception = config.waitacception;
				this.uploaderhost = config.uploadhost;
				playerips = config.sendpackstrictaccess || config.uploadstrictaccess ? new ConcurrentHashMap<Object,InetAddress>(16,0.75f,1) : null;
				Runtime runtime = Runtime.getRuntime();
				SoundSource source = config.encoderuse ? config.encodetracksasync ? new LocalUnconvertedParallelSource(runtime, config.musicdir, config.packsizelimit, config.encoderbinary, config.encoderbitrate, config.encoderchannels, config.encodersamplingrate) : new LocalUnconvertedSource(runtime, config.musicdir, config.packsizelimit, config.encoderbinary, config.encoderbitrate, config.encoderchannels, config.encodersamplingrate) : new LocalConvertedSource(config.musicdir, config.packsizelimit);
				LocalAMusic amusic = new LocalAMusic(config, source, packsender, soundstarter, soundstopper, playerips);
				this.resourcemanager = amusic.resourcemanager;
				this.positiontracker = amusic.positiontracker;
				this.amusic = amusic;
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
			this.waitacception = false;
			this.playerips = null;
			this.uploaderhost = null;
			this.resourcemanager = null;
			this.positiontracker = null;
			this.amusic = null;
		}
	}
	
	public final static AMusic API() {
		return instance;
	}

	//PLUGIN INIT START
	public void onEnable() {
		Server server = Bukkit.getServer();
		if(!this.configerrors.isEmpty()) {
			server.getLogger().severe("AMusic config initialization errors: \n".concat(configerrors));
			return;
		}
		if(this.amusic == null) {
			return;
		}
		SelectorProcessor selectorprocessor = new SelectorProcessor(Bukkit.getServer(), new Random());
		PluginCommand loadmusiccommand = getCommand("loadmusic");
		loadmusiccommand.setExecutor(new LoadmusicCommand(server, amusic, selectorprocessor));
		loadmusiccommand.setTabCompleter(new LoadmusicTabComplete(server, amusic));
		PlaymusicTabComplete pmtc = new PlaymusicTabComplete(server, amusic);
		PluginCommand playmusiccommand = getCommand("playmusic");
		playmusiccommand.setExecutor(new PlaymusicCommand(server, amusic, selectorprocessor, true));
		playmusiccommand.setTabCompleter(pmtc);
		PluginCommand playmusicntrackablecommand = getCommand("playmusicuntrackable");
		playmusicntrackablecommand.setExecutor(new PlaymusicCommand(server, amusic, selectorprocessor, false));
		playmusicntrackablecommand.setTabCompleter(pmtc);
		PluginCommand repeatcommand = getCommand("repeat");
		repeatcommand.setExecutor(new RepeatCommand(server, amusic, selectorprocessor));
		repeatcommand.setTabCompleter(new RepeatTabComplete(server));
		PluginCommand uploadmusiccommand = getCommand("uploadmusic");
		UploadmusicCommand uploadmusiccmd = new UploadmusicCommand(amusic, uploaderhost);
		uploadmusiccommand.setExecutor(uploadmusiccmd);
		uploadmusiccommand.setTabCompleter(new UploadmusicTabComplete(amusic));
		if(playerips != null) {
			playerips.clear();
			for(Player player : Bukkit.getOnlinePlayers()) {
				playerips.put(player, player.getAddress().getAddress());
			}
		}
		PluginManager pluginmanager = server.getPluginManager();
		if(this.resourcemanager != null) {
			pluginmanager.registerEvents(new EventListener(resourcemanager, positiontracker, playerips, uploadmusiccmd), this);
			if(waitacception) {
				pluginmanager.registerEvents(new PackStatusEventListener(resourcemanager), this);
			}
		}
		
		this.amusic.enable();
	}

	public void onDisable() {
		if(this.amusic == null) {
			return;
		}
		this.amusic.disable();
	}
	//PLUGIN INIT END

}
