package me.bomb.amusic.uploader;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

final class UploadSession {
	
	private final int limitsize, limitcount;
	protected final String targetplaylist;
	private final ConcurrentHashMap<String, byte[]> uploadentrys;
	private final AtomicInteger size = new AtomicInteger(0);
	private boolean end;
	private long lastaccesstime;
	
	protected UploadSession(int limitsize, int limitcount, String targetplaylist) {
		this.limitsize = limitsize;
		this.limitcount = limitcount;
		this.targetplaylist = targetplaylist;
		this.uploadentrys = new ConcurrentHashMap<>();
		resetTime();
	}
	
	private final void resetTime() {
		this.lastaccesstime = System.currentTimeMillis();
	}
	
	protected final long getTime() {
		return System.currentTimeMillis() - this.lastaccesstime;
	}
	
	protected boolean canPut(int size) {
		if(end) {
			return false;
		}
		resetTime();
		return uploadentrys.size() < limitcount && this.size.get() + size < this.limitsize;
	}
	
	protected void put(String soundname, byte[] sound) {
		if(end || soundname == null || sound == null || size.get() + sound.length>limitsize) {
			return;
		}
		resetTime();
		size.addAndGet(sound.length);
		uploadentrys.put(soundname, sound);
	}
	
	protected void remove(String soundname) {
		byte[] sound;
		if(end || soundname == null || (sound = uploadentrys.remove(soundname)) == null) {
			return;
		}
		resetTime();
		size.addAndGet(-sound.length);
	}
	
	protected ConcurrentHashMap<String, byte[]> endSession() {
		if(end) {
			return null;
		}
		end = true;
		return uploadentrys;
	}
	
}
