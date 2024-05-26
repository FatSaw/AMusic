package me.bomb.amusic;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

final class Converter implements Runnable {
	
	private static final String fmpegbinarypath;
	
	private final AtomicBoolean status;
	protected final File input, output;
	
	private final int bitrate, samplingrate;
	private final byte channels;
	
	static {
		String os = System.getProperty("os.name").toLowerCase();
		fmpegbinarypath = new File("plugins/AMusic/", "ffmpeg".concat(os.contains("windows") ? ".exe" : os.contains("mac") ? "-osx" : "")).getAbsolutePath();
	}

	protected Converter(boolean async,int bitrate, byte channels, int samplingrate ,File input, File output) {
		this.input = input;
		this.output = output.getAbsoluteFile();
		this.bitrate = bitrate;
		this.channels = channels;
		this.samplingrate = samplingrate;

		if (async) {
			status = new AtomicBoolean(false);
			new Thread(this).start();
		} else {
			status = null;
			run();
		}
	}

	@Override
	public void run() {
		Runtime runtime = Runtime.getRuntime();
		Process ffmpeg = null;
		try {
			ffmpeg = runtime.exec(new String[] {fmpegbinarypath, "-i", input.getAbsolutePath(), "-strict", "-2", "-acodec", "vorbis", "-ab", Integer.toString(bitrate), "-ac", Byte.toString(channels), "-ar", Integer.toString(samplingrate), "-f", "ogg", "-y", output.getAbsolutePath()});
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
		if (status != null) {
			status.set(true);
		}
	}
	
	protected boolean finished() {
		return status == null || status.get();
	}

	private final class ProcessKiller extends Thread {
		private final Process process;

		private ProcessKiller(Process process) {
			this.process = process;
		}

		@Override
		public void run() {
			process.destroy();
		}
	}
}
