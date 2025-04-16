package me.bomb.amusic.source;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;

public final class StaticPackSource extends PackSource {
	
	private final FileSystemProvider fsp;
	private final Path pack;
	private final int maxpacksize;
	
	public StaticPackSource(Path pack, int maxpacksize) {
		this.fsp = pack.getFileSystem().provider();
		this.pack = pack;
		this.maxpacksize = maxpacksize;
	}

	@Override
	public byte[] get(String id) {
		final Path parentpack = pack;
		final int size;
		try {
			BasicFileAttributes attributes = fsp.readAttributes(parentpack, BasicFileAttributes.class);
			final long sizel = attributes.size();
			if(attributes.isDirectory() || sizel > maxpacksize) {
				return null;
			}
			size = (int) sizel;
		} catch (IOException e) {
			return null;
		}
		final byte[] packbuf = new byte[size];
		InputStream is = null;
		boolean ok = false;
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
		return ok ? packbuf : null;
	}

}
