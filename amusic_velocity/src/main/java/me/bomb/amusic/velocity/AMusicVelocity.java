package me.bomb.amusic.velocity;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
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
import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.uploader.UploadManager;
import me.bomb.amusic.util.AMusicLogger;
import me.bomb.amusic.util.LangOptions;
import me.bomb.amusic.velocity.command.LoadmusicCommand;
import me.bomb.amusic.velocity.command.PlaymusicCommand;
import me.bomb.amusic.velocity.command.RepeatCommand;
import me.bomb.amusic.velocity.command.UploadmusicCommand;
import me.bomb.amusic.velocity.event.DisconnectHandler;
import me.bomb.amusic.velocity.event.LoginHandler;
import me.bomb.amusic.velocity.event.PlayerResourcePackStatusHandler;
import me.bomb.amusic.velocity.event.ProxyShutdownHandler;

public final class AMusicVelocity {
	
	private final ProxyServer server;
	private final Configuration config;
	private final LocalAMusic amusic;
	private final ConcurrentHashMap<Object,InetAddress> playerips;
	
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
		Path plugindir = dataDirectory, configfile = plugindir.resolve("config.yml"), langfile = plugindir.resolve("lang.yml"), musicdir = plugindir.resolve("Music"), packeddir = plugindir.resolve("Packed");
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
		this.playerips = config.sendpackstrictaccess || config.uploadstrictaccess ? new ConcurrentHashMap<Object,InetAddress>(16,0.75f,1) : null;

		PackSender packsender = new VelocityPackSender(server);
		LocalConvertedZerocopySource lczs = new LocalConvertedZerocopySource(config.musicdir, config.packsizelimit, config.packsizelimit, config.packthreadcoefficient, config.packthreadlimitcount);
		PositionTracker positiontracker = new PositionTracker(new VelocitySoundStarter(server), new VelocitySoundStopper(server));
		ResourceManager resourcemanager = new ResourceManager(packsender, positiontracker, config.sendpackhost, config.packsizelimit, config.tokensalt, config.waitacception, config.sendpackstrictaccess ? playerips.values() : null, config.sendpackifip, config.sendpackport, config.sendpackbacklog, config.sendpacktimeout, config.sendpackserverfactory, (short) 2, config.sendpackexecutorchecker, config.sendpackexecutorsender);
		Data datamanager = config.ramcache ? Data.getRamStorage(!config.processpack, lczs) : Data.getNoStorage(!config.processpack, lczs);
		UploadManager uploadmanager = config.uploaduse ? new UploadManager(config.uploadlifetime, config.uploadlimitsize, config.uploadlimitcount, config.musicdir, config.uploadstrictaccess ? playerips.values() : null, config.uploadifip, config.uploadport, config.uploadbacklog, config.uploadtimeout, config.uploadserverfactory, (short) 2) : null;
		if(config.connectuse) {
			this.amusic = new ServerAMusic(amusiclogger, config.executor, lczs, positiontracker, resourcemanager, datamanager, uploadmanager, config.connectifip, config.connectremoteip, config.connectport, config.connectbacklog, config.connectserverfactory, config.serverexecutor);
		} else {
			this.amusic = new LocalAMusic(amusiclogger, config.executor, lczs, positiontracker, resourcemanager, datamanager, uploadmanager);
		}
		LangOptions.loadLang(new VelocityMessageSender(), langfile, false);
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
		UploadmusicCommand uploadmusic = null;
		
		if(config.usecmd) {
			LoadmusicCommand loadmusic = new LoadmusicCommand(server, amusic);
			PlaymusicCommand playmusic = new PlaymusicCommand(server, amusic, true), playmusicuntrackable = new PlaymusicCommand(server, amusic, false);
			RepeatCommand repeat = new RepeatCommand(server, amusic);
			uploadmusic = new UploadmusicCommand(amusic, config.uploadhost);
			CommandManager cmdmanager = server.getCommandManager();
			CommandMeta loadmusicmeta = cmdmanager.metaBuilder("loadmusic").plugin(this).build(), playmusicmeta = cmdmanager.metaBuilder("playmusic").plugin(this).build(), playmusicuntrackablemeta = cmdmanager.metaBuilder("playmusicuntrackable").plugin(this).build(), repeatmeta = cmdmanager.metaBuilder("repeat").plugin(this).build(), uploadmusicmeta = cmdmanager.metaBuilder("uploadmusic").plugin(this).build();
			cmdmanager.register(loadmusicmeta, loadmusic);
			cmdmanager.register(playmusicmeta, playmusic);
			cmdmanager.register(playmusicuntrackablemeta, playmusicuntrackable);
			cmdmanager.register(repeatmeta, repeat);
			cmdmanager.register(uploadmusicmeta, uploadmusic);
		}
		
		EventManager eventmanager = server.getEventManager();
		eventmanager.register(this, ProxyShutdownEvent.class, new ProxyShutdownHandler(amusic, geyser));
		eventmanager.register(this, LoginEvent.class, new LoginHandler(amusic, playerips, config.joinplaylist));
		eventmanager.register(this, DisconnectEvent.class, new DisconnectHandler(amusic, playerips, uploadmusic));
		eventmanager.register(this, PlayerResourcePackStatusEvent.class, new PlayerResourcePackStatusHandler(amusic.resourcemanager));
	}
	
}
