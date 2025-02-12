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
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
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
import me.bomb.amusic.source.LocalUnconvertedParallelSource;
import me.bomb.amusic.source.LocalUnconvertedSource;
import me.bomb.amusic.source.SoundSource;
import me.bomb.amusic.util.LangOptions;
import me.bomb.amusic.velocity.command.LoadmusicCommand;
import me.bomb.amusic.velocity.command.PlaymusicCommand;
import me.bomb.amusic.velocity.command.RepeatCommand;
import me.bomb.amusic.velocity.command.UploadmusicCommand;

@Plugin(id = "amusic", name = "AMusic", dependencies = {@Dependency(id = "protocolize")}, version = "0.16", authors = {"Bomb"})
public final class AMusicVelocity {
	
	private final ProxyServer server;
    private final Logger logger;
	private final AMusic amusic;
	private final ResourceManager resourcemanager;
	private final ConcurrentHashMap<Object,InetAddress> playerips;
	private final PositionTracker positiontracker;
	private final String configerrors, uploaderhost;
    
	@Inject
	public AMusicVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
		File plugindir = dataDirectory.toFile(), configfile = new File(plugindir, "config.yml"), langfile = new File(plugindir, "lang.yml"), musicdir = new File(plugindir, "Music"), packeddir = new File(plugindir, "Packed");
		if(!plugindir.exists()) {
			plugindir.mkdirs();
		}
		if(!musicdir.exists()) {
			musicdir.mkdir();
		}
		if(!packeddir.exists()) {
			packeddir.mkdir();
		}
		Configuration config = new Configuration(configfile, musicdir, packeddir, true, false);
		if(config.use) {
			this.configerrors = config.errors;
			this.uploaderhost = config.uploadhost;
			playerips = config.sendpackstrictaccess || config.uploadstrictaccess ? new ConcurrentHashMap<Object,InetAddress>(16,0.75f,1) : null;

			PackSender packsender = new VelocityPackSender(server);
	        
			Runtime runtime = Runtime.getRuntime();
			SoundSource<?> source = config.encoderuse ? config.encodetracksasync ? new LocalUnconvertedParallelSource(runtime, config.musicdir, config.packsizelimit, config.encoderbinary, config.encoderbitrate, config.encoderchannels, config.encodersamplingrate) : new LocalUnconvertedSource(runtime, config.musicdir, config.packsizelimit, config.encoderbinary, config.encoderbitrate, config.encoderchannels, config.encodersamplingrate) : new LocalConvertedSource(config.musicdir, config.packsizelimit);
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
		} else {
			this.configerrors = null;
			this.playerips = null;
			this.uploaderhost = null;
			this.resourcemanager = null;
			this.positiontracker = null;
			this.server = null;
	        this.logger = null;
			this.amusic = null;
		}
    }
	
	@Subscribe
	public void onProxyInitialization(ProxyInitializeEvent event) {
		if(this.amusic == null) {
			return;
		}
		if(!this.configerrors.isEmpty()) {
			logger.error("AMusic filed to load config options: \n".concat(configerrors));
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
