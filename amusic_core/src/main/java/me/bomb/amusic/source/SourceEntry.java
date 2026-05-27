package me.bomb.amusic.source;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SourceEntry {
	
	public final String[] names;
	public final byte[] splits;
	public final short[] lengths;
	public final UUID[] soundhashs;
	public final byte[][][] data;
	private final AtomicBoolean[] finished;
	public final boolean[] success;
	
	public SourceEntry(String[] names, byte[] splits, short[] lengths, UUID[] soundhashs, byte[][][] data, AtomicBoolean[] finished, boolean[] success) {
		this.names = names;
		this.splits = splits;
		this.lengths = lengths;
		this.soundhashs = soundhashs;
		this.data = data;
		this.finished = finished;
		this.success = success;
	}
	
	public boolean finished() {
		if(finished == null) {
			return true;
		}
		int i = finished.length;
		while(--i > -1) {
			if(finished[i].get()) continue;
			return false;
		}
		return true;
	}
	
	public boolean finished(int i) {
		return finished == null || finished[i].get();
	}

}
