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
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import dev.simplix.protocolize.api.PacketDirection;
import dev.simplix.protocolize.api.Protocol;
import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.providers.ProtocolRegistrationProvider;
import me.bomb.amusic.AMusic;
import me.bomb.amusic.Configuration;
import me.bomb.amusic.LocalAMusic;
import me.bomb.amusic.PackSender;
import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.ServerAMusic;
import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.source.LocalConvertedSource;
import me.bomb.amusic.source.LocalUnconvertedSource;
import me.bomb.amusic.source.SoundSource;
import me.bomb.amusic.util.LangOptions;
import me.bomb.amusic.velocity.command.LoadmusicCommand;
import me.bomb.amusic.velocity.command.PlaymusicCommand;
import me.bomb.amusic.velocity.command.RepeatCommand;
import me.bomb.amusic.velocity.command.UploadmusicCommand;

public final class AMusicVelocity {
	
	private static AMusic instance = null;
	
	private final ProxyServer server;
    private final Logger logger;
	private final AMusic amusic;
	private final ResourceManager resourcemanager;
	private final ConcurrentHashMap<Object,InetAddress> playerips;
	private final PositionTracker positiontracker;
	private final String configerrors, uploaderhost;
    
	@Inject
	public AMusicVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
		Path plugindir = dataDirectory, configfile = plugindir.resolve("config.yml"), langfile = plugindir.resolve("lang.yml"), musicdir = plugindir.resolve("Music"), packeddir = plugindir.resolve("Packed");
		FileSystem fs = plugindir.getFileSystem();
		FileSystemProvider fsp = fs.provider();
		try {
			fsp.createDirectory(plugindir);
		} catch (IOException e) {
		}
		boolean waitacception = true;
		Configuration config = new Configuration(plugindir.getFileSystem(), configfile, musicdir, packeddir, waitacception, false);
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
			this.uploaderhost = config.uploadhost;
			playerips = config.sendpackstrictaccess || config.uploadstrictaccess ? new ConcurrentHashMap<Object,InetAddress>(16,0.75f,1) : null;

			PackSender packsender = new VelocityPackSender(server);
	        
			Runtime runtime = Runtime.getRuntime();
			SoundSource source = config.encoderuse ? new LocalUnconvertedSource(runtime, config.musicdir, config.packsizelimit, config.encoderbinary, config.encoderbitrate, config.encoderchannels, config.encodersamplingrate, config.packthreadcoefficient, config.packthreadlimitcount) : new LocalConvertedSource(config.musicdir, config.packsizelimit, config.packthreadcoefficient, config.packthreadlimitcount);
			if(config.connectuse) {
				ServerAMusic amusic = new ServerAMusic(config, source, packsender, new ProtocoliseSoundStarter(), new ProtocoliseSoundStopper(server, true), playerips);
				this.resourcemanager = amusic.resourcemanager;
				this.positiontracker = amusic.positiontracker;
				this.amusic = amusic;
			} else {
				LocalAMusic amusic = new LocalAMusic(config, source, packsender, new ProtocoliseSoundStarter(), new ProtocoliseSoundStopper(server, true), playerips);
				this.resourcemanager = amusic.resourcemanager;
				this.positiontracker = amusic.positiontracker;
				this.amusic = amusic;
			}
			this.server = server;
	        this.logger = logger;
			LangOptions.loadLang(new VelocityMessageSender(), langfile, false);
			if(AMusicVelocity.instance == null) {
				AMusicVelocity.instance = this.amusic;
			}
		} else {
			this.playerips = null;
			this.uploaderhost = null;
			this.resourcemanager = null;
			this.positiontracker = null;
			this.server = null;
	        this.logger = null;
			this.amusic = null;
		}
    }
	
	public final static AMusic API() {
		return instance;
	}
	
	@Subscribe
	public void onProxyInitialization(ProxyInitializeEvent event) {
		if(!this.configerrors.isEmpty()) {
			logger.error("AMusic config initialization errors: \n".concat(configerrors));
			return;
		}
		if(this.amusic == null) {
			return;
		}
		ProtocolRegistrationProvider protocolregistration = Protocolize.protocolRegistration();
		protocolregistration.registerPacket(SoundStopPacket.MAPPINGS, Protocol.PLAY, PacketDirection.CLIENTBOUND, SoundStopPacket.class);
		protocolregistration.registerPacket(NamedSoundEffectPacket.MAPPINGS, Protocol.PLAY, PacketDirection.CLIENTBOUND, NamedSoundEffectPacket.class);
		LoadmusicCommand loadmusic = new LoadmusicCommand(server, amusic);
		PlaymusicCommand playmusic = new PlaymusicCommand(server, amusic, true), playmusicuntrackable = new PlaymusicCommand(server, amusic, false);
		RepeatCommand repeat = new RepeatCommand(server, amusic);
		UploadmusicCommand uploadmusic = new UploadmusicCommand(amusic, uploaderhost);
		CommandManager cmdmanager = this.server.getCommandManager();
		CommandMeta loadmusicmeta = cmdmanager.metaBuilder("loadmusic").plugin(this).build(), playmusicmeta = cmdmanager.metaBuilder("playmusic").plugin(this).build(), playmusicuntrackablemeta = cmdmanager.metaBuilder("playmusicuntrackable").plugin(this).build(), repeatmeta = cmdmanager.metaBuilder("repeat").plugin(this).build(), uploadmusicmeta = cmdmanager.metaBuilder("uploadmusic").plugin(this).build();
		cmdmanager.register(loadmusicmeta, loadmusic);
		cmdmanager.register(playmusicmeta, playmusic);
		cmdmanager.register(playmusicuntrackablemeta, playmusicuntrackable);
		cmdmanager.register(repeatmeta, repeat);
		cmdmanager.register(uploadmusicmeta, uploadmusic);
		if(this.resourcemanager != null) {
			this.server.getEventManager().register(this, new EventListener(resourcemanager, positiontracker, playerips, uploadmusic));
		}
		this.amusic.enable();
	}
	
	@Subscribe
	public void onProxyShutdown(ProxyShutdownEvent event) {
		if(this.amusic == null) {
			return;
		}
		this.amusic.disable();
	}
	
}
