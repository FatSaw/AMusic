package me.bomb.amusic.resourceserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

final class CachedResource {
	
	protected final byte[] resource;
	
	protected CachedResource(byte[] resource) {
		this.resource = resource;
	}

	protected CachedResource(File fileresource, int buffersize) {
		long filesize = fileresource.length();
		if(filesize > buffersize) {
			filesize = buffersize;
		}
		byte[] resource = new byte[(int) filesize];
		try {
			FileInputStream streamresource = new FileInputStream(fileresource);
			int size = streamresource.read(resource);
			streamresource.close();
			if(size < filesize) {
				resource = Arrays.copyOf(resource, size);
			}
		} catch (IOException e) {
		}
		this.resource = resource;
	}

}
