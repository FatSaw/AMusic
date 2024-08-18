package me.bomb.amusic.converter;

import java.io.IOException;

final class ConvertWatcher extends Thread {
	
	private static int threadNumber;
    private static synchronized int nextThreadNum() {
        return threadNumber++;
    }
    
    protected boolean paused = false;
    private boolean run = true;
	private final String[] args;
	private ConvertationFiles convertationfiles;

	ConvertWatcher(String fmpegbinarypath, int bitrate, byte channels, int samplingrate) {
		super("ConvertWatcher-" + nextThreadNum());
		this.args = new String[] {fmpegbinarypath, "-i", null, "-strict", "-2", "-acodec", "vorbis", "-ab", Integer.toString(bitrate), "-ac", Byte.toString(channels), "-ar", Integer.toString(samplingrate), "-f", "ogg", "-vn", "-y", null};
		start();
	}
	
	public void convert(ConvertationFiles convertationfiles) {
		convertationfiles.started = true;
		this.args[2] = convertationfiles.input.getAbsolutePath();
		this.args[17] = convertationfiles.output.getAbsolutePath();
		this.convertationfiles = convertationfiles;
		synchronized(this) {
			this.notify();
		}
	}
	
	@Override
	public void run() {
		Runtime runtime = Runtime.getRuntime();
		Process ffmpeg = null;
		while(run) {
			ffmpeg = null;
			this.paused = true;
			synchronized(this) {
				try {
					wait();
				} catch (InterruptedException e) {
				}
			}
			if(this.args[2] == null || this.args[17] == null) {
				continue;
			}
			this.paused = false;
			try {
				ffmpeg = runtime.exec(args);
			} catch (IOException | SecurityException e) {
				continue;
			}
			if(ffmpeg == null) {
				continue;
			}
			
			FFmpegKiller ffmpegKiller = new FFmpegKiller(ffmpeg);
			runtime.addShutdownHook(ffmpegKiller);

			try {
				boolean exiterror = ffmpeg.waitFor() != 0;
				runtime.removeShutdownHook(ffmpegKiller);
				if(exiterror) {
					continue;
				}
			} catch (InterruptedException ex) {
				continue;
			}
			this.args[2] = null;
			this.args[17] = null;
			this.convertationfiles.finished = true;
			this.convertationfiles = null;
		}
	}
	
	public void end() {
		run = false;
		synchronized(this) {
			this.notify();
		}
	}
}
