package me.bomb.amusic.uploader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class UploadManager extends Thread {
	
	private final int expiretime, limit;
	private final File musicdir;
	public final String uploaderhost;
	private final ConcurrentHashMap<UUID, UploadSession> sessions;
	private boolean run;
	
	public UploadManager(int expiretime, int limit, File musicdir, String uploaderhost) {
		this.expiretime = expiretime;
		this.limit = limit;
		this.musicdir = musicdir;
		this.uploaderhost = uploaderhost;
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
		sessions.put(token, new UploadSession(limit, filterName(targetplaylist)));
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
	
	private static String filterName(String name) {
		char[] chars = name.toCharArray();
		int finalcount = 0;
		int i = chars.length;
		while(--i > -1) {
			char c = chars[i];
			//if(c == '/' || c == '\\' || c == ':' || c == '<' || c == '>' || c == '*' || c == '?' || c == '|' || c == '\"' || c == '\0' || (c > 0 && c < 32)) { // who use windows for servers
			if(c == '/' || c == '\0') { //unix
				chars[i] = '\0';
			} else {
				++finalcount;
			}
		}
		char[] filtered = new char[finalcount];
		int j = 0;
		while(++i < chars.length && j < finalcount) {
			char c = chars[i];
			if(c != '\0') {
				filtered[j] = c;
				++j;
			}
		}
		return new String(filtered);
	}
	
}
