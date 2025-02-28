package me.bomb.amusic.source;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.concurrent.atomic.AtomicBoolean;

import me.bomb.amusic.util.ByteArraysOutputStream;

public class ReadSound implements Runnable {

	protected final int maxsoundsize;
	protected final int[] sizes;
	protected final Path[] files;
	protected final byte[][] data;
	protected final short[] lengths;
	protected final AtomicBoolean[] finished;
	protected final boolean[] success;
	protected final int start, end;
	
	public ReadSound(int maxsoundsize, int[] sizes, Path[] files, byte[][] data, short[] lengths, AtomicBoolean[] finished , boolean[] success, int start, int end) {
		this.maxsoundsize = maxsoundsize;
		this.sizes = sizes;
		this.files = files;
		this.data = data;
		this.lengths = lengths;
		this.finished = finished;
		this.success = success;
		this.start = start;
		this.end = end;
	}

	@Override
	public void run() {
		final boolean sizesknown = sizes != null;
		for(int i = this.start; i < this.end;++i) {
			byte[] buf = sizesknown ? new byte[sizes[i]] : null;
			InputStream is = null;
			try {
				FileSystemProvider fs = files[i].getFileSystem().provider();
				is = fs.newInputStream(files[i]);
				if(sizesknown) {
					is.read(buf, 0, buf.length);
				} else {
					ByteArraysOutputStream baos = new ByteArraysOutputStream(maxsoundsize >> 14);
					byte[] buff;
					int b;
					while((b = is.read(buff = new byte[16384])) != -1) {
						if(b < 16384) {
							byte[] nbuff = new byte[b];
							System.arraycopy(buff, 0, nbuff, 0, b);
							buff = nbuff;
						}
						baos.write(buff);
					}
					buf = baos.toByteArray();
					baos.close();
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
	
	protected static short calculateDuration(byte[] t) {
		int rate = -1, length = -1, size = t.length;
		for (int i = size - 15; i >= 0 && length < 0; i--) {
			if (t[i] == (byte) 'O' && t[i + 1] == (byte) 'g' && t[i + 2] == (byte) 'g' && t[i + 3] == (byte) 'S') {
				byte[] byteArray = new byte[] { t[i + 6], t[i + 7], t[i + 8], t[i + 9], t[i + 10], t[i + 11], t[i + 12], t[i + 13] };
				ByteBuffer bb = ByteBuffer.wrap(byteArray);
				bb.order(ByteOrder.LITTLE_ENDIAN);
				length = bb.getInt(0);
			}
		}
		for (int i = 0; i < size - 14 && rate < 0; i++) {
			if (t[i] == (byte) 'v' && t[i + 1] == (byte) 'o' && t[i + 2] == (byte) 'r' && t[i + 3] == (byte) 'b' && t[i + 4] == (byte) 'i' && t[i + 5] == (byte) 's') {
				byte[] byteArray = new byte[] { t[i + 11], t[i + 12], t[i + 13], t[i + 14] };
				ByteBuffer bb = ByteBuffer.wrap(byteArray);
				bb.order(ByteOrder.LITTLE_ENDIAN);
				rate = bb.getInt(0);
			}
		}
		int res = length / rate;
		return res > Short.MAX_VALUE ? Short.MAX_VALUE : (short) res;
	}
	
}
