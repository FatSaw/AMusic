package me.bomb.amusic.yt;

import java.io.File;
import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.ConfigOptions;
import me.bomb.amusic.PackSender;
import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.SoundStopper;
import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.source.SoundSource;
import me.bomb.amusic.yt.command.LangOptions;
import me.bomb.amusic.yt.command.LoadmusicCommand;
import me.bomb.amusic.yt.command.LoadmusicTabComplete;
import me.bomb.amusic.yt.command.PlaymusicCommand;
import me.bomb.amusic.yt.command.PlaymusicTabComplete;
import me.bomb.amusic.yt.command.RepeatCommand;
import me.bomb.amusic.yt.command.RepeatTabComplete;
import me.bomb.amusic.yt.command.SelectorProcessor;
import me.bomb.amusic.yt.legacy.LegacyPackSender_1_10_R1;
import me.bomb.amusic.yt.legacy.LegacyPackSender_1_7_R4;
import me.bomb.amusic.yt.legacy.LegacyPackSender_1_8_R3;
import me.bomb.amusic.yt.legacy.LegacyPackSender_1_9_R2;
import me.bomb.amusic.yt.legacy.LegacySoundStopper_1_7_R4;
import me.bomb.amusic.yt.legacy.LegacySoundStopper_1_8_R3;
import me.bomb.amusic.yt.legacy.LegacySoundStopper_1_9_R2;

public final class AMusicYT extends JavaPlugin {
	
	private final AMusic amusic;
	private final ResourceManager resourcemanager;
	private final ConcurrentHashMap<Object,InetAddress> playerips;
	private final PositionTracker positiontracker;
	
	public AMusicYT() {
		byte ver = 127;
		try {
			String nmsversion = Bukkit.getServer().getClass().getPackage().getName().substring(23);
			ver = Byte.valueOf(nmsversion.split("_", 3)[1]);
		} catch (StringIndexOutOfBoundsException e) {
		}
		
		File plugindir = this.getDataFolder(), configfile = new File(plugindir, "config.yml"), langfile = new File(plugindir, "lang.yml"), packeddir = new File(plugindir, "Packed"), tempdirectory = new File(plugindir, "Temp");
		if(!plugindir.exists()) {
			plugindir.mkdirs();
		}
		if(!packeddir.exists()) {
			packeddir.mkdir();
		}
		if(!tempdirectory.exists()) {
			tempdirectory.mkdir();
		}
		boolean waitacception = true;
		PackSender packsender;
		SoundStopper soundstopper;
		switch (ver) {
		case 7:
			packsender = new LegacyPackSender_1_7_R4();
			soundstopper = new LegacySoundStopper_1_7_R4();
			waitacception = false;
		break;
		case 8:
			packsender = new LegacyPackSender_1_8_R3();
			soundstopper = new LegacySoundStopper_1_8_R3();
		break;
		case 9:
			packsender = new LegacyPackSender_1_9_R2();
			soundstopper = new LegacySoundStopper_1_9_R2();
		break;
		case 10:
			packsender = new LegacyPackSender_1_10_R1();
			soundstopper = new BukkitSoundStopper();
		break;
		default:
			packsender = new BukkitPackSender();
			soundstopper = new BukkitSoundStopper();
		break;
		}
		int maxpacksize = ver < 15 ? 52428800 : ver < 18 ? 104857600 : 262144000;
		ConfigOptions configoptions = new ConfigOptions(configfile, maxpacksize, null, packeddir, waitacception);
		playerips = configoptions.strictdownloaderlist ? new ConcurrentHashMap<Object,InetAddress>(16,0.75f,1) : null;
		Runtime runtime = Runtime.getRuntime();
		SoundSource<?> source = new YoutubeSource(tempdirectory, runtime, configoptions.maxmusicfilesize, configoptions.ffmpegbinary, configoptions.bitrate, configoptions.channels, configoptions.samplingrate);
		this.amusic = new AMusic(configoptions, source, packsender, new BukkitSoundStarter(), soundstopper, playerips);
		this.resourcemanager = amusic.resourcemanager;
		this.positiontracker = amusic.positiontracker;
		LangOptions.loadLang(langfile, ver > 15);
	}
	
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
		if(playerips != null) {
			playerips.clear();
			for(Player player : Bukkit.getOnlinePlayers()) {
				playerips.put(player, player.getAddress().getAddress());
			}
		}
		Bukkit.getPluginManager().registerEvents(new EventListener(resourcemanager, positiontracker, playerips), this);
		this.amusic.enable();
	}
	
	public void onDisable() {
		this.amusic.disable();
	}

}
