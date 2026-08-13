package me.bomb.amusic.velocity;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import me.bomb.amusic.Configuration;
import me.bomb.amusic.GeyserHook;
import me.bomb.amusic.LocalAMusic;
import me.bomb.amusic.PackSender;
import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.ServerAMusic;
import me.bomb.amusic.packedinfo.Data;
import me.bomb.amusic.packedinfo.LocalConvertedZerocopySource;
import me.bomb.amusic.packedinfo.PackMergeEntryFile;
import me.bomb.amusic.packedinfo.PackMergeSourceLocal;
import me.bomb.amusic.permission.AMusicPermission;
import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.util.AMusicLogger;
import me.bomb.amusic.util.LangLoader;
import me.bomb.amusic.velocity.command.LoadmusicCommand;
import me.bomb.amusic.velocity.command.PlaymusicCommand;
import me.bomb.amusic.velocity.command.RepeatCommand;
import me.bomb.amusic.velocity.event.DisconnectHandler;
import me.bomb.amusic.velocity.event.LoginHandler;
import me.bomb.amusic.velocity.event.PlayerResourcePackStatusHandler;
import me.bomb.amusic.velocity.event.ProxyShutdownHandler;

public final class AMusicVelocity {
	
	private final ProxyServer server;
	private final Configuration config;
	private final LocalAMusic amusic;
	private final ConcurrentHashMap<Object,InetAddress> playerips;
	
	private final LoadmusicCommand loadmusic;
	private final PlaymusicCommand playmusic;
	private final RepeatCommand repeat;
	
	private final LoginHandler login;
	private final DisconnectHandler disconnect;
	private final PlayerResourcePackStatusHandler resourcepackstatus;
	
	@Inject
	public AMusicVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
		me.bomb.amusic.util.Logger amusiclogger = new me.bomb.amusic.util.Logger() {
			@Override
			public void warn(String msg) {
				logger.warn(msg);
			}
			
			@Override
			public void info(String msg) {
				logger.info(msg);
			}
			
			@Override
			public void error(String msg) {
				logger.error(msg);
			}
		};
		AMusicLogger.setLogger(amusiclogger);
		Path plugindir = dataDirectory, mergezip = plugindir.resolve("resourcepack.zip"), configfile = plugindir.resolve("config.yml"), langfile = plugindir.resolve("lang.yml"), musicdir = plugindir.resolve("Music"), packeddir = plugindir.resolve("Packed");
		FileSystem fs = plugindir.getFileSystem();
		FileSystemProvider fsp = fs.provider();
		try {
			fsp.createDirectory(plugindir);
		} catch (IOException e) {
		}
		boolean waitacception = true;
		Configuration config = new Configuration(fs, configfile, musicdir, packeddir, waitacception, false);
		String configerrors = config.errors;
		if(!configerrors.isEmpty()) {
			throw new IllegalStateException("AMusic config initialization errors: \n".concat(configerrors));
		}
		if(!config.use) {
			this.server = null;
			this.config = null;
			this.amusic = null;
			this.playerips = null;
			this.loadmusic = null;
			this.playmusic = null;
			this.repeat = null;
			this.login = null;
			this.disconnect = null;
			this.resourcepackstatus = null;
			return;
		}
		this.server = server;
		this.config = config;
		try {
			fsp.createDirectory(musicdir);
		} catch (IOException e) {
		}
		try {
			fsp.createDirectory(packeddir);
		} catch (IOException e) {
		}
		this.playerips = config.sendpackstrictaccess ? new ConcurrentHashMap<Object,InetAddress>(16,0.75f,1) : null;
		boolean rgb = false;
		
		PackSender packsender = new VelocityPackSender(server);
		LocalConvertedZerocopySource lczs = new LocalConvertedZerocopySource(config.musicdir, config.packsizelimit, config.packsizelimit, config.packthreadcoefficient, config.packthreadlimitcount);
		PositionTracker positiontracker = new PositionTracker(new VelocitySoundStarter(server), new VelocitySoundStopper(server));
		ResourceManager resourcemanager = new ResourceManager(packsender, positiontracker, config.sendpackhost, config.packsizelimit, config.tokensalt, config.waitacception, config.sendpackstrictaccess ? playerips.values() : null, config.sendpackifip, config.sendpackport, config.sendpackbacklog, config.sendpacktimeout, config.sendpackserverfactory, (short) 2, config.sendpackexecutorchecker, config.sendpackexecutorsender);
		PackMergeSourceLocal packmergesource = new PackMergeSourceLocal(new PackMergeEntryFile(mergezip, config.packsizelimit), config.musicdir, config.packsizelimit);
		Data datamanager = config.ramcache ? config.diskstore ? Data.getLocalCachedStorage(!config.processpack, lczs, packmergesource, packeddir) : Data.getRamStorage(!config.processpack, lczs, packmergesource) : config.diskstore ? Data.getLocalStorage(!config.processpack, lczs, packmergesource, packeddir) : Data.getNoStorage(!config.processpack, lczs, packmergesource);
		if(config.connectuse) {
			this.amusic = new ServerAMusic(amusiclogger, config.executor, lczs, positiontracker, resourcemanager, datamanager, config.connectifip, config.connectremoteip, config.connectport, config.connectbacklog, config.connectserverfactory, config.serverexecutor);
		} else {
			this.amusic = new LocalAMusic(amusiclogger, config.executor, lczs, positiontracker, resourcemanager, datamanager);
		}
		LangLoader lang = new LangLoader(langfile, rgb ? "lang_rgb.yml" : "lang_old.yml", new VelocityMessageSender());
		final ConcurrentHashMap<UUID, EnumSet<AMusicPermission>> playerspermission = new ConcurrentHashMap<UUID, EnumSet<AMusicPermission>>();
		LoadmusicCommand loadmusic = null;
		PlaymusicCommand playmusic = null;
		RepeatCommand repeat = null;
		if(config.usecmd) {
			loadmusic = new LoadmusicCommand(server, amusic, lang, playerspermission);
			playmusic = new PlaymusicCommand(server, amusic, lang, playerspermission);
			repeat = new RepeatCommand(server, amusic, lang, playerspermission);
		}
		LoginHandler login = null;
		DisconnectHandler disconnect = null;
		PlayerResourcePackStatusHandler resourcepackstatus;
		
		login = new LoginHandler(amusic, playerspermission, playerips, config.joinplaylist);
		disconnect = new DisconnectHandler(amusic, playerspermission, playerips);
		resourcepackstatus = new PlayerResourcePackStatusHandler(amusic.resourcemanager);
		
		this.loadmusic = loadmusic;
		this.playmusic = playmusic;
		this.repeat = repeat;
		
		this.login = login;
		this.disconnect = disconnect;
		this.resourcepackstatus = resourcepackstatus;
    }


	@Subscribe
	public void onProxyInitialize(ProxyInitializeEvent event) {
		this.amusic.enable();
		GeyserHook geyser = null;
		try {
			geyser = new GeyserHook(this, amusic.datamanager);
			AMusicLogger.info("Geyser hook loaded");
		} catch (NoClassDefFoundError e) {
		}
		
		
		if(config.usecmd) {
			CommandManager cmdmanager = this.server.getCommandManager();
			if(this.loadmusic != null) {
				CommandMeta loadmusicmeta =  cmdmanager.metaBuilder("loadmusic").plugin(this).build();
				cmdmanager.register(loadmusicmeta, this.loadmusic);
			}
			if(this.playmusic != null) {
				CommandMeta playmusicmeta = cmdmanager.metaBuilder("playmusic").plugin(this).build();
				cmdmanager.register(playmusicmeta, this.playmusic);
			}
			if(this.repeat != null) {
				CommandMeta repeatmeta = cmdmanager.metaBuilder("repeat").plugin(this).build();
				cmdmanager.register(repeatmeta, this.repeat);
			}
		}

		ProxyShutdownHandler proxyshutdown = new ProxyShutdownHandler(this.amusic, geyser);
		EventManager eventmanager = this.server.getEventManager();
		if(this.login != null) eventmanager.register(this, LoginEvent.class, this.login);
		if(this.disconnect != null) eventmanager.register(this, DisconnectEvent.class, this.disconnect);
		if(this.resourcepackstatus != null) eventmanager.register(this, PlayerResourcePackStatusEvent.class, this.resourcepackstatus);
		eventmanager.register(this, ProxyShutdownEvent.class, proxyshutdown);
	}
	
}
