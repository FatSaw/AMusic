package me.bomb.amusic.source;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import me.bomb.amusic.util.ByteArraysOutputStream;

public class ConvertSound extends ReadSound {
	protected final Runtime runtime;
	protected final String[] args;

	public ConvertSound(Runtime runtime, Path fmpegbinary, int bitrate, byte channels, int samplingrate, int maxsoundsize, byte[] splits, int[] sizes, Path[] files, byte[][][] data, short[] lengths, AtomicBoolean[] finished, boolean[] success, int start, int end) {
		super(maxsoundsize, splits, sizes, files, data, lengths, finished, success, start, end);
		this.runtime = runtime;
		this.args = new String[] {fmpegbinary.toAbsolutePath().toString(), "-i", null, "-strict", "-2", "-acodec", "vorbis", "-ab", Integer.toString(bitrate), "-ac", Byte.toString(channels), "-ar", Integer.toString(samplingrate), "-f", "ogg", "-vn", "-y", "pipe:1"};
	}
	
	@Override
	public void run() {
		final boolean sizesknown = sizes != null;
		for(int i = this.start; i < this.end;++i) {
			byte[] buf = sizesknown ? new byte[sizes[i]] : null;
			args[2] = files[i].toAbsolutePath().toString();
			Process ffmpeg = null;
			try {
				ffmpeg = runtime.exec(args);
			} catch (IOException | SecurityException e) {
				success[i] = false;
				if(finished != null) {
					finished[i].set(true);
				}
				return;
			}
			if(ffmpeg == null) {
				success[i] = false;
				if(finished != null) {
					finished[i].set(true);
				}
				return;
			}
			InputStream is = null;
			try {
				is = ffmpeg.getInputStream();
				if(sizesknown) {
					is.read(buf, 0, buf.length);
				} else {
					ByteArraysOutputStream baos = new ByteArraysOutputStream(maxsoundsize >> 14);
					byte[] buff;
					int b;
					while((b = is.read(buff = new byte[16384])) != -1) baos.write(buff, 0, b);
					buf = baos.toByteArray();
					baos.close();
				}
				is.close();
			} catch (IOException e1) {
				success[i] = false;
				if(is != null) {
					try {
						is.close();
					} catch (IOException e2) {
					}
				}
			}
			
			final byte split = splits[i];
			
			byte[][] parts = new byte[split][];
			
			if((split & 0xFE) != 0x00) {
				//TODO: IMPLEMENT SPLITTER
			}
			
			if((split & 0x01) == 0x01) {
				parts[parts.length - 1] = buf;
			}
			
			data[i] = parts;
			lengths[i] = calculateDuration(buf);
			success[i] = true;
			
			if(finished == null) {
				continue;
			}
			finished[i].set(true);
		}
	}

}
