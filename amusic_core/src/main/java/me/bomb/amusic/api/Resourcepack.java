package me.bomb.amusic.api;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public interface Resourcepack {
	
	/**
	 * Get resourcepack
	 * @return resourcepack byte array null if signature invalid
	 */
	public abstract byte[] getPack();
	
	/**
	 * Send resourcepack to channel
	 */
	public abstract void sendTo(SocketChannel channel) throws IOException;
	
	/**
	 * Get length of resourcepack
	 */
	public abstract int getLength();
}
