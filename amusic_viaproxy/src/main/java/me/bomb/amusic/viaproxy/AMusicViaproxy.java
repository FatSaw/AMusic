package me.bomb.amusic.viaproxy;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;

import me.bomb.amusic.AMusic;
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
import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.util.AMusicLogger;
import me.bomb.amusic.viaproxy.command.Command;
import me.bomb.amusic.viaproxy.command.LoadmusicCommand;
import me.bomb.amusic.viaproxy.command.PlaymusicCommand;
import me.bomb.amusic.viaproxy.command.RepeatCommand;
import net.lenni0451.lambdaevents.LambdaManager;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.plugins.ViaProxyPlugin;
import net.raphimc.viaproxy.plugins.events.ClientLoggedInEvent;
import net.raphimc.viaproxy.plugins.events.ConsoleCommandEvent;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

public final class AMusicViaproxy extends ViaProxyPlugin {
	
	private final me.bomb.amusic.util.Logger logger;
	
	private final AMusic amusic;
	private GeyserHook geyserhook = null;
	
	private final ConsoleCommandHandler consolecommand;
	private final LoginLogoutHandler loginlogout;
	
	public AMusicViaproxy() {
		this.logger = new me.bomb.amusic.util.Logger() {
			
			private final org.apache.logging.log4j.Logger logger = LogManager.getLogger("AMusic");
			
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
		AMusicLogger.setLogger(this.logger);
		Path plugindir = this.getDataFolder().toPath(), mergezip = plugindir.resolve("resourcepack.zip"), configfile = plugindir.resolve("config.yml"), musicdir = plugindir.resolve("Music"), packeddir = plugindir.resolve("Packed");
		FileSystem fs = plugindir.getFileSystem();
		FileSystemProvider fsp = fs.provider();
		try {
			fsp.createDirectory(plugindir);
		} catch (IOException e) {
		}
		Configuration config = new Configuration(fs, configfile, musicdir, packeddir, false);
		String configerrors = config.errors;
		
		if(!configerrors.isEmpty()) {
			throw new IllegalStateException("AMusic config initialization errors: \n".concat(configerrors));
		}
		if(!config.use) {
			this.amusic = null;
			this.consolecommand = null;
			this.loginlogout = null;
			return;
		}
		try {
			fsp.createDirectory(musicdir);
		} catch (IOException e) {
		}
		try {
			fsp.createDirectory(packeddir);
		} catch (IOException e) {
		}
		ConcurrentHashMap<UUID,ProxyConnection> players = new ConcurrentHashMap<UUID,ProxyConnection>(16,0.75f,1);
		ConcurrentHashMap<Object,InetAddress> playerips = config.sendpackstrictaccess ? new ConcurrentHashMap<Object,InetAddress>(16,0.75f,1) : null;

		PackSender packsender = new ViaproxyPackSender(players);
        
		LocalConvertedZerocopySource lczs = new LocalConvertedZerocopySource(musicdir, config.packsizelimit, config.packsizelimit, config.packthreadcoefficient, config.packthreadlimitcount);
		PositionTracker positiontracker = new PositionTracker(new ViaproxySoundStarter(players), new ViaproxySoundStopper(players));
		ResourceManager resourcemanager = new ResourceManager(packsender, positiontracker, config.sendpackhost, config.packsizelimit, config.tokensalt, config.sendpackstrictaccess ? playerips.values() : null, config.sendpackifip, config.sendpackport, config.sendpackbacklog, config.sendpacktimeout, config.sendpackserverfactory, config.sendpackacceptthreads, config.sendpackexecutorsender, config.waitacceptioncount, config.waitacceptionwait, config.waitacceptionschedulerthreads);
		PackMergeSourceLocal packmergesource = new PackMergeSourceLocal(new PackMergeEntryFile(mergezip, config.packsizelimit), musicdir, config.packsizelimit);
		Data datamanager = config.ramcache ? config.diskstore ? Data.getLocalCachedStorage(!config.processpack, lczs, packmergesource, packeddir) : Data.getRamStorage(!config.processpack, lczs, packmergesource) : config.diskstore ? Data.getLocalStorage(!config.processpack, lczs, packmergesource, packeddir) : Data.getNoStorage(!config.processpack, lczs, packmergesource);
		if(config.connectuse) {
			this.amusic = new ServerAMusic(this.logger, config.executor, lczs, positiontracker, resourcemanager, datamanager, config.connectifip, config.connectremoteip, config.connectport, config.connectbacklog, config.connectserverfactory, config.serverexecutor);
		} else {
			this.amusic = new LocalAMusic(this.logger, config.executor, lczs, positiontracker, resourcemanager, datamanager);
		}
		ConcurrentHashMap<String, UUID> uuidByPlayername = new ConcurrentHashMap<String, UUID>(16,0.75f,1);

		ConsoleCommandHandler consolecommand = null;
		LoginLogoutHandler loginlogout = null;
		if(config.usecmd) {
			Command loadmusic = new LoadmusicCommand(this.amusic, uuidByPlayername), playmusic = new PlaymusicCommand(this.amusic, uuidByPlayername), repeat = new RepeatCommand(this.amusic, uuidByPlayername);
			consolecommand = new ConsoleCommandHandler(this.logger, loadmusic, playmusic, repeat);
		}
		loginlogout = new LoginLogoutHandler(this.amusic, players, playerips, config.joinplaylist, uuidByPlayername);

		this.consolecommand = consolecommand;
		this.loginlogout = loginlogout;
	}

	@Override
	public void onEnable() {
		if(this.amusic == null) {
			return;
		}
		this.amusic.enable();
		final Data data = ((LocalAMusic) this.amusic).datamanager;
		new Thread("GeyserHookLoader") {
			@Override
			public void run() {
				byte i = 10;
				while(--i > -1) {
					try {
						sleep(1000);
					} catch (InterruptedException e) {
					}
					try {
						AMusicViaproxy.this.geyserhook = new GeyserHook(this, data);
						logger.info("Geyser hook loaded");
						return;
					} catch(NoClassDefFoundError e) {
						return;
					} catch(RuntimeException e) {
					}
				}
			}
		}.start();
		LambdaManager eventManager = ViaProxy.EVENT_MANAGER;
		if(this.consolecommand != null) {
			eventManager.registerConsumer(this.consolecommand, ConsoleCommandEvent.class);
		}
		if(this.loginlogout != null) {
			eventManager.registerConsumer(this.loginlogout, ClientLoggedInEvent.class);
		}
	}
	
	@Override
    public void onDisable() {
		if(this.geyserhook != null) {
			this.geyserhook.unregister();
		}
		if(this.amusic == null) {
			return;
		}
		LambdaManager eventManager = ViaProxy.EVENT_MANAGER;
		if(this.consolecommand != null) {
			eventManager.unregisterConsumer(this.consolecommand, ConsoleCommandEvent.class);
		}
		if(this.loginlogout != null) {
			eventManager.unregisterConsumer(this.loginlogout, ClientLoggedInEvent.class);
		}
		this.amusic.disable();
	}
	
}
