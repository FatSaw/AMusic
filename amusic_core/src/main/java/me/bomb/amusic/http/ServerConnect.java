package me.bomb.amusic.http;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

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
			final ServerSocket server = serverwatcher.server;
			synchronized (this) {
				try {
					this.wait();
				} catch (InterruptedException e) {
				}
			}
			if(server == null || server.isClosed()) {
				continue;
			}
			while(true) {
				final Socket connected;
				try {
					connected = server.accept();
					connected.setSoTimeout(serverwatcher.timeout);
					worker.processConnection(connected);
				} catch (SocketTimeoutException e) {
					continue;
				} catch (SocketException e) {
					break;
				} catch (IOException e1) {
					break;
				}
			}
		}
	}

}
