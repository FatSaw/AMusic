package me.bomb.amusic.api;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public interface Resourcepack {
	
	/**
	 * Get resourcepack
	 * @return resourcepack ByteBuffer
	 */
	public abstract ByteBuffer getPack();
	
	/**
	 * Send resourcepack to channel
	 */
	public abstract void sendTo(SocketChannel channel) throws IOException;
	
	/**
	 * Get length of resourcepack
	 */
	public abstract int getLength();
}
