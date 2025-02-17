package me.bomb.amusic.uploader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ServerSocketFactory;

import static me.bomb.amusic.util.NameFilter.filterName;

public final class UploadManager extends Thread {
	
	private final int expiretime, limitsize, limitcount;
	private final File musicdir;
	private final ConcurrentHashMap<UUID, UploadSession> sessions;
	private final UploaderServer server;
	private boolean run;
	
	public UploadManager(int expiretime, int limitsize, int limitcount, File musicdir, final ConcurrentHashMap<Object, InetAddress> onlineips, final InetAddress ip, final int port, final int backlog, final ServerSocketFactory serverfactory) {
		this.expiretime = expiretime;
		this.limitsize = limitsize;
		this.limitcount = limitcount;
		this.musicdir = musicdir;
		this.sessions = new ConcurrentHashMap<>();
		this.server = new UploaderServer(this, onlineips, ip, port, backlog, serverfactory);
	}
	
	@Override
	public void start() {
		run = true;
		super.start();
		server.start();
	}

	public void end() {
		run = false;
		server.end();
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
	
	public UUID startSession(String targetplaylist) {
		final UUID token = UUID.randomUUID();
		targetplaylist = filterName(targetplaylist);
		if(targetplaylist == null) {
			targetplaylist = "";
		}
		sessions.put(token, new UploadSession(limitsize, limitcount, targetplaylist));
		return token;
	}
	
	public UploadSession getSession(UUID token) {
		return sessions.get(token);
	}
	
	public UUID[] getSessions() {
		synchronized (sessions) {
			int i = sessions.size();
			UUID[] tokens = new UUID[i];
			Enumeration<UUID> keys = sessions.keys();
			while(keys.hasMoreElements() && --i > -1) {
				tokens[i] = keys.nextElement();
			}
			return tokens;
		}
	}
	
	public boolean endSession(UUID token, boolean save) {
		UploadSession session;
		ConcurrentHashMap<String, byte[]> uploadentrys;
		if(token == null || (session = sessions.remove(token)) == null || (uploadentrys = session.endSession()) == null) {
			return false;
		}
		if(!save) {
			return true;
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
	
}
