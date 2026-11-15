package me.bomb.amusic.resourcepack.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import me.bomb.amusic.api.Resourcepack;

public abstract class DataEntry implements Resourcepack {
	
	public final String storeid;
	public final ResourcepackInfoImpl info;

	protected DataEntry(String storeid, ResourcepackInfoImpl info) {
		this.storeid = storeid;
		this.info = info;
	}
	
	/**
	 * Get resourcepack
	 * @return resourcepack ByteBuffer
	 */
	@Override
	public abstract ByteBuffer getPack();
	
	/**
	 * Send resourcepack to channel
	 */
	@Override
	public abstract void sendTo(SocketChannel channel) throws IOException;
	
	/**
	 * Get length of resourcepack
	 */
	@Override
	public abstract int getLength();
	
}