package me.bomb.amusic.source;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;

public final class MusicdirPackSource extends PackSource {
	
	private final FileSystemProvider fsp;
	private final Path musicdir;
	private final int maxpacksize;
	
	public MusicdirPackSource(Path musicdir, int maxpacksize) {
		this.fsp = musicdir.getFileSystem().provider();
		this.musicdir = musicdir;
		this.maxpacksize = maxpacksize;
	}

	@Override
	public byte[] get(String id) {
		if(id == null) {
			return null;
		}
		final Path parentpack = musicdir.resolve(id.concat(".zip"));
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
