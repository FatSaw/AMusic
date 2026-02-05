package me.bomb.amusic.uploader;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

final class UploadSession {
	
	private final int limitsize, limitcount;
	protected final String targetplaylist;
	private final ConcurrentHashMap<String, byte[]> uploadingentrys, uploadentrys;
	private final AtomicInteger size = new AtomicInteger(0);
	private volatile boolean end;
	private volatile long lastaccesstime;
	
	protected UploadSession(int limitsize, int limitcount, String targetplaylist) {
		this.limitsize = limitsize;
		this.limitcount = limitcount;
		this.targetplaylist = targetplaylist;
		this.uploadingentrys = new ConcurrentHashMap<>();
		this.uploadentrys = new ConcurrentHashMap<>();
		resetTime();
	}
	
	private final void resetTime() {
		this.lastaccesstime = System.currentTimeMillis();
	}
	
	protected final long getTime() {
		return System.currentTimeMillis() - this.lastaccesstime;
	}
	
	protected boolean canPut(final String soundname, int size) {
		if(end) {
			return false;
		}
		resetTime();
		return uploadentrys.size() < limitcount && this.size.get() + size < this.limitsize && !uploadingentrys.containsKey(soundname);
	}
	
	protected byte[] tryAllocPutBuf(final String soundname, final int allocSize) {
		if (end || soundname == null || allocSize < 0) {
	    	return null;
	    }
	    resetTime();
	    
	    byte i = 16;
	    while (--i!=0) {
	    	final int current = size.get(), next = current + allocSize;
	        if (next > limitsize) {
	        	return null;
	        }
	        if (size.compareAndSet(current, next)) {
	        	break;
	        }
	        try {
				Thread.sleep(10L);
			} catch (InterruptedException e) {
			}
	    }
	    if (i==0) {
	    	return null;
	    }
	    byte[] buf = new byte[allocSize];
	    byte[] prev = uploadingentrys.putIfAbsent(soundname, buf);
	    if (prev != null) {
	    	size.addAndGet(-allocSize);
	        return null;
	    }
		if (uploadingentrys.size() > limitcount) {
			uploadingentrys.remove(soundname);
	        size.addAndGet(-allocSize);
	        return null;
		}
	    return buf;
	}
	
	protected void putDone(final String soundname) {
		final byte[] buf;
		if(end || (buf = uploadingentrys.remove(soundname)) == null) {
			return;
		}
		uploadentrys.put(soundname, buf);
	}
	
	protected byte[] query() {
		if(end) {
			return null;
		}
		resetTime();
		int bufsize = 2;
		int i = uploadentrys.size();
		byte[] keysizes = new byte[i];
		byte[] valuesizes = new byte[i<<2];
		byte[][] keysb = new byte[i][];
		bufsize += i;;
		bufsize += i << 2;
		Iterator<Entry<String, byte[]>> iterator = uploadentrys.entrySet().iterator();
		Entry<String, byte[]> entry;
		byte[] key, value;
		int length;
		while(--i > -1 && iterator.hasNext()) {
			length = (key = (entry = iterator.next()).getKey().getBytes(StandardCharsets.UTF_8)).length;
			if(length > 0xff) {
				length = 0xff;
				byte[] nkey = new byte[0xff];
				System.arraycopy(key, 0, nkey, 0, 0xff);
				key = nkey;
			}
			keysizes[i] = (byte) length;
			bufsize += length;
			if((value = entry.getValue()) != null) {
				length = value.length;
				int j = (1+i)<<2;
				valuesizes[--j] = (byte) length;
				length >>>= 8;
				valuesizes[--j] = (byte) length;
				length >>>= 8;
				valuesizes[--j] = (byte) length;
				length >>>= 8;
				valuesizes[--j] = (byte) length;
			}
			keysb[i] = key;
		}
		byte[] query = new byte[bufsize];
		i = keysb.length;
		while(--i > -1) {
			byte[] keys = keysb[i];
			length = keys.length;
			bufsize-=length;
			System.arraycopy(keys, 0, query, bufsize, length);
		}
		length = valuesizes.length;
		bufsize-=length;
		System.arraycopy(valuesizes, 0, query, bufsize, length);
		length = keysizes.length;
		bufsize-=length;
		System.arraycopy(keysizes, 0, query, bufsize, length);
		query[--bufsize] = (byte) length;
		length >>>= 8;
		query[--bufsize] = (byte) length;
		return query;
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
