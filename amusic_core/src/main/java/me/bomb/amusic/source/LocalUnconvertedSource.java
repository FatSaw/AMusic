package me.bomb.amusic.source;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;

public final class LocalUnconvertedSource extends SoundSource {
	
	private final Runtime runtime;
	private final File musicdir, fmpegbinary;
	private final int maxsoundsize, bitrate, samplingrate;
	private final byte channels;
	
	public LocalUnconvertedSource(Runtime runtime, File musicdir, int maxsoundsize, File fmpegbinary, int bitrate, byte channels, int samplingrate) {
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
		File musicdir = new File(this.musicdir, entrykey);
		if(musicdir == null || !musicdir.exists()) return null;
		String[] args = new String[] {fmpegbinary.getAbsolutePath(), "-i", null, "-strict", "-2", "-acodec", "vorbis", "-ab", Integer.toString(bitrate), "-ac", Byte.toString(channels), "-ar", Integer.toString(samplingrate), "-f", "ogg", "-vn", "-y", "pipe:1"};
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
		boolean[] success = new boolean[i];
		SourceEntry source = new SourceEntry(names, lengths, data, null, success);
		while(--i > -1) {
			File infile = musicfiles.get(i);
			
			args[2] = null;
			args[2] = infile.getAbsolutePath();
			
			Process ffmpeg = null;
			try {
				ffmpeg = runtime.exec(args);
			} catch (IOException | SecurityException e) {
				continue;
			}
			if(ffmpeg == null) {
				continue;
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
				lengths[i] = calculateDuration(resource);
				data[i] = resource;
				success[i] = true;
			} catch (IOException e) {
			} finally {
				if(is != null) {
					try {
						is.close();
					} catch (IOException e) {
					}
				}
			}
		}
		return source;
	}

	@Override
	public boolean exists(String entrykey) {
		File musicdir = new File(this.musicdir, entrykey);
		if(musicdir == null || !musicdir.exists()) return false;
		for (File musicfile : musicdir.listFiles()) {
			if (musicfile.length() > maxsoundsize) continue;
			return true;
		}
		return false;
	}
	
	@Override
	public Path getSource() {
		return this.musicdir.toPath();
	}

}
