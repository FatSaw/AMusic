package me.bomb.amusic.packedinfo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;

public final class DataEntry {
	
	public int size;
	private final FileSystemProvider fs;
	public final Path datapath;
	public final String name;
	public SoundInfo[] sounds;
	public byte[] sha1;
	protected boolean saved;

	protected DataEntry(Path datapath, int size, String name, SoundInfo[] sounds, byte[] sha1) {
		this.fs = datapath.getFileSystem().provider();
		this.datapath = datapath;
		this.size = size;
		this.name = name;
		this.sounds = sounds;
		this.sha1 = sha1;
	}
	
	public byte[] getPack() {
		InputStream is = null;
		byte[] buf = new byte[size];
		try {
			is = fs.newInputStream(datapath);
			is.read(buf, 0, buf.length);
		} catch (IOException e) {
		} finally {
			if(is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}
		return buf;
	}
	
}