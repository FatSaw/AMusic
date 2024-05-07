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
import me.bomb.amusic.DataStorage;
import me.bomb.amusic.PackSender;
import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.bukkit.command.LoadmusicCommand;
import me.bomb.amusic.bukkit.command.LoadmusicTabComplete;
import me.bomb.amusic.bukkit.command.PlaymusicCommand;
import me.bomb.amusic.bukkit.command.PlaymusicTabComplete;
import me.bomb.amusic.bukkit.command.RepeatCommand;
import me.bomb.amusic.bukkit.command.RepeatTabComplete;
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
		byte ver = Byte.valueOf(Bukkit.getServer().getClass().getPackage().getName().substring(23).split("_", 3)[1]);
		File plugindir = this.getDataFolder(), configfile = new File(plugindir, "config.yml"), datafile = new File(plugindir, "data.yml"), musicdir = new File(plugindir, "Music"), packeddir = new File(plugindir, "Packed"), tempdir = new File(plugindir, "Temp");
		if(!plugindir.exists()) {
			plugindir.mkdirs();
		}
		if(!musicdir.exists()) {
			musicdir.mkdir();
		}
		if(!packeddir.exists()) {
			packeddir.mkdir();
		}
		if(!tempdir.exists()) {
			tempdir.mkdir();
		}
		int maxpacksize = ver < 15 ? 52428800 : ver < 18 ? 104857600 : 262144000;
		configoptions = new ConfigOptions(configfile, maxpacksize, musicdir, packeddir, tempdir);
		playerips = configoptions.strictdownloaderlist ? new ConcurrentHashMap<Object,InetAddress>(16,0.75f,1) : null;
		data = new DataStorage(packeddir, (byte) 2);
		data.load();
		if(!datafile.exists()) {
			data.save();
		}
		packsender = new BukkitPackSender();
		this.amusic = new AMusic(configoptions, data, packsender, new BukkitSoundStarter(), new BukkitSoundStopper(), playerips);
		this.resourcemanager = amusic.resourcemanager;
		this.positiontracker = amusic.positiontracker;
		amusic.setAPI();
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
		if(playerips != null) {
			playerips.clear();
			for(Player player : Bukkit.getOnlinePlayers()) {
				playerips.put(player, player.getAddress().getAddress());
			}
		}
		Bukkit.getPluginManager().registerEvents(new EventListener(resourcemanager, positiontracker, playerips), this);
		if (configoptions.hasplaceholderapi) {
			new AMusicPlaceholderExpansion(positiontracker).register();
		}
		this.amusic.enable();
	}

	public void onDisable() {
		this.amusic.disable();
		this.data.end();
	}
	//PLUGIN INIT END

}
