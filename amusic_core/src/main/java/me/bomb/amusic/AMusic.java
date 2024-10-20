package me.bomb.amusic;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.resourceserver.ResourceServer;
import me.bomb.amusic.source.LocalConvertedSource;
import me.bomb.amusic.source.LocalUnconvertedParallelSource;
import me.bomb.amusic.source.LocalUnconvertedSource;
import me.bomb.amusic.source.SoundSource;

public final class AMusic {
	
	private static AMusic instance;
	public final SoundSource source;
	private final ConfigOptions configoptions;
	public final PositionTracker positiontracker;
	public final ResourceManager resourcemanager;
	public final ResourceServer resourceserver;
	private final PackSender packsender;
	private final Data data;
	
	public AMusic(ConfigOptions configoptions, Data data, PackSender packsender, SoundStarter soundstarter, SoundStopper soundstopper, ConcurrentHashMap<Object,InetAddress> playerips) {
		Runtime runtime = Runtime.getRuntime();
		this.source = configoptions.useconverter ? configoptions.encodetracksasynchronly ? new LocalUnconvertedParallelSource(runtime, configoptions.musicdir, configoptions.maxmusicfilesize, configoptions.ffmpegbinary, configoptions.bitrate, configoptions.channels, configoptions.samplingrate) : new LocalUnconvertedSource(runtime, configoptions.musicdir, configoptions.maxmusicfilesize, configoptions.ffmpegbinary, configoptions.bitrate, configoptions.channels, configoptions.samplingrate) : new LocalConvertedSource(configoptions.musicdir, configoptions.maxmusicfilesize);
		this.configoptions = configoptions;
		this.data = data;
		this.resourcemanager = new ResourceManager(configoptions.maxpacksize, configoptions.servercache, configoptions.clientcache, configoptions.tokensalt, configoptions.waitacception);
		this.positiontracker = new PositionTracker(soundstarter, soundstopper);
		this.packsender = packsender;
		this.resourceserver = new ResourceServer(playerips, configoptions.ip, configoptions.port, configoptions.backlog, resourcemanager);
	}
	
	/**
	 * Starts {@link AMusic#positiontracker} and {@link AMusic#resourceserver} threads.
	 */
	public void enable() {
		positiontracker.start();
		resourceserver.start();
	}
	
	/**
	 * Stops {@link AMusic#positiontracker} and {@link AMusic#resourceserver} threads.
	 */
	public void disable() {
		positiontracker.end();
		resourceserver.end();
		while (positiontracker.isAlive() || resourceserver.isAlive()) { //DONT STOP)
		}
	}
	
	public File getMusicDir() {
		return configoptions.musicdir;
	}
	
	/**
	 * Set main AMusic instance {@link AMusic#instance}.
	 * Should be used only during AMusic plugin initialization;
	 */
	public void setAPI() throws ExceptionInInitializerError {
		if(AMusic.instance!=null) throw new ExceptionInInitializerError("AMusic API already initialized!");
		AMusic.instance = this;
	}
	
	public static AMusic API() {
		return AMusic.instance;
	}
	
	/**
	 * Get the names of playlists that were loaded at least once.
	 *
	 * @return the names of playlists that were loaded at least once.
	 */
	public Set<String> getPlaylists() {
		return data.getPlaylists();
	}

	/**
	 * Get the names of sounds in playlist.
	 *
	 * @return the names of sounds in playlist.
	 */
	public List<String> getPlaylistSoundnames(String playlistname) {
		SoundInfo[] soundinfos = data.getPlaylist(playlistname).sounds;
		if(soundinfos==null) {
			return null;
		}
		int infossize = soundinfos.length;
		List<String> soundnames = new ArrayList<String>(infossize);
		for(int i = 0;i<infossize;++i) {
			soundnames.add(soundinfos[i].name);
		}
		return soundnames;
	}

	/**
	 * Get the names of sounds in playlist that loaded to player.
	 *
	 * @return the names of sounds in playlist that loaded to player.
	 */
	public List<String> getPlaylistSoundnames(UUID playeruuid) {
		SoundInfo[] soundinfos = positiontracker.getSoundInfo(playeruuid);
		if(soundinfos==null) {
			return null;
		}
		int infossize = soundinfos.length;
		List<String> soundnames = new ArrayList<String>(infossize);
		for(int i = 0;i<infossize;++i) {
			soundnames.add(soundinfos[i].name);
		}
		return soundnames;
	}

	/**
	 * Get the lenghs of sounds in playlist.
	 *
	 * @return the lenghs of sounds in playlist.
	 */
	public List<Short> getPlaylistSoundlengths(String playlistname) {
		SoundInfo[] soundinfos = data.getPlaylist(playlistname).sounds;
		if(soundinfos==null) {
			return null;
		}
		int infossize = soundinfos.length;
		List<Short> soundlengths = new ArrayList<Short>(infossize);
		for(int i = 0;i<infossize;++i) {
			soundlengths.add(soundinfos[i].length);
		}
		return soundlengths;
	}

	/**
	 * Get the lenghs of sounds in playlist that loaded to player.
	 *
	 * @return the lenghs of sounds in playlist that loaded to player.
	 */
	public List<Short> getPlaylistSoundlengths(UUID playeruuid) {
		SoundInfo[] soundinfos = positiontracker.getSoundInfo(playeruuid);
		if(soundinfos==null) {
			return null;
		}
		int infossize = soundinfos.length;
		List<Short> soundlengths = new ArrayList<Short>(infossize);
		for(int i = 0;i<infossize;++i) {
			soundlengths.add(soundinfos[i].length);
		}
		return soundlengths;
	}

	/**
	 * Set sound repeat mode, null to not repeat.
	 */
	public void setRepeatMode(UUID playeruuid, RepeatType repeattype) {
		positiontracker.setRepeater(playeruuid, repeattype);
	}

	/**
	 * Get playing sound name.
	 *
	 * @return playing sound name.
	 */
	public String getPlayingSoundName(UUID playeruuid) {
		return positiontracker.getPlaying(playeruuid);
	}

	/**
	 * Get playing sound size in seconds.
	 *
	 * @return playing sound size in seconds.
	 */
	public short getPlayingSoundSize(UUID playeruuid) {
		return positiontracker.getPlayingSize(playeruuid);
	}

	/**
	 * Get playing sound remaining seconds.
	 *
	 * @return playing sound remaining seconds.
	 */
	public int getPlayingSoundRemain(UUID playeruuid) {
		return positiontracker.getPlayingRemain(playeruuid);
	}

	/**
	 * Loads resource pack to player.
	 */
	public void loadPack(UUID[] playeruuid, String name, boolean update) throws FileNotFoundException {
		ResourceFactory.load(source, configoptions, data, resourcemanager, positiontracker, packsender, playeruuid, name, update);
	}

	/**
	 * Get loaded pack name.
	 *
	 * @return loaded pack name.
	 */
	public String getPackName(UUID playeruuid) {
		return positiontracker.getPlaylistName(playeruuid);
	}

	/**
	 * Stop sound from loaded pack.
	 */
	public void stopSound(UUID playeruuid) {
		positiontracker.stopMusic(playeruuid);
	}

	/**
	 * Play sound from loaded pack.
	 */
	public void playSound(UUID playeruuid, String name) {
		positiontracker.playMusic(playeruuid, name);
	}
}
