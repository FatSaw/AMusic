package me.bomb.amusic.source;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;

public final class CachedStaticPackSource extends PackSource {
	
	private final byte[] packbuf;
	
	public CachedStaticPackSource(Path pack, int maxpacksize) {
		FileSystemProvider fsp = pack.getFileSystem().provider();
		final Path parentpack = pack;
		int size = -1;
		byte[] packbuf = null;
		try {
			BasicFileAttributes attributes = fsp.readAttributes(parentpack, BasicFileAttributes.class);
			final long sizel = attributes.size();
			if(!attributes.isDirectory() && sizel <= maxpacksize) {
				size = (int) sizel;
			}
		} catch (IOException e) {
		}
		boolean ok = false;
		if(size > 0) {
			packbuf = new byte[size];
			InputStream is = null;
			try {
				is = fsp.newInputStream(parentpack);
				is.read(packbuf, 0, size);
				ok = true;
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
		
		this.packbuf = ok ? packbuf : null;
	}

	@Override
	public byte[] get(String id) {
		return this.packbuf;
	}

}
