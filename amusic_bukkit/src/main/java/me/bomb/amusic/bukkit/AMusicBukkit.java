package me.bomb.amusic.bukkit;

import java.io.File;
import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.ConfigOptions;
import me.bomb.amusic.DataStorage;
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
import me.bomb.amusic.bukkit.legacy.LegacyPackSender_1_10_R1;
import me.bomb.amusic.bukkit.legacy.LegacyPackSender_1_7_R4;
import me.bomb.amusic.bukkit.legacy.LegacyPackSender_1_8_R3;
import me.bomb.amusic.bukkit.legacy.LegacyPackSender_1_9_R2;
import me.bomb.amusic.bukkit.legacy.LegacySoundStopper_1_7_R4;
import me.bomb.amusic.bukkit.legacy.LegacySoundStopper_1_8_R3;
import me.bomb.amusic.bukkit.legacy.LegacySoundStopper_1_9_R2;
import me.bomb.amusic.resourceserver.ResourceManager;


public final class AMusicBukkit extends JavaPlugin {
	private final AMusic amusic;
	private final ConfigOptions configoptions;
	private final DataStorage data;
	private final ResourceManager resourcemanager;
	private final ConcurrentHashMap<Object,InetAddress> playerips;
	private final PackSender packsender;
	private final PositionTracker positiontracker;

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
		configoptions = new ConfigOptions(configfile, maxpacksize, musicdir, packeddir, waitacception);
		playerips = configoptions.strictdownloaderlist ? new ConcurrentHashMap<Object,InetAddress>(16,0.75f,1) : null;
		data = new DataStorage(packeddir, (byte) 2);
		data.load();
		this.amusic = new AMusic(configoptions, data, packsender, new BukkitSoundStarter(), soundstopper, playerips);
		this.resourcemanager = amusic.resourcemanager;
		this.positiontracker = amusic.positiontracker;
		LangOptions.loadLang(langfile, ver > 15);
		amusic.setAPI();
	}

	//PLUGIN INIT START
	public void onEnable() {
		SelectorProcessor selectorprocessor = new SelectorProcessor(Bukkit.getServer(), new Random());
		PluginCommand loadmusiccommand = getCommand("loadmusic");
		loadmusiccommand.setExecutor(new LoadmusicCommand(configoptions, data, resourcemanager, positiontracker, packsender, selectorprocessor));
		loadmusiccommand.setTabCompleter(new LoadmusicTabComplete(data));
		PlaymusicTabComplete pmtc = new PlaymusicTabComplete(positiontracker);
		PluginCommand playmusiccommand = getCommand("playmusic");
		playmusiccommand.setExecutor(new PlaymusicCommand(positiontracker, selectorprocessor, true));
		playmusiccommand.setTabCompleter(pmtc);
		PluginCommand playmusicntrackablecommand = getCommand("playmusicuntrackable");
		playmusicntrackablecommand.setExecutor(new PlaymusicCommand(positiontracker, selectorprocessor, false));
		playmusicntrackablecommand.setTabCompleter(pmtc);
		PluginCommand repeatcommand = getCommand("repeat");
		repeatcommand.setExecutor(new RepeatCommand(positiontracker, selectorprocessor));
		repeatcommand.setTabCompleter(new RepeatTabComplete());
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
		this.data.end();
	}
	//PLUGIN INIT END

}
