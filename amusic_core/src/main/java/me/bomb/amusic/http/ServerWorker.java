package me.bomb.amusic.http;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public interface ServerWorker {
	
	public abstract void processConnection(final SocketChannel connected) throws IOException;

	public void start();
	
	public void end();
	
}
