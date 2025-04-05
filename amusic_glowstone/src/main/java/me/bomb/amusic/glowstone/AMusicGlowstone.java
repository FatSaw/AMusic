package me.bomb.amusic.glowstone;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Server;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.Configuration;
import me.bomb.amusic.LocalAMusic;
import me.bomb.amusic.MessageSender;
import me.bomb.amusic.PackSender;
import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.SoundStarter;
import me.bomb.amusic.ClientAMusic;
import me.bomb.amusic.SoundStopper;
import me.bomb.amusic.glowstone.command.LoadmusicCommand;
import me.bomb.amusic.glowstone.command.LoadmusicTabComplete;
import me.bomb.amusic.glowstone.command.PlaymusicCommand;
import me.bomb.amusic.glowstone.command.PlaymusicTabComplete;
import me.bomb.amusic.glowstone.command.RepeatCommand;
import me.bomb.amusic.glowstone.command.RepeatTabComplete;
import me.bomb.amusic.glowstone.command.SelectorProcessor;
import me.bomb.amusic.glowstone.command.UploadmusicCommand;
import me.bomb.amusic.glowstone.command.UploadmusicTabComplete;
import me.bomb.amusic.util.LangOptions;
import net.glowstone.GlowServer;
import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.source.LocalConvertedSource;
import me.bomb.amusic.source.LocalUnconvertedSource;
import me.bomb.amusic.source.SoundSource;


public final class AMusicGlowstone extends JavaPlugin {
	
	private static AMusic instance = null;
	
	private final AMusic amusic;
	private final ConcurrentHashMap<Object,InetAddress> playerips;
	private final boolean waitacception;
	private final String configerrors, uploaderhost;
	private final ResourceManager resourcemanager;
	private final PositionTracker positiontracker;

	public AMusicGlowstone() {
		Path plugindir = this.getDataFolder().toPath(), configfile = plugindir.resolve("config.yml"), langfile = plugindir.resolve("lang.yml"), musicdir = plugindir.resolve("Music"), packeddir = plugindir.resolve("Packed");
		FileSystem fs = plugindir.getFileSystem();
		FileSystemProvider fsp = fs.provider();
		try {
			fsp.createDirectory(plugindir);
		} catch (IOException e) {
		}
		boolean waitacception = true;
		Configuration config = new Configuration(plugindir.getFileSystem(), configfile, musicdir, packeddir, waitacception, true);
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
			if(config.connectuse) {
				this.waitacception = false;
				this.playerips = null;
				this.uploaderhost = null;
				ClientAMusic amusic = new ClientAMusic(config.connectifip, config.connectremoteip, config.connectport);
				this.resourcemanager = null;
				this.positiontracker = null;
				this.amusic = amusic;
			} else {
				PackSender packsender = new GlowstonePackSender();
				SoundStarter soundstarter;
				SoundStopper soundstopper = new GlowstoneSoundStopper();
				
				switch (GlowServer.PROTOCOL_VERSION) {
				case 340:
					packsender = new GlowstonePackSender();
					soundstarter = new GlowstoneLegacySoundStarter();
					soundstopper = new GlowstoneLegacySoundStopper();
				break;
				default:
					packsender = new GlowstonePackSender();
					soundstarter = new GlowstoneSoundStarter();
					soundstopper = new GlowstoneSoundStopper();
				break;
				}
				this.waitacception = config.waitacception;
				this.uploaderhost = config.uploadhost;
				playerips = config.sendpackstrictaccess || config.uploadstrictaccess ? new ConcurrentHashMap<Object,InetAddress>(16,0.75f,1) : null;
				Runtime runtime = Runtime.getRuntime();
				SoundSource source = config.encoderuse ? new LocalUnconvertedSource(runtime, config.musicdir, config.packsizelimit, config.encoderbinary, config.encoderbitrate, config.encoderchannels, config.encodersamplingrate, config.packthreadcoefficient, config.packthreadlimitcount) : new LocalConvertedSource(config.musicdir, config.packsizelimit, config.packthreadcoefficient, config.packthreadlimitcount);
				LocalAMusic amusic = new LocalAMusic(config, source, packsender, soundstarter, soundstopper, playerips);
				this.resourcemanager = amusic.resourcemanager;
				this.positiontracker = amusic.positiontracker;
				this.amusic = amusic;
			}
			MessageSender messagesender = new GlowstoneMessageSender();
			
			LangOptions.loadLang(messagesender, langfile, GlowServer.PROTOCOL_VERSION > 578);
			if(AMusicGlowstone.instance == null) {
				AMusicGlowstone.instance = this.amusic;
			}
		} else {
			this.waitacception = false;
			this.playerips = null;
			this.uploaderhost = null;
			this.resourcemanager = null;
			this.positiontracker = null;
			this.amusic = null;
		}
	}
	
	public final static AMusic API() {
		return instance;
	}

	//PLUGIN INIT START
	public void onEnable() {
		final Server server = this.getServer();
		if(!this.configerrors.isEmpty()) {
			server.getLogger().severe("AMusic config initialization errors: \n".concat(configerrors));
			return;
		}
		if(this.amusic == null) {
			return;
		}
		SelectorProcessor selectorprocessor = new SelectorProcessor(server, new Random());
		PluginCommand loadmusiccommand = getCommand("loadmusic");
		loadmusiccommand.setExecutor(new LoadmusicCommand(server, amusic, selectorprocessor));
		loadmusiccommand.setTabCompleter(new LoadmusicTabComplete(server, amusic));
		PlaymusicTabComplete pmtc = new PlaymusicTabComplete(server, amusic);
		PluginCommand playmusiccommand = getCommand("playmusic");
		playmusiccommand.setExecutor(new PlaymusicCommand(server, amusic, selectorprocessor, true));
		playmusiccommand.setTabCompleter(pmtc);
		PluginCommand playmusicntrackablecommand = getCommand("playmusicuntrackable");
		playmusicntrackablecommand.setExecutor(new PlaymusicCommand(server, amusic, selectorprocessor, false));
		playmusicntrackablecommand.setTabCompleter(pmtc);
		PluginCommand repeatcommand = getCommand("repeat");
		repeatcommand.setExecutor(new RepeatCommand(server, amusic, selectorprocessor));
		repeatcommand.setTabCompleter(new RepeatTabComplete(server));
		PluginCommand uploadmusiccommand = getCommand("uploadmusic");
		UploadmusicCommand uploadmusiccmd = new UploadmusicCommand(amusic, uploaderhost);
		uploadmusiccommand.setExecutor(uploadmusiccmd);
		uploadmusiccommand.setTabCompleter(new UploadmusicTabComplete(amusic));
		if(playerips != null) {
			playerips.clear();
			for(Player player : server.getOnlinePlayers()) {
				playerips.put(player, player.getAddress().getAddress());
			}
		}
		PluginManager pluginmanager = server.getPluginManager();
		if(this.resourcemanager != null) {
			pluginmanager.registerEvents(new EventListener(resourcemanager, positiontracker, playerips, uploadmusiccmd), this);
			if(waitacception) {
				pluginmanager.registerEvents(new PackStatusEventListener(resourcemanager), this);
			}
		}
		
		this.amusic.enable();
	}

	public void onDisable() {
		if(this.amusic == null) {
			return;
		}
		this.amusic.disable();
	}
	//PLUGIN INIT END

}
