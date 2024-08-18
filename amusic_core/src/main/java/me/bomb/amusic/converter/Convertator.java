package me.bomb.amusic.converter;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

public final class Convertator {
	
	private final String fmpegbinarypath;
	private final short threadcount;
	private final int bitrate, samplingrate;
	private final byte channels;
	public final ConvertWatcher[] watcherthreads;
	
	public Convertator(File fmpegbinary, short threadcount, int bitrate, byte channels, int samplingrate) {
		this.watcherthreads = new ConvertWatcher[threadcount];
		this.fmpegbinarypath = fmpegbinary.getAbsolutePath();
		this.threadcount = threadcount;
		this.bitrate = bitrate;
		this.channels = channels;
		this.samplingrate = samplingrate;
	}
	
	public void end() {
		short threadcount = this.threadcount;
		while(--threadcount>-1) {
			if(this.watcherthreads[threadcount] == null) continue;
			this.watcherthreads[threadcount].end();
			this.watcherthreads[threadcount] = null;
		}
	}
	
	public void init() {
		end();
		short threadcount = this.threadcount;
		while(--threadcount>-1) {
			this.watcherthreads[threadcount] = new ConvertWatcher(fmpegbinarypath, bitrate, channels, samplingrate);
		}
	}
	
	public ConvertatorSession convertTask(ConvertationFiles[] convertationfiles, short threadcount) {
		if(threadcount > this.threadcount) {
			threadcount = this.threadcount;
		}
		ConvertWatcher[] threads = new ConvertWatcher[threadcount];
		
		Random rng = new Random();
		Set<Integer> generated = new LinkedHashSet<Integer>();
		while (generated.size() < threadcount) {
		    Integer next = rng.nextInt(this.threadcount);
		    generated.add(next);
		}
		
		int i = threadcount;
		Iterator<Integer> iterator = generated.iterator();
		while(--i > -1) {
			threads[i] = watcherthreads[iterator.next()];
		}
		return new ConvertatorSession(convertationfiles, threads);
	}
	
	public static final class ConvertatorSession implements Runnable {
		private final ConvertationFiles[] convertationfiles;
		private final ConvertWatcher[] threads;
		ConvertatorSession(ConvertationFiles[] convertationfiles, ConvertWatcher[] threads) {
			this.convertationfiles = convertationfiles;
			this.threads = threads;
		}
		@Override
		public void run() {
			boolean convertationrunning = true;
			short checkcount = 0;
			while (convertationrunning && ++checkcount != 0) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
				}
				boolean finished = true;
				
				int i = threads.length;
				while(--i > -1 && !threads[i].paused);
				if(i == -1) continue; //THREADS BUSY

				ConvertWatcher thread = threads[i];
				i = convertationfiles.length;
				while(--i > -1 && ((finished &= convertationfiles[i].finished) || convertationfiles[i].started));
				if(i != -1) thread.convert(convertationfiles[i]);
				
				convertationrunning = !finished;
			}
		}
	}
}
