package me.bomb.amusic;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

final class Converter implements Runnable {
	
	private final String[] args;
	private final AtomicBoolean status;
	protected final File input, output;

	protected Converter(File fmpegbinary, boolean async,int bitrate, byte channels, int samplingrate, File input, File output) {
		this.input = input;
		this.output = output;
		this.args = new String[] {fmpegbinary.getAbsolutePath(), "-i", input.getAbsolutePath(), "-strict", "-2", "-acodec", "vorbis", "-ab", Integer.toString(bitrate), "-ac", Byte.toString(channels), "-ar", Integer.toString(samplingrate), "-f", "ogg", "-vn", "-y", output.getAbsolutePath()};
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
