package me.bomb.amusic.source;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConvertSound extends ReadSound {
	
	protected final Runtime runtime;
	protected final String[] args;

	public ConvertSound(Runtime runtime, String[] args, int maxsoundsize, int[] sizes, Path[] files, byte[][] data, short[] lengths, AtomicBoolean[] finished, boolean[] success, int start, int end) {
		super(maxsoundsize, sizes, files, data, lengths, finished, success, start, end);
		this.runtime = runtime;
		this.args = Arrays.copyOf(args, args.length);
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
					ByteArrayOutputStream baos = new ByteArrayOutputStream(maxsoundsize);
					int b;
					while((b = is.read()) != -1) {
						baos.write(b);
					}
					buf = baos.toByteArray();
				}
				data[i] = buf;
				lengths[i] = calculateDuration(buf);
				success[i] = true;
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
			if(finished == null) {
				continue;
			}
			finished[i].set(true);
		}
	}

}
