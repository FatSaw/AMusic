package me.bomb.amusic;

import java.net.InetAddress;
import java.util.Collection;
import java.util.UUID;

import me.bomb.amusic.packedinfo.Data;
import me.bomb.amusic.packedinfo.SoundInfo;
import me.bomb.amusic.resource.ResourceDispatcher;
import me.bomb.amusic.resource.ResourceFactory;
import me.bomb.amusic.resource.StatusReport;
import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.source.PackSource;
import me.bomb.amusic.source.SoundSource;
import me.bomb.amusic.uploader.UploadManager;

public class LocalAMusic implements AMusic {
	public final SoundSource soundsource;
	public final PackSource packsource;
	public final PositionTracker positiontracker;
	public final ResourceManager resourcemanager;
	public final ResourceDispatcher dispatcher;
	public final Data datamanager;
	public final UploadManager uploadermanager;
	
	public LocalAMusic(Configuration config, SoundSource soundsource, PackSource packsource, PackSender packsender, SoundStarter soundstarter, SoundStopper soundstopper, Collection<InetAddress> playerips) {
		this.soundsource = soundsource;
		this.packsource = packsource;
		this.resourcemanager = new ResourceManager(config.packsizelimit, config.servercache, config.clientcache ? config.tokensalt : null, config.waitacception, config.sendpackstrictaccess ? playerips : null, config.sendpackifip, config.sendpackport, config.sendpackbacklog, config.sendpacktimeout, config.sendpackserverfactory, (short) 2);
		this.positiontracker = new PositionTracker(soundstarter, soundstopper);
		this.dispatcher = new ResourceDispatcher(packsender, resourcemanager, positiontracker, config.sendpackhost);
		this.datamanager = Data.getDefault(config.packeddir, !config.processpack);
		this.uploadermanager = config.uploaduse ? new UploadManager(config.uploadlifetime, config.uploadlimitsize, config.uploadlimitcount, config.musicdir, config.uploadstrictaccess ? playerips : null, config.uploadifip, config.uploadport, config.uploadbacklog, config.uploadtimeout, config.uploadserverfactory, (short) 2) : null;
	}
	
	/**
	 * Starts threads.
	 */
	public void enable() {
		positiontracker.start();
		resourcemanager.start();
		datamanager.start();
		if(this.uploadermanager != null) uploadermanager.start();
		datamanager.load();
	}
	
	/**
	 * Stops threads.
	 */
	public void disable() {
		positiontracker.end();
		resourcemanager.end();
		datamanager.end();
		if(this.uploadermanager != null) uploadermanager.end();
	}
	
	/**
	 * Get player uuids that loaded specific playlistname.
	 *
	 * @return player uuids that loaded specific playlistname.
	 */
	@Override
	public final UUID[] getPlayersLoaded(String playlistname) {
		return positiontracker.getPlayersLoaded(playlistname);
	}
	
	/**
	 * Get the names of playlists that were loaded at least once.
	 *
	 * @return the names of playlists that were loaded at least once.
	 */
	public final String[] getPlaylists(boolean packed) {
		return packed ? datamanager.getPlaylists() : soundsource.getPlaylists();
	}

	/**
	 * Get the names of sounds in playlist.
	 *
	 * @return the names of sounds in playlist.
	 */
	public final String[] getPlaylistSoundnames(String playlistname, boolean packed) {
		if(packed) {
			SoundInfo[] soundinfos = datamanager.getPlaylist(playlistname).sounds;
			if(soundinfos==null) {
				return null;
			}
			int i = soundinfos.length;
			String[] soundnames = new String[i];
			while(--i > -1) {
				soundnames[i] = soundinfos[i].name;
			}
			return soundnames;
		} else {
			return soundsource.getSounds(playlistname);
		}
	}

	/**
	 * Get the names of sounds in playlist that loaded to player.
	 *
	 * @return the names of sounds in playlist that loaded to player.
	 */
	public final String[] getPlaylistSoundnames(UUID playeruuid) {
		SoundInfo[] soundinfos = positiontracker.getSoundInfo(playeruuid);
		if(soundinfos==null) {
			return null;
		}
		int i = soundinfos.length;
		String[] soundnames = new String[i];
		while(--i > -1) {
			soundnames[i] = soundinfos[i].name;
		}
		return soundnames;
	}

	/**
	 * Get the lenghs of sounds in playlist.
	 *
	 * @return the lenghs of sounds in playlist.
	 */
	public final short[] getPlaylistSoundlengths(String playlistname) {
		SoundInfo[] soundinfos = datamanager.getPlaylist(playlistname).sounds;
		if(soundinfos==null) {
			return null;
		}
		int i = soundinfos.length;
		short[] soundlengths = new short[i];
		while(--i > -1) {
			soundlengths[i] = soundinfos[i].length;
		}
		return soundlengths;
	}

	/**
	 * Get the lenghs of sounds in playlist that loaded to player.
	 *
	 * @return the lenghs of sounds in playlist that loaded to player.
	 */
	public final short[] getPlaylistSoundlengths(UUID playeruuid) {
		SoundInfo[] soundinfos = positiontracker.getSoundInfo(playeruuid);
		if(soundinfos==null) {
			return null;
		}
		int i = soundinfos.length;
		short[] soundlengths = new short[i];
		while(--i > -1) {
			soundlengths[i] = soundinfos[i].length;
		}
		return soundlengths;
	}

	/**
	 * Set sound repeat mode, null to not repeat.
	 */
	public final void setRepeatMode(UUID playeruuid, RepeatType repeattype) {
		positiontracker.setRepeater(playeruuid, repeattype);
	}

	/**
	 * Get playing sound name.
	 *
	 * @return playing sound name.
	 */
	public final String getPlayingSoundName(UUID playeruuid) {
		return positiontracker.getPlaying(playeruuid);
	}

	/**
	 * Get playing sound size in seconds.
	 *
	 * @return playing sound size in seconds.
	 */
	public final short getPlayingSoundSize(UUID playeruuid) {
		return positiontracker.getPlayingSize(playeruuid);
	}

	/**
	 * Get playing sound remaining seconds.
	 *
	 * @return playing sound remaining seconds.
	 */
	public final short getPlayingSoundRemain(UUID playeruuid) {
		return positiontracker.getPlayingRemain(playeruuid);
	}

	/**
	 * Loads resource pack to player.
	 */
	public final void loadPack(UUID[] playeruuid, String name, boolean update, StatusReport statusreport) {
		new ResourceFactory(name, playeruuid, datamanager, dispatcher, soundsource, packsource, update, statusreport, true);
	}

	/**
	 * Get loaded pack name.
	 *
	 * @return loaded pack name.
	 */
	public final String getPackName(UUID playeruuid) {
		return positiontracker.getPlaylistName(playeruuid);
	}

	/**
	 * Stop sound from loaded pack.
	 */
	public final void stopSound(UUID playeruuid) {
		positiontracker.stopMusic(playeruuid);
	}
	
	/**
	 * Stop sound from loaded pack.
	 */
	public final void stopSoundUntrackable(UUID playeruuid) {
		positiontracker.stopMusicUntrackable(playeruuid);
	}

	/**
	 * Play sound from loaded pack.
	 */
	public final void playSound(UUID playeruuid, String name) {
		positiontracker.playMusic(playeruuid, name);
	}
	
	/**
	 * Play sound from loaded pack.
	 */
	public final void playSoundUntrackable(UUID playeruuid, String name) {
		positiontracker.playMusicUntrackable(playeruuid, name);
	}
	
	/**
	 * Open upload session.
	 * 
	 * @return session token.
	 */
	public final UUID openUploadSession(String playlistname) {
		return uploadermanager == null ? null : uploadermanager.startSession(playlistname);
	}
	
	/**
	 * Get upload sessions.
	 * 
	 * @return upload sessions.
	 */
	public final UUID[] getUploadSessions() {
		return uploadermanager == null ? null : uploadermanager.getSessions();
	}
	
	/**
	 * Close upload session.
	 * 
	 * @return true if session closed successfully.
	 */
	public final boolean closeUploadSession(UUID token, boolean save) {
		return uploadermanager == null ? false : uploadermanager.endSession(token, save);
	}
	
}
