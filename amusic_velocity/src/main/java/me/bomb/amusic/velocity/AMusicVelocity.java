package me.bomb.amusic.velocity;

import java.io.File;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.ConfigOptions;
import me.bomb.amusic.PackSender;
import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.velocity.command.LoadmusicCommand;
import me.bomb.amusic.velocity.command.PlaymusicCommand;
import me.bomb.amusic.velocity.command.RepeatCommand;

@Plugin(id = "amusic", name = "AMusic", version = "0.13", authors = {"Bomb"})
public class AMusicVelocity {
	
	private final ProxyServer server;
    private final Logger logger;
	private final AMusic amusic;
    private final ConfigOptions configoptions;
	private final DataConfig data;
	private final ResourceManager resourcemanager;
	private final ConcurrentHashMap<Object,InetAddress> playerips;
    private final PackSender packsender;
	private final PositionTracker positiontracker;
    
	@Inject
	public AMusicVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
		byte ver = 0;
		File plugindir = dataDirectory.toFile(), configfile = new File(plugindir, "config.yml"), datafile = new File(plugindir, "data.yml"), musicdir = new File(plugindir, "Music"), packeddir = new File(plugindir, "Packed"), tempdir = new File(plugindir, "Temp");
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
		this.configoptions = new ConfigOptions(configfile, maxpacksize, musicdir, packeddir, tempdir);
		playerips = configoptions.strictdownloaderlist ? new ConcurrentHashMap<Object,InetAddress>(16,0.75f,1) : null;
		data = new DataConfig(datafile);
		data.load();
		if(!datafile.exists()) {
			data.save();
		}

        this.packsender = new VelocityPackSender(server);
		this.amusic = new AMusic(configoptions, data, packsender, new VelocitySoundStarter(server), new VelocitySoundStopper(server), playerips);
		this.resourcemanager = amusic.resourcemanager;
		this.positiontracker = amusic.positiontracker;
		this.server = server;
        this.logger = logger;
        logger.info("AMusic loaded!");
    }
	
	@Subscribe
	public void onProxyInitialization(ProxyInitializeEvent event) {
		LoadmusicCommand loadmusic = new LoadmusicCommand(server, configoptions, data, resourcemanager, positiontracker, packsender);
		PlaymusicCommand playmusic = new PlaymusicCommand(server, positiontracker);
		RepeatCommand repeat = new RepeatCommand(server, positiontracker);
		CommandManager cmdmanager = this.server.getCommandManager();
		CommandMeta loadmusicmeta = cmdmanager.metaBuilder("loadmusic").plugin(this).build(), playmusicmeta = cmdmanager.metaBuilder("playmusic").plugin(this).build(), repeatmeta = cmdmanager.metaBuilder("repeat").plugin(this).build();
		cmdmanager.register(loadmusicmeta, loadmusic);
		cmdmanager.register(playmusicmeta, playmusic);
		cmdmanager.register(repeatmeta, repeat);
		this.server.getEventManager().register(this, new EventListener(resourcemanager, positiontracker, playerips));
		this.amusic.enable();
        logger.info("AMusic enabled!");
	}
	
	@Subscribe
	public void onProxyInitialization(ProxyShutdownEvent event) {
		this.amusic.disable();
        logger.info("AMusic disabled!");
	}
	
}
