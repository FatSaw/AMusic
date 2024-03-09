package me.bomb.amusic.bukkit;

import java.io.File;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.ConfigOptions;
import me.bomb.amusic.PackSender;
import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.SoundStopper;
import me.bomb.amusic.SoundStarter;
import me.bomb.amusic.bukkit.command.LoadmusicCommand;
import me.bomb.amusic.bukkit.command.LoadmusicTabComplete;
import me.bomb.amusic.bukkit.command.PlaymusicCommand;
import me.bomb.amusic.bukkit.command.PlaymusicTabComplete;
import me.bomb.amusic.bukkit.command.RepeatCommand;
import me.bomb.amusic.bukkit.command.RepeatTabComplete;
import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.resourceserver.ResourceServer;


public final class AMusicBukkit extends JavaPlugin {
	
	private static ResourceServer server;
	private ConfigOptions configoptions;
	private Data data;
	private ResourceManager resourcemanager;
	private PositionTracker positiontracker;
	private PackSender packsender;
	
	//PLUGIN INIT START
	public void onEnable() {
		byte ver = Byte.valueOf(Bukkit.getServer().getClass().getPackage().getName().substring(23).split("_", 3)[1]);
		configoptions = new ConfigOptions(this.getDataFolder(), ver);
		ConcurrentHashMap<Object,InetAddress> playerips = new ConcurrentHashMap<Object,InetAddress>(16,0.75f,1);
		data = new Data(new File(this.getDataFolder(), "data.yml"));
		data.load();
		SoundStopper soundstopper;
		soundstopper = new BukkitSoundStopper();
		packsender = new BukkitPackSender();
		SoundStarter soundstarter = new BukkitSoundStarter();
		try {
			resourcemanager = new ResourceManager(configoptions.maxpacksize, configoptions.servercache, configoptions.clientcache, configoptions.tokensalt);
		} catch (NoSuchAlgorithmException e) {
		}
		positiontracker = new PositionTracker(soundstarter, soundstopper, configoptions.hasplaceholderapi);
		
		server = new ResourceServer(playerips, configoptions.port, resourcemanager);
		
		PluginCommand loadmusiccommand = getCommand("loadmusic");
		loadmusiccommand.setExecutor(new LoadmusicCommand(configoptions, data, resourcemanager, positiontracker, packsender));
		loadmusiccommand.setTabCompleter(new LoadmusicTabComplete(data));
		PluginCommand playmusiccommand = getCommand("playmusic");
		playmusiccommand.setExecutor(new PlaymusicCommand(positiontracker));
		playmusiccommand.setTabCompleter(new PlaymusicTabComplete(positiontracker));
		PluginCommand repeatcommand = getCommand("repeat");
		repeatcommand.setExecutor(new RepeatCommand(positiontracker));
		repeatcommand.setTabCompleter(new RepeatTabComplete());
		Bukkit.getPluginManager().registerEvents(new EventListener(resourcemanager, positiontracker, playerips), this);
		if (configoptions.hasplaceholderapi) {
			new AMusicPlaceholderExpansion(positiontracker).register();
		}
		new AMusic(configoptions, positiontracker, resourcemanager, packsender, data);
	}

	public void onDisable() {
		positiontracker.end();
		server.end();
		while (positiontracker.isAlive() || server.isAlive()) { //DONT STOP)
		}
	}
	//PLUGIN INIT END

}
