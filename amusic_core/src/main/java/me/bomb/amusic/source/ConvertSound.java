package me.bomb.amusic.source;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import me.bomb.amusic.util.ByteArraysOutputStream;

public class ConvertSound extends ReadSound {
	protected final Runtime runtime;
	protected final String[] args;

	public ConvertSound(Runtime runtime, Path fmpegbinary, int bitrate, byte channels, int samplingrate, int maxsoundsize, byte[] splits, int[] sizes, Path[] files, UUID[] soundhashs, byte[][][] data, short[] lengths, AtomicBoolean[] finished, boolean[] success, int start, int end) {
		super(maxsoundsize, splits, sizes, files, soundhashs, data, lengths, finished, success, start, end);
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
			
			byte lastsplitbitid = 8;
			while(--lastsplitbitid > 0) {
				if((split >>> lastsplitbitid & 0x01) == 0x01) {
					break;
				}
			}
			++lastsplitbitid;
			
			byte[][] parts = new byte[split][];
			int partid = -1;
			if((split & 0x01) == 0x01) {
				parts[++partid] = buf;
			}
			
			byte[][] allparts = new byte[1][];
			allparts[0] = buf;

			lengths[i] = calculateDuration(buf);
			buf = md5hash.digest(buf);
			long lsb = 0L, msb = 0L;
			lsb = buf[0x0F] & 0xFF;
			lsb<<=8;
			lsb |= buf[0x0E] & 0xFF;
			lsb<<=8;
			lsb |= buf[0x0D] & 0xFF;
			lsb<<=8;
			lsb |= buf[0x0C] & 0xFF;
			lsb<<=8;
			lsb |= buf[0x0B] & 0xFF;
			lsb<<=8;
			lsb |= buf[0x0A] & 0xFF;
			lsb<<=8;
			lsb |= buf[0x09] & 0xFF;
			lsb<<=8;
			lsb |= buf[0x08] & 0xFF;
			msb = buf[0x07] & 0xFF;
			msb<<=8;
			msb |= buf[0x06] & 0xFF;
			msb<<=8;
			msb |= buf[0x05] & 0xFF;
			msb<<=8;
			msb |= buf[0x04] & 0xFF;
			msb<<=8;
			msb |= buf[0x03] & 0xFF;
			msb<<=8;
			msb |= buf[0x02] & 0xFF;
			msb<<=8;
			msb |= buf[0x01] & 0xFF;
			msb<<=8;
			msb |= buf[0x00] & 0xFF;
			soundhashs[i] = new UUID(msb, lsb);
			
			byte splitbitid = 0;
			while(++splitbitid < lastsplitbitid) {
				int newallpartid = -1;
				byte[][] newallpart = new byte[1 << splitbitid][];
				int allpartid = -1;
				while(++allpartid < allparts.length) {
					OggVorbisSplitter splitter = new OggVorbisSplitter(allparts[allpartid]);
					splitter.run();
					newallpart[++newallpartid] = splitter.part1;
					newallpart[++newallpartid] = splitter.part2;
					if((split >>> splitbitid & 0x01) == 0x01) {
						parts[++partid] = splitter.part1;
						parts[++partid] = splitter.part2;
					}
				}
				allparts = newallpart;
				newallpartid = -1;
			}
			data[i] = parts;
			success[i] = true;
			
			if(finished == null) {
				continue;
			}
			finished[i].set(true);
		}
	}

}
