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
import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.source.LocalConvertedSource;
import me.bomb.amusic.source.LocalUnconvertedSource;
import me.bomb.amusic.source.MusicdirFStaticPackSource;
import me.bomb.amusic.source.MusicdirPackSource;
import me.bomb.amusic.source.PackSource;
import me.bomb.amusic.source.SoundSource;
import me.bomb.amusic.source.StaticPackSource;
import me.bomb.amusic.util.AMusicLogger;
import me.bomb.amusic.viaproxy.command.Command;
import me.bomb.amusic.viaproxy.command.LoadmusicCommand;
import me.bomb.amusic.viaproxy.command.PlaymusicCommand;
import me.bomb.amusic.viaproxy.command.RepeatCommand;
import me.bomb.amusic.viaproxy.command.UploadmusicCommand;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.plugins.ViaProxyPlugin;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

public final class AMusicViaproxy extends ViaProxyPlugin {
	
	private static AMusic instance = null;
	
	private final org.apache.logging.log4j.Logger logger;
	
	private AMusic amusic;
	private ResourceManager resourcemanager;
	private ConcurrentHashMap<UUID,ProxyConnection> players;
	private ConcurrentHashMap<Object,InetAddress> playerips;
	private boolean usecmd;
	private PositionTracker positiontracker;
	private String configerrors, uploaderhost, joinplaylist;
	
	public AMusicViaproxy() {
		this.logger = LogManager.getLogger("AMusic");
		AMusicLogger.setLogger(new me.bomb.amusic.util.Logger() {
			private final org.apache.logging.log4j.Logger logger = AMusicViaproxy.this.logger;
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
		});
	}

	@Override
	public void onEnable() {
		Path plugindir = this.getDataFolder().toPath(), configfile = plugindir.resolve("config.yml"), defaultresourcepackfile = plugindir.resolve("resourcepack.zip"), musicdir = plugindir.resolve("Music"), packeddir = plugindir.resolve("Packed");
		FileSystem fs = plugindir.getFileSystem();
		FileSystemProvider fsp = fs.provider();
		try {
			fsp.createDirectory(plugindir);
		} catch (IOException e) {
		}
		Configuration config = new Configuration(fs, configfile, musicdir, packeddir, false, false);
		this.configerrors = config.errors;
		if(config.use) {
			try {
				fsp.createDirectory(musicdir);
			} catch (IOException e) {
			}
			try {
				fsp.createDirectory(packeddir);
			} catch (IOException e) {
			}
			this.usecmd = config.usecmd;
			this.uploaderhost = config.uploadhost;
			this.joinplaylist = config.joinplaylist;
			players = new ConcurrentHashMap<UUID,ProxyConnection>(16,0.75f,1);
			playerips = config.sendpackstrictaccess || config.uploadstrictaccess ? new ConcurrentHashMap<Object,InetAddress>(16,0.75f,1) : null;

			PackSender packsender = new ViaproxyPackSender(this.players);
	        
			Runtime runtime = Runtime.getRuntime();
			SoundSource soundsource = config.encoderuse ? new LocalUnconvertedSource(runtime, config.musicdir, config.packsizelimit, config.encoderbinary, config.encoderbitrate, config.encoderchannels, config.encodersamplingrate, config.packthreadcoefficient, config.packthreadlimitcount) : new LocalConvertedSource(config.musicdir, config.packsizelimit, config.packthreadcoefficient, config.packthreadlimitcount);
			PackSource packsource = new MusicdirFStaticPackSource(new MusicdirPackSource(musicdir, config.packsizelimit), new StaticPackSource(defaultresourcepackfile, config.packsizelimit));
			if(config.connectuse) {
				ServerAMusic amusic = new ServerAMusic(config, soundsource, packsource, packsender, new ViaproxySoundStarter(this.players), new ViaproxySoundStopper(this.players), playerips == null ? null : playerips.values());
				this.resourcemanager = amusic.resourcemanager;
				this.positiontracker = amusic.positiontracker;
				this.amusic = amusic;
			} else {
				LocalAMusic amusic = new LocalAMusic(config, soundsource, packsource, packsender, new ViaproxySoundStarter(this.players), new ViaproxySoundStopper(this.players), playerips == null ? null : playerips.values());
				this.resourcemanager = amusic.resourcemanager;
				this.positiontracker = amusic.positiontracker;
				this.amusic = amusic;
			}
			if(AMusicViaproxy.instance == null) {
				AMusicViaproxy.instance = this.amusic;
			}
		} else {
			this.usecmd = false;
			this.players = null;
			this.playerips = null;
			this.uploaderhost = null;
			this.joinplaylist = null;
			this.resourcemanager = null;
			this.positiontracker = null;
			this.amusic = null;
		}
		if(!this.configerrors.isEmpty()) {
			this.logger.info("AMusic config initialization errors: \n".concat(configerrors));
			return;
		}
		if(this.amusic == null) {
			return;
		}
		ConcurrentHashMap<String, UUID> uuidByPlayername = new ConcurrentHashMap<String, UUID>(16,0.75f,1);
		if(this.usecmd) {
			Command loadmusic = new LoadmusicCommand(this.amusic, uuidByPlayername), playmusic = new PlaymusicCommand(this.amusic, uuidByPlayername, true), playmusicuntrackable = new PlaymusicCommand(this.amusic, uuidByPlayername, false), repeat = new RepeatCommand(this.amusic, uuidByPlayername), uploadmusic = new UploadmusicCommand(this.amusic, this.uploaderhost);
			
			ViaProxy.EVENT_MANAGER.register(new ConsoleCommandListener(this.logger, loadmusic, playmusic, playmusicuntrackable, repeat, uploadmusic));
		}
		
		if(this.resourcemanager != null) {
			ViaProxy.EVENT_MANAGER.register(new EventListener(this.amusic, resourcemanager, positiontracker, players, playerips, joinplaylist, uuidByPlayername));
		}
		
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
						new GeyserHook(data);
						logger.info("Geyser hook loaded");
						return;
					} catch(NoClassDefFoundError e) {
						return;
					} catch(RuntimeException e) {
					}
				}
			}
		}.start();
		this.amusic.enable();
	}
	
	@Override
    public void onDisable() {
		if(this.amusic == null) {
			return;
		}
		this.amusic.disable();
	}
	
}
