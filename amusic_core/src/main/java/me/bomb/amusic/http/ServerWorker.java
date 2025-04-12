package me.bomb.amusic.http;

import java.io.IOException;
import java.net.Socket;

public interface ServerWorker {
	
	public abstract void processConnection(final Socket connected) throws IOException;

}
