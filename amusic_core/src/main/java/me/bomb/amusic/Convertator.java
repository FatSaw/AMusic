package me.bomb.amusic;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

public final class Convertator {
	
	private final String fmpegbinarypath;
	private final short threadcount;
	private final int bitrate, samplingrate;
	private final byte channels;
	private final ConvertatorThread[] watcherthreads;
	
	public Convertator(File fmpegbinary, short threadcount, int bitrate, byte channels, int samplingrate) {
		this.watcherthreads = new ConvertatorThread[threadcount];
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
			this.watcherthreads[threadcount] = new ConvertatorThread(fmpegbinarypath, bitrate, channels, samplingrate);
		}
	}
	
	public ConvertatorSession convertTask(ConvertationFiles[] convertationfiles, short threadcount) {
		if(this.threadcount > threadcount) {
			threadcount = this.threadcount;
		}
		ConvertatorThread[] threads = new ConvertatorThread[threadcount];
		
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
	
	protected static final class ConvertatorSession implements Runnable {
		private final ConvertationFiles[] convertationfiles;
		private final ConvertatorThread[] threads;
		ConvertatorSession(ConvertationFiles[] convertationfiles, ConvertatorThread[] threads) {
			this.convertationfiles = convertationfiles;
			this.threads = threads;
		}
		@Override
		public void run() {
			boolean convertationrunning = true;
			byte checkcount = 0;
			while (convertationrunning) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				boolean finished = true;
				
				int i = threads.length;
				while(--i > -1) {
					if(!threads[i].isPaused()) {
						continue;
					}
					int j = convertationfiles.length;
					while(--j > -1 && finished) {
						finished &= convertationfiles[j].finished;
					}
					threads[i].convert(convertationfiles[j]);
				}
				
				convertationrunning = !finished;
				if (++checkcount == 0) {
					return; // drop task if not finished for 4 minutes
				}
			}
		}
	}
	
	protected static final class ConvertatorThread extends Thread {
		
		private boolean paused = false, run = true;
		private final String[] args;
		private ConvertationFiles convertationfiles;

		ConvertatorThread(String fmpegbinarypath, int bitrate, byte channels, int samplingrate) {
			this.args = new String[] {fmpegbinarypath, "-i", null, "-strict", "-2", "-acodec", "vorbis", "-ab", Integer.toString(bitrate), "-ac", Byte.toString(channels), "-ar", Integer.toString(samplingrate), "-f", "ogg", "-vn", "-y", null};
			start();
		}
		
		protected void convert(ConvertationFiles convertationfiles) {
			if(!this.paused || convertationfiles.taken || convertationfiles.finished) {
				return;
			}
			convertationfiles.taken = true;
			this.args[2] = convertationfiles.input.getAbsolutePath();
			this.args[17] = convertationfiles.output.getAbsolutePath();
			synchronized(this) {
				this.notify();
			}
			this.convertationfiles = convertationfiles;
		}
		
		@Override
		public void run() {
			while(run) {
				this.paused = true;
				synchronized(this) {
					try {
						wait();
					} catch (InterruptedException e) {
					}
				}
				this.paused = false;
				Runtime runtime = Runtime.getRuntime();
				Process ffmpeg = null;
				try {
					ffmpeg = runtime.exec(args);
				} catch (IOException | SecurityException e) {
					return;
				}
				if(ffmpeg == null) {
					return;
				}
				
				ProcessKiller ffmpegKiller = new ProcessKiller(ffmpeg);
				runtime.addShutdownHook(ffmpegKiller);
				try {
					boolean exiterror = ffmpeg.waitFor() != 0;
					runtime.removeShutdownHook(ffmpegKiller);
					if(exiterror) {
						return;
					}
				} catch (InterruptedException ex) {
					return;
				}
				
				this.args[2] = null;
				this.args[17] = null;
				this.convertationfiles.finished = true;
			}
		}
		
		protected boolean isPaused() {
			return this.paused;
		}
		
		protected void end() {
			run = false;
			synchronized(this) {
				this.notify();
			}
		}
	}

	private final static class ProcessKiller extends Thread {
		private final Process process;

		private ProcessKiller(Process process) {
			this.process = process;
		}

		@Override
		public void run() {
			process.destroy();
		}
	}
	
	protected final static class ConvertationFiles {
		protected final File input, output;
		protected boolean taken, finished;
		ConvertationFiles(File input, File output) {
			this.input = input;
			this.output = output;
		}
	}
}
