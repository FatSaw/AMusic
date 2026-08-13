package me.bomb.amusic.packedinfo;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;

public final class PackMergeEntryFile implements PackMergeEntry {
	
	private final byte[] mergepack;
	private final int entrycount, cdsize, cdoffset, commentlength;
	
	
	public PackMergeEntryFile(Path mergepack, int maxresourcepacksize) {
		byte[] mergepackb = null;
		int mregeentriescount = 0, mergecdsize = 0, mergecdoffset = 0, mergecommentlength = 0;
		if(mergepack != null) {
			try {
				FileSystemProvider fsp = mergepack.getFileSystem().provider();
				BasicFileAttributes attributes = fsp.readAttributes(mergepack, BasicFileAttributes.class);
				final long size = attributes.size();
				if(attributes.isRegularFile() && size <= maxresourcepacksize) {
					byte[] buf = new byte[(int) size];
					InputStream is = null;
					try {
						is = fsp.newInputStream(mergepack);
						{
							int off = 0;
							int remaining = buf.length;
							while (remaining > 0) {
								int read = is.read(buf, off, remaining);
								if (read < 0) throw new EOFException();
								off += read;
								remaining -= read;
							}
						}
					} catch (IOException e) {
						throw new IllegalStateException(e);
					} finally {
						if(is != null) {
							try {
								is.close();
							} catch (IOException e2) {
							}
						}
					}
					int i = buf.length;
					if(i > 21) {
						int end = buf.length - 65558;
						if(end < -1) end = -1;
						i -= 21;
						while (--i > end) {
							int commentlength; //THIS WILL BE USED LATER
							if(buf[i] == 0x50 && buf[1+i] == 0x4B && buf[2+i] == 0x05 && buf[3+i] == 0x06 && buf.length == (commentlength = ((buf[21+i] & 0xFF) << 8) | (buf[20+i] & 0xFF)) + i + 22) {
								mregeentriescount = ((buf[i + 11] & 0xFF) << 8) | (buf[i + 10] & 0xFF);
								mergecdsize = ((buf[i + 15] & 0xFF) << 24) | ((buf[i + 14] & 0xFF) << 16) | ((buf[i + 13] & 0xFF) << 8) | (buf[i + 12] & 0xFF);
								mergecdoffset = ((buf[i + 19] & 0xFF) << 24) | ((buf[i + 18] & 0xFF) << 16) | ((buf[i + 17] & 0xFF) << 8) | (buf[i + 16] & 0xFF);
								mergecommentlength = commentlength;
								mergepackb = buf;
								break;
							}
						}
					}
				}
			} catch (IOException e) {
			}
		}
		this.mergepack = mergepackb;
		this.entrycount = mregeentriescount;
		this.cdsize = mergecdsize;
		this.cdoffset = mergecdoffset;
		this.commentlength = mergecommentlength;
	}

	@Override
	public byte[] getMergePack() {
		return this.mergepack;
	}

	@Override
	public int entryCount() {
		return this.entrycount;
	}

	@Override
	public int cdSize() {
		return this.cdsize;
	}

	@Override
	public int cdOffset() {
		return this.cdoffset;
	}

	@Override
	public int commentLength() {
		return this.commentlength;
	}

}
