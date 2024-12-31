package me.bomb.amusic.uploader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static me.bomb.amusic.util.NameFilter.filterName;

public final class UploadManager extends Thread {
	
	private final int expiretime, limitsize;
	private final File musicdir;
	private final ConcurrentHashMap<UUID, UploadSession> sessions;
	private boolean run;
	
	public UploadManager(int expiretime, int limitsize, File musicdir) {
		this.expiretime = expiretime;
		this.limitsize = limitsize;
		this.musicdir = musicdir;
		sessions = new ConcurrentHashMap<>();
	}
	
	@Override
	public void start() {
		run = true;
		super.start();
	}

	public void end() {
		run = false;
	}
	
	@Override
	public void run() {
		while (run) {
			sessions.entrySet().removeIf(entry -> entry.getValue().getTime() > expiretime);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
	}
	
	public UUID generateToken(String targetplaylist) {
		final UUID token = UUID.randomUUID();
		targetplaylist = filterName(targetplaylist);
		if(targetplaylist == null) {
			targetplaylist = "";
		}
		sessions.put(token, new UploadSession(limitsize, 65535, targetplaylist));
		return token;
	}
	
	public UploadSession getSession(UUID token) {
		return sessions.get(token);
	}
	
	public Enumeration<UUID> getSessions() {
		return sessions.keys();
	}
	
	public boolean endSession(UUID token) {
		UploadSession session;
		ConcurrentHashMap<String, byte[]> uploadentrys;
		if(token == null || (session = sessions.remove(token)) == null || (uploadentrys = session.endSession()) == null) {
			return false;
		}
		File musicdir = new File(this.musicdir, session.targetplaylist);
		if(!musicdir.exists()) {
			musicdir.mkdir();
		}
		for(Entry<String, byte[]> entry : uploadentrys.entrySet()) {
			byte[] value = entry.getValue();
			File soundfile = new File(musicdir, entry.getKey().concat(".ogg"));
			if(value == null || value.length == 0) {
				soundfile.delete();
				continue;
			}
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(soundfile, false);
				fos.write(value);
			} catch(IOException ex) {
			} finally {
				if(fos == null) {
					continue;
				}
				try {
					fos.close();
				}  catch(IOException ex) {
				}
			}
		}
		return true;
	}
	
	public boolean remove(UUID token) {
		UploadSession session = sessions.remove(token);
		if(session == null) {
			return false;
		}
		session.endSession();
		return true;
	}
	
}
