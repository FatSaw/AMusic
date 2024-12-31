package me.bomb.amusic;

import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import me.bomb.amusic.resource.StatusReport;

public final class RemoteAMusic implements AMusic {

	@Override
	public void enable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<String> getPlaylists() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getPlaylistSoundnames(String playlistname) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getPlaylistSoundnames(UUID playeruuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Short> getPlaylistSoundlengths(String playlistname) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Short> getPlaylistSoundlengths(UUID playeruuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setRepeatMode(UUID playeruuid, RepeatType repeattype) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getPlayingSoundName(UUID playeruuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public short getPlayingSoundSize(UUID playeruuid) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public short getPlayingSoundRemain(UUID playeruuid) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void loadPack(UUID[] playeruuid, String name, boolean update, StatusReport statusreport) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getPackName(UUID playeruuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void stopSound(UUID playeruuid) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stopSoundUntrackable(UUID playeruuid) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void playSound(UUID playeruuid, String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void playSoundUntrackable(UUID playeruuid, String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public UUID openUploadSession(String playlistname) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Enumeration<UUID> getUploadSessions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean closeUploadSession(UUID token) {
		// TODO Auto-generated method stub
		return false;
	}

}
