package me.bomb.amusic.resourcepack;

import java.io.IOException;
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
	 * @return resourcepack byte array null if signature invalid
	 */
	@Override
	public abstract byte[] getPack();
	
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