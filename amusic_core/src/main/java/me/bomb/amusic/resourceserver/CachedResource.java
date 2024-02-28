package me.bomb.amusic.resourceserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

final class CachedResource {
	
	private final static MessageDigest md5hash;
	
	static {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
		}
		md5hash = md;
	}
	
	protected final byte[] hash;
	protected final byte[] resource;
	
	protected CachedResource(byte[] resource) {
		this.resource = resource;
		this.hash = md5hash.digest(resource);
	}

	protected CachedResource(File fileresource, int buffersize) {
		byte[] resource = new byte[buffersize];
		try {
			FileInputStream streamresource = new FileInputStream(fileresource);
			int size = streamresource.read(resource);
			streamresource.close();
			resource = Arrays.copyOf(resource, size);
		} catch (IOException e) {
		}
		this.resource = resource;
		this.hash = md5hash.digest(resource);
	}

}
