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
import me.bomb.amusic.AMusic;
import me.bomb.amusic.ConfigOptions;
import me.bomb.amusic.PackSender;
import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.source.LocalConvertedSource;
import me.bomb.amusic.source.LocalUnconvertedParallelSource;
import me.bomb.amusic.source.LocalUnconvertedSource;
import me.bomb.amusic.source.SoundSource;
import me.bomb.amusic.velocity.command.LangOptions;
import me.bomb.amusic.velocity.command.LoadmusicCommand;
import me.bomb.amusic.velocity.command.PlaymusicCommand;
import me.bomb.amusic.velocity.command.RepeatCommand;

@Plugin(id = "amusic", name = "AMusic", dependencies = {@Dependency(id = "protocolize")}, version = "0.14", authors = {"Bomb"})
public class AMusicVelocity {
	
	private final ProxyServer server;
    //private final Logger logger;
	private final AMusic amusic;
	private final ResourceManager resourcemanager;
	private final ConcurrentHashMap<Object,InetAddress> playerips;
	private final PositionTracker positiontracker;
    
	@Inject
	public AMusicVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
		byte ver = 0;
		File plugindir = dataDirectory.toFile(), configfile = new File(plugindir, "config.yml"), langfile = new File(plugindir, "lang.yml"), musicdir = new File(plugindir, "Music"), packeddir = new File(plugindir, "Packed"), tempdir = new File(plugindir, "Temp");
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
		ConfigOptions configoptions = new ConfigOptions(configfile, maxpacksize, musicdir, packeddir, true);
		playerips = configoptions.resourcestrictaccess ? new ConcurrentHashMap<Object,InetAddress>(16,0.75f,1) : null;

		PackSender packsender = new VelocityPackSender(server);
        
		Runtime runtime = Runtime.getRuntime();
		SoundSource<?> source = configoptions.useconverter ? configoptions.encodetracksasynchronly ? new LocalUnconvertedParallelSource(runtime, configoptions.musicdir, configoptions.maxmusicfilesize, configoptions.ffmpegbinary, configoptions.bitrate, configoptions.channels, configoptions.samplingrate) : new LocalUnconvertedSource(runtime, configoptions.musicdir, configoptions.maxmusicfilesize, configoptions.ffmpegbinary, configoptions.bitrate, configoptions.channels, configoptions.samplingrate) : new LocalConvertedSource(configoptions.musicdir, configoptions.maxmusicfilesize);
		this.amusic = new AMusic(configoptions, source, packsender, new ProtocoliseSoundStarter(), new ProtocoliseSoundStopper(), playerips);
		this.resourcemanager = amusic.resourcemanager;
		this.positiontracker = amusic.positiontracker;
		this.server = server;
        //this.logger = logger;
		LangOptions.loadLang(langfile, false);
        amusic.setAPI();
    }
	
	@Subscribe
	public void onProxyInitialization(ProxyInitializeEvent event) {
		Protocolize.protocolRegistration().registerPacket(SoundStopPacket.MAPPINGS, Protocol.PLAY, PacketDirection.CLIENTBOUND, SoundStopPacket.class);
		Protocolize.protocolRegistration().registerPacket(NamedSoundEffectPacket.MAPPINGS, Protocol.PLAY, PacketDirection.CLIENTBOUND, NamedSoundEffectPacket.class);
		LoadmusicCommand loadmusic = new LoadmusicCommand(server, amusic.source, amusic.datamanager, amusic.dispatcher);
		PlaymusicCommand playmusic = new PlaymusicCommand(server, positiontracker, true), playmusicuntrackable = new PlaymusicCommand(server, positiontracker, false);
		RepeatCommand repeat = new RepeatCommand(server, positiontracker);
		CommandManager cmdmanager = this.server.getCommandManager();
		CommandMeta loadmusicmeta = cmdmanager.metaBuilder("loadmusic").plugin(this).build(), playmusicmeta = cmdmanager.metaBuilder("playmusic").plugin(this).build(), playmusicuntrackablemeta = cmdmanager.metaBuilder("playmusicuntrackable").plugin(this).build(), repeatmeta = cmdmanager.metaBuilder("repeat").plugin(this).build();
		cmdmanager.register(loadmusicmeta, loadmusic);
		cmdmanager.register(playmusicmeta, playmusic);
		cmdmanager.register(playmusicuntrackablemeta, playmusicuntrackable);
		cmdmanager.register(repeatmeta, repeat);
		this.server.getEventManager().register(this, new EventListener(resourcemanager, positiontracker, playerips));
		this.amusic.enable();
	}
	
	@Subscribe
	public void onProxyShutdown(ProxyShutdownEvent event) {
		this.amusic.disable();
	}
	
}
