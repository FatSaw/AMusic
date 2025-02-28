package me.bomb.amusic.packedinfo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
	
	/**
	 * Get resourcepack
	 * @return resourcepack byte array null if signature invalid
	 */
	public byte[] getPack() {
		final MessageDigest sha1hash;
		try {
			sha1hash = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
		InputStream is = null;
		byte[] buf = new byte[size];
		try {
			is = fs.newInputStream(datapath);
			is.read(buf, 0, buf.length);
			is.close();
		} catch (IOException e1) {
			if(is != null) {
				try {
					is.close();
				} catch (IOException e2) {
				}
			}
		}
		byte i = 20;
		byte[] filesha1 = sha1hash.digest(buf);
		while(--i > -1) {
			if(filesha1[i] != sha1[i]) {
				return null;
			}
		}
		
		return buf;
	}
	
}