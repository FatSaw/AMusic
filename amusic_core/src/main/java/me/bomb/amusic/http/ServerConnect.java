package me.bomb.amusic.http;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

class ServerConnect extends Thread {
	
	protected final ServerWatcher serverwatcher;
	protected final ServerWorker worker;
	
	protected ServerConnect(final ServerWatcher serverwatcher, final ServerWorker worker) {
		this.serverwatcher = serverwatcher;
		this.worker = worker;
	}
	
	@Override
	public void run() {
		while (serverwatcher.run) {
			final ServerSocketChannel server = serverwatcher.server;
			synchronized (this) {
				try {
					this.wait();
				} catch (InterruptedException e) {
				}
			}
			if(server == null || !server.isOpen()) {
				continue;
			}
			while(true) {
				SocketChannel connected = null;
				try {
					connected = server.accept();
					connected.socket().setSoTimeout(serverwatcher.timeout);
					worker.processConnection(connected);
				} catch (SocketTimeoutException e1) {
					if(connected != null) {
						try {
							connected.close();
						} catch (IOException e2) {
						}
					}
					continue;
				} catch (SocketException e1) {
					break;
				} catch (IOException e1) {
					break;
				}
			}
		}
	}

}
