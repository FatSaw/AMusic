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
import me.bomb.amusic.ConfigOptions;
import me.bomb.amusic.PackSender;
import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.SoundStopper;
import me.bomb.amusic.bukkit.command.LangOptions;
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
import me.bomb.amusic.bukkit.legacy.LegacySoundStopper_1_7_R4;
import me.bomb.amusic.bukkit.legacy.LegacySoundStopper_1_8_R3;
import me.bomb.amusic.bukkit.legacy.LegacySoundStopper_1_9_R2;
import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.source.LocalConvertedSource;
import me.bomb.amusic.source.LocalUnconvertedParallelSource;
import me.bomb.amusic.source.LocalUnconvertedSource;
import me.bomb.amusic.source.SoundSource;


public final class AMusicBukkit extends JavaPlugin {
	private final AMusic amusic;
	private final ResourceManager resourcemanager;
	private final ConcurrentHashMap<Object,InetAddress> playerips;
	private final PositionTracker positiontracker;
	private final boolean waitacception;

	public AMusicBukkit() {
		byte ver = 127;
		try {
			String nmsversion = Bukkit.getServer().getClass().getPackage().getName().substring(23);
			ver = Byte.valueOf(nmsversion.split("_", 3)[1]);
		} catch (StringIndexOutOfBoundsException e) {
		}
		
		File plugindir = this.getDataFolder(), configfile = new File(plugindir, "config.yml"), langfile = new File(plugindir, "lang.yml"), musicdir = new File(plugindir, "Music"), packeddir = new File(plugindir, "Packed");
		if(!plugindir.exists()) {
			plugindir.mkdirs();
		}
		if(!musicdir.exists()) {
			musicdir.mkdir();
		}
		if(!packeddir.exists()) {
			packeddir.mkdir();
		}
		boolean waitacception = true;
		PackSender packsender;
		SoundStopper soundstopper;
		MessageSender messagesender;
		switch (ver) {
		case 7:
			packsender = new LegacyPackSender_1_7_R4();
			soundstopper = new LegacySoundStopper_1_7_R4();
			messagesender = new LegacyMessageSender_1_7_R4();
			waitacception = false;
		break;
		case 8:
			packsender = new LegacyPackSender_1_8_R3();
			soundstopper = new LegacySoundStopper_1_8_R3();
			messagesender = new LegacyMessageSender_1_8_R3();
		break;
		case 9:
			packsender = new LegacyPackSender_1_9_R2();
			soundstopper = new LegacySoundStopper_1_9_R2();
			messagesender = new LegacyMessageSender_1_9_R2();
		break;
		case 10:
			packsender = new LegacyPackSender_1_10_R1();
			soundstopper = new BukkitSoundStopper();
			messagesender = new LegacyMessageSender_1_10_R1();
		break;
		case 11:
			packsender = new BukkitPackSender();
			soundstopper = new BukkitSoundStopper();
			messagesender = new LegacyMessageSender_1_11_R1();
		break;
		default:
			packsender = new BukkitPackSender();
			soundstopper = new BukkitSoundStopper();
			messagesender = new SpigotMessageSender();
		break;
		}
		int maxpacksize = ver < 15 ? 52428800 : ver < 18 ? 104857600 : 262144000;
		ConfigOptions configoptions = new ConfigOptions(configfile, maxpacksize, musicdir, packeddir, waitacception);
		this.waitacception = configoptions.waitacception;
		playerips = configoptions.resourcestrictaccess || configoptions.uploaderstrictaccess ? new ConcurrentHashMap<Object,InetAddress>(16,0.75f,1) : null;
		Runtime runtime = Runtime.getRuntime();
		SoundSource<?> source = configoptions.useconverter ? configoptions.encodetracksasynchronly ? new LocalUnconvertedParallelSource(runtime, configoptions.musicdir, configoptions.maxmusicfilesize, configoptions.ffmpegbinary, configoptions.bitrate, configoptions.channels, configoptions.samplingrate) : new LocalUnconvertedSource(runtime, configoptions.musicdir, configoptions.maxmusicfilesize, configoptions.ffmpegbinary, configoptions.bitrate, configoptions.channels, configoptions.samplingrate) : new LocalConvertedSource(configoptions.musicdir, configoptions.maxmusicfilesize);
		this.amusic = new AMusic(configoptions, source, packsender, new BukkitSoundStarter(), soundstopper, playerips);
		this.resourcemanager = amusic.resourcemanager;
		this.positiontracker = amusic.positiontracker;
		LangOptions.loadLang(messagesender, langfile, ver > 15);
		amusic.setAPI();
	}

	//PLUGIN INIT START
	public void onEnable() {
		Server server = Bukkit.getServer();
		SelectorProcessor selectorprocessor = new SelectorProcessor(Bukkit.getServer(), new Random());
		PluginCommand loadmusiccommand = getCommand("loadmusic");
		loadmusiccommand.setExecutor(new LoadmusicCommand(server, amusic.source, amusic.datamanager, amusic.dispatcher, selectorprocessor));
		loadmusiccommand.setTabCompleter(new LoadmusicTabComplete(server, amusic.datamanager));
		PlaymusicTabComplete pmtc = new PlaymusicTabComplete(server, positiontracker);
		PluginCommand playmusiccommand = getCommand("playmusic");
		playmusiccommand.setExecutor(new PlaymusicCommand(server, positiontracker, selectorprocessor, true));
		playmusiccommand.setTabCompleter(pmtc);
		PluginCommand playmusicntrackablecommand = getCommand("playmusicuntrackable");
		playmusicntrackablecommand.setExecutor(new PlaymusicCommand(server, positiontracker, selectorprocessor, false));
		playmusicntrackablecommand.setTabCompleter(pmtc);
		PluginCommand repeatcommand = getCommand("repeat");
		repeatcommand.setExecutor(new RepeatCommand(server, positiontracker, selectorprocessor));
		repeatcommand.setTabCompleter(new RepeatTabComplete(server));
		PluginCommand uploadmusiccommand = getCommand("uploadmusic");
		UploadmusicCommand uploadmusiccmd = new UploadmusicCommand(amusic.uploadermanager);
		uploadmusiccommand.setExecutor(uploadmusiccmd);
		uploadmusiccommand.setTabCompleter(new UploadmusicTabComplete(amusic.uploadermanager));
		if(playerips != null) {
			playerips.clear();
			for(Player player : Bukkit.getOnlinePlayers()) {
				playerips.put(player, player.getAddress().getAddress());
			}
		}
		PluginManager pluginmanager = server.getPluginManager();
		pluginmanager.registerEvents(new EventListener(resourcemanager, positiontracker, playerips, uploadmusiccmd), this);
		if(waitacception) {
			pluginmanager.registerEvents(new PackStatusEventListener(resourcemanager, positiontracker), this);
		}
		this.amusic.enable();
	}

	public void onDisable() {
		this.amusic.disable();
	}
	//PLUGIN INIT END

}
