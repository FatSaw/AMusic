package me.bomb.amusic.sponge;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.Configuration;
import me.bomb.amusic.LocalAMusic;
import me.bomb.amusic.MessageSender;
import me.bomb.amusic.PackSender;
import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.SoundStarter;
import me.bomb.amusic.ClientAMusic;
import me.bomb.amusic.SoundStopper;
import me.bomb.amusic.util.LangOptions;
import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.source.LocalConvertedSource;
import me.bomb.amusic.source.LocalUnconvertedSource;
import me.bomb.amusic.source.MusicdirFStaticPackSource;
import me.bomb.amusic.source.MusicdirPackSource;
import me.bomb.amusic.source.PackSource;
import me.bomb.amusic.source.SoundSource;
import me.bomb.amusic.source.StaticPackSource;
import me.bomb.amusic.sponge.command.LoadmusicCommand;
import me.bomb.amusic.sponge.command.PlaymusicCommand;
import me.bomb.amusic.sponge.command.RepeatCommand;
import me.bomb.amusic.sponge.command.UploadmusicCommand;

import org.slf4j.Logger;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppedServerEvent;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.Player;

public final class AMusicSponge7 {
	
	private static AMusic instance = null;
	
	private AMusic amusic;
	private ConcurrentHashMap<Object,InetAddress> playerips;
	private boolean waitacception;
	private String configerrors, uploaderhost;
	private ResourceManager resourcemanager;
	private PositionTracker positiontracker;
	
	@Inject
	private Logger logger;
	
	@Inject
	@ConfigDir(sharedRoot = false)
	private Path privateConfigDir;
	

	public AMusicSponge7() {
		
	}
	
	public final static AMusic API() {
		return instance;
	}
	//PLUGIN INIT START
	@Listener
    public void onServerStart(GameStartedServerEvent event) {
		Path plugindir = privateConfigDir, configfile = plugindir.resolve("config.yml"), langfile = plugindir.resolve("lang.yml"), defaultresourcepackfile = plugindir.resolve("resourcepack.zip"), musicdir = plugindir.resolve("Music"), packeddir = plugindir.resolve("Packed");
		FileSystem fs = plugindir.getFileSystem();
		FileSystemProvider fsp = fs.provider();
		try {
			fsp.createDirectory(plugindir);
		} catch (IOException e) {
		}
		boolean waitacception = true;
		Configuration config = new Configuration(plugindir.getFileSystem(), configfile, musicdir, packeddir, waitacception, true);
		this.configerrors = config.errors;
		Server server = Sponge.getServer();
		if(config.use) {
			try {
				fsp.createDirectory(musicdir);
			} catch (IOException e) {
			}
			try {
				fsp.createDirectory(packeddir);
			} catch (IOException e) {
			}
			if(config.connectuse) {
				this.waitacception = false;
				this.playerips = null;
				this.uploaderhost = null;
				ClientAMusic amusic = new ClientAMusic(config.connectifip, config.connectremoteip, config.connectport);
				this.resourcemanager = null;
				this.positiontracker = null;
				this.amusic = amusic;
			} else {
				PackSender packsender = new SpongePackSender(server);
				SoundStarter soundstarter = new SpongeLegacySoundStarter(server);
				SoundStopper soundstopper = new SpongeSoundStopper(server);
				
				this.waitacception = config.waitacception;
				this.uploaderhost = config.uploadhost;
				playerips = config.sendpackstrictaccess || config.uploadstrictaccess ? new ConcurrentHashMap<Object,InetAddress>(16,0.75f,1) : null;
				Runtime runtime = Runtime.getRuntime();
				SoundSource soundsource = config.encoderuse ? new LocalUnconvertedSource(runtime, config.musicdir, config.packsizelimit, config.encoderbinary, config.encoderbitrate, config.encoderchannels, config.encodersamplingrate, config.packthreadcoefficient, config.packthreadlimitcount) : new LocalConvertedSource(config.musicdir, config.packsizelimit, config.packthreadcoefficient, config.packthreadlimitcount);
				PackSource packsource = new MusicdirFStaticPackSource(new MusicdirPackSource(musicdir, config.packsizelimit), new StaticPackSource(defaultresourcepackfile, config.packsizelimit));
				LocalAMusic amusic = new LocalAMusic(config, soundsource, packsource, packsender, soundstarter, soundstopper, playerips.values());
				this.resourcemanager = amusic.resourcemanager;
				this.positiontracker = amusic.positiontracker;
				this.amusic = amusic;
			}
			MessageSender messagesender = new SpongeMessageSender();
			
			LangOptions.loadLang(messagesender, langfile, false);
			if(AMusicSponge7.instance == null) {
				AMusicSponge7.instance = this.amusic;
			}
		} else {
			this.waitacception = false;
			this.playerips = null;
			this.uploaderhost = null;
			this.resourcemanager = null;
			this.positiontracker = null;
			this.amusic = null;
		}
		if(!this.configerrors.isEmpty()) {
			logger.error("AMusic config initialization errors: \n".concat(configerrors));
			return;
		}
		if(this.amusic == null) {
			return;
		}
		CommandManager cmdManager = Sponge.getCommandManager();
		
		cmdManager.register(this, new LoadmusicCommand(server, amusic), "loadmusic");
		cmdManager.register(this, new PlaymusicCommand(server, amusic, true), "playmusic");
		cmdManager.register(this, new PlaymusicCommand(server, amusic, false), "playmusicuntrackable");
		cmdManager.register(this, new RepeatCommand(server, amusic), "repeat");
		cmdManager.register(this, new UploadmusicCommand(amusic, uploaderhost), "uploadmusic");
		
		if(playerips != null) {
			playerips.clear();
			for(Player player : server.getOnlinePlayers()) {
				playerips.put(player, player.getConnection().getAddress().getAddress());
			}
		}
		
		EventManager eventmanager =  Sponge.getEventManager();
		eventmanager.registerListeners(this, new EventListener(resourcemanager, positiontracker, playerips, null));
		if(this.waitacception) {
			eventmanager.registerListeners(this, new PackStatusEventListener(resourcemanager));
		}
		
		this.amusic.enable();
    }
	
	@Listener
    public void onServerStop(GameStoppedServerEvent event) {
		if(this.amusic == null) {
			return;
		}
		this.amusic.disable();
    }
	//PLUGIN INIT END

}
