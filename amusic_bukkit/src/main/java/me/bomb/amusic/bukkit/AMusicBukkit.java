package me.bomb.amusic.bukkit;

import java.io.File;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.ConfigOptions;
import me.bomb.amusic.PackSender;
import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.bukkit.command.LoadmusicCommand;
import me.bomb.amusic.bukkit.command.LoadmusicTabComplete;
import me.bomb.amusic.bukkit.command.PlaymusicCommand;
import me.bomb.amusic.bukkit.command.PlaymusicTabComplete;
import me.bomb.amusic.bukkit.command.RepeatCommand;
import me.bomb.amusic.bukkit.command.RepeatTabComplete;
import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.resourceserver.ResourceServer;


public final class AMusicBukkit extends JavaPlugin {
	
	private final ConfigOptions configoptions;
	private final Data data;
	private final ResourceManager resourcemanager;
	private final ConcurrentHashMap<Object,InetAddress> playerips;
	private final PackSender packsender;
	private final ResourceServer resourceserver;
	private final PositionTracker positiontracker;
	
	public AMusicBukkit() {
		byte ver = Byte.valueOf(Bukkit.getServer().getClass().getPackage().getName().substring(23).split("_", 3)[1]);
		configoptions = new ConfigOptions(this.getDataFolder(), ver);
		playerips = new ConcurrentHashMap<Object,InetAddress>(16,0.75f,1);
		File datafile = new File(this.getDataFolder(), "data.yml");
		data = new Data(datafile);
		data.load();
		if(!datafile.exists()) {
			data.save();
		}
		packsender = new BukkitPackSender();
		resourcemanager = new ResourceManager(configoptions.maxpacksize, configoptions.servercache, configoptions.clientcache, configoptions.tokensalt);
		positiontracker = new PositionTracker(new BukkitSoundStarter(), new BukkitSoundStopper(), configoptions.hasplaceholderapi);
		resourceserver = new ResourceServer(playerips, configoptions.port, resourcemanager);
	}
	
	//PLUGIN INIT START
	public void onEnable() {
		PluginCommand loadmusiccommand = getCommand("loadmusic");
		loadmusiccommand.setExecutor(new LoadmusicCommand(configoptions, data, resourcemanager, positiontracker, packsender));
		loadmusiccommand.setTabCompleter(new LoadmusicTabComplete(data));
		PluginCommand playmusiccommand = getCommand("playmusic");
		playmusiccommand.setExecutor(new PlaymusicCommand(positiontracker));
		playmusiccommand.setTabCompleter(new PlaymusicTabComplete(positiontracker));
		PluginCommand repeatcommand = getCommand("repeat");
		repeatcommand.setExecutor(new RepeatCommand(positiontracker));
		repeatcommand.setTabCompleter(new RepeatTabComplete());
		playerips.clear();
		for(Player player : Bukkit.getOnlinePlayers()) {
			playerips.put(player, player.getAddress().getAddress());
		}
		Bukkit.getPluginManager().registerEvents(new EventListener(resourcemanager, positiontracker, playerips), this);
		if (configoptions.hasplaceholderapi) {
			new AMusicPlaceholderExpansion(positiontracker).register();
		}
		try {
			new AMusic(configoptions, positiontracker, resourcemanager, packsender, data);
		} catch (ExceptionInInitializerError e) {
			e.printStackTrace();
		}
		positiontracker.start();
		resourceserver.start();
	}

	public void onDisable() {
		positiontracker.end();
		resourceserver.end();
		while (positiontracker.isAlive() || resourceserver.isAlive()) { //DONT STOP)
		}
	}
	//PLUGIN INIT END

}
