package me.bomb.amusic;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import me.bomb.amusic.packedinfo.DataStorage;
import me.bomb.amusic.packedinfo.SoundInfo;
import me.bomb.amusic.resource.ResourceDispatcher;
import me.bomb.amusic.resource.ResourceFactory;
import me.bomb.amusic.resource.StatusReport;
import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.resourceserver.ResourceServer;
import me.bomb.amusic.source.SoundSource;
import me.bomb.amusic.uploader.UploadManager;
import me.bomb.amusic.uploader.UploaderServer;

public final class AMusic {
	
	private static AMusic instance;
	public final SoundSource<?> source;
	public final PositionTracker positiontracker;
	public final ResourceManager resourcemanager;
	public final ResourceServer resourceserver;
	public final ResourceDispatcher dispatcher;
	public final DataStorage datamanager;
	public final UploadManager uploadermanager;
	public final UploaderServer uploader;
	
	public AMusic(ConfigOptions configoptions, SoundSource<?> source, PackSender packsender, SoundStarter soundstarter, SoundStopper soundstopper, ConcurrentHashMap<Object,InetAddress> playerips) {
		this.source = source;
		this.resourcemanager = new ResourceManager(configoptions.maxpacksize, configoptions.servercache, configoptions.clientcache ? configoptions.tokensalt : null, configoptions.waitacception);
		this.positiontracker = new PositionTracker(soundstarter, soundstopper);
		this.resourceserver = new ResourceServer(configoptions.resourcestrictaccess ? playerips : null, configoptions.ip, configoptions.port, configoptions.backlog, resourcemanager);
		this.dispatcher = new ResourceDispatcher(packsender, resourcemanager, positiontracker, configoptions.host);
		this.datamanager = new DataStorage(configoptions.packeddir, !configoptions.processpack, (byte)2);
		this.uploadermanager = configoptions.useuploader ? new UploadManager(configoptions.uploadertimeout, configoptions.uploaderlimit, configoptions.musicdir, configoptions.uploaderhost) : null;
		this.uploader = configoptions.useuploader ? new UploaderServer(this.uploadermanager, configoptions.uploaderstrictaccess ? playerips : null, configoptions.uploaderip, configoptions.uploaderport, configoptions.uploaderbacklog) : null;
	}
	
	/**
	 * Starts {@link AMusic#positiontracker} and {@link AMusic#resourceserver} threads.
	 */
	public void enable() {
		positiontracker.start();
		resourceserver.start();
		datamanager.start();
		if(this.uploader != null) uploader.start();
		datamanager.load();
	}
	
	/**
	 * Stops {@link AMusic#positiontracker} and {@link AMusic#resourceserver} threads.
	 */
	public void disable() {
		positiontracker.end();
		resourceserver.end();
		datamanager.end();
		if(this.uploader != null) uploader.end();
		while (positiontracker.isAlive() || resourceserver.isAlive()) { //DONT STOP)
		}
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
		return datamanager.getPlaylists();
	}

	/**
	 * Get the names of sounds in playlist.
	 *
	 * @return the names of sounds in playlist.
	 */
	public List<String> getPlaylistSoundnames(String playlistname) {
		SoundInfo[] soundinfos = datamanager.getPlaylist(playlistname).sounds;
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
		SoundInfo[] soundinfos = datamanager.getPlaylist(playlistname).sounds;
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
	public void loadPack(UUID[] playeruuid, String name, boolean update, StatusReport statusreport) {
		new ResourceFactory(name, playeruuid, datamanager, dispatcher, source, update, statusreport);
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
	
	/**
	 * Open upload session.
	 * 
	 * @return session token.
	 */
	public UUID openUploadSession(String playlistname) {
		return uploadermanager == null ? null : uploadermanager.generateToken(playlistname);
	}
	
	/**
	 * Close upload session.
	 * 
	 * @return true if session closed successfully.
	 */
	public boolean closeUploadSession(UUID token) {
		return uploadermanager == null ? false : uploadermanager.endSession(token);
	}
}
