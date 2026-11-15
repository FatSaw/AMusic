package me.bomb.amusic.resourcepack.packer;

import java.nio.ByteBuffer;

import me.bomb.amusic.resourcepack.SourceEntry;
import me.bomb.amusic.resourcepack.storage.ResourcepackInfoImpl;

public final class PackedResourcepack implements SourceEntry {
	
	public final ByteBuffer resourcepack;
	public final ResourcepackInfoImpl info;
	
	public PackedResourcepack(ByteBuffer resourcepack, ResourcepackInfoImpl info) {
		this.resourcepack = resourcepack;
		this.info = info;
	}
}
