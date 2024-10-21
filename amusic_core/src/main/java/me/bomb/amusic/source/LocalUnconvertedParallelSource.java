package me.bomb.amusic.source;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LocalUnconvertedParallelSource extends SoundSource {
	
	private final Runtime runtime;
	private final File musicdir, fmpegbinary;
	private final int maxsoundsize, bitrate, samplingrate;
	private final byte channels;
	
	public LocalUnconvertedParallelSource(Runtime runtime, File musicdir, int maxsoundsize, File fmpegbinary, int bitrate, byte channels, int samplingrate) {
		this.runtime = runtime;
		this.musicdir = musicdir;
		this.maxsoundsize = maxsoundsize;
		this.fmpegbinary = fmpegbinary;
		this.bitrate = bitrate;
		this.samplingrate = samplingrate;
		this.channels = channels;
	}

	@Override
	public SourceEntry get(String entrykey) {
		String[] args = new String[] {fmpegbinary.getAbsolutePath(), "-i", null, "-strict", "-2", "-acodec", "vorbis", "-ab", Integer.toString(bitrate), "-ac", Byte.toString(channels), "-ar", Integer.toString(samplingrate), "-f", "ogg", "-vn", "-y", "pipe:1"};
		File musicdir = new File(this.musicdir, entrykey);
		ArrayList<File> musicfiles = new ArrayList<>();
		ArrayList<String> soundnames = new ArrayList<>();
		for (File musicfile : musicdir.listFiles()) {
			if (musicfile.length() > maxsoundsize) continue;
			musicfiles.add(musicfile);
			String songname = musicfile.getName();
			int i = songname.lastIndexOf(".");
			if (i != -1) {
				songname = songname.substring(0, i);
			}
			soundnames.add(songname);
		}
		int i = musicfiles.size();
		String[] names = soundnames.toArray(new String[i]);
		short[] lengths = new short[i];
		byte[][] data = new byte[i][];
		AtomicBoolean[] finished = new AtomicBoolean[i];
		boolean[] success = new boolean[i];
		SourceEntry source = new SourceEntry(names, lengths, data, finished, success);
		while(--i > -1) {
			finished[i] = new ConvertThread(runtime, data, lengths, i, args, musicfiles.get(i), maxsoundsize, success).finished;
		}
		return source; //Need to wait until convertation ends to use source
	}
	
	private final static class ConvertThread extends Thread {
		private final Runtime runtime;
		private final byte[][] data;
		private short[] lengths;
		private final int index;
		private final String[] args;
		private final File infile;
		private final int maxsoundsize;
		protected final AtomicBoolean finished = new AtomicBoolean(false);
		protected final boolean[] success;
		
		private ConvertThread(Runtime runtime, byte[][] data, short[] lengths, int index, String[] args, File infile, int maxsoundsize, boolean[] success) {
			super();
			this.runtime = runtime;
			this.data = data;
			this.lengths = lengths;
			this.index = index;
			this.args = args;
			this.infile = infile;
			this.maxsoundsize = maxsoundsize;
			this.success = success;
			start();
		}
		
		public void run() {
			String[] args = Arrays.copyOf(this.args, this.args.length);
			args[2] = infile.getAbsolutePath();
			Process ffmpeg = null;
			try {
				ffmpeg = runtime.exec(args);
			} catch (IOException | SecurityException e) {
				finished.set(true);
				return;
			}
			if(ffmpeg == null) {
				finished.set(true);
				return;
			}
			
			InputStream is = null;
			try {
				is = ffmpeg.getInputStream();
				ByteArrayOutputStream bos = new ByteArrayOutputStream(maxsoundsize);
				int b;
				while((b = is.read()) != -1) {
					bos.write(b);
				}
				byte[] resource = bos.toByteArray();
				lengths[index] = calculateDuration(resource);
				data[index] = resource;
				success[index] = true;
			} catch (IOException e) {
			} finally {
				if(is != null) {
					try {
						is.close();
					} catch (IOException e) {
					}
				}
			}
			finished.set(true);
		}
		
	}

}
