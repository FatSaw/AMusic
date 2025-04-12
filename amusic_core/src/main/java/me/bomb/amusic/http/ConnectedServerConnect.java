package me.bomb.amusic.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

final class ConnectedServerConnect extends ServerConnect {
	
	private final static byte[] noaccess = "HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
	
	private final Collection<InetAddress> onlineips;

	protected ConnectedServerConnect(final ServerWatcher serverwatcher, final ServerWorker worker, final Collection<InetAddress> onlineips) {
		super(serverwatcher, worker);
		this.onlineips = onlineips;
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
					if (onlineips.contains(connected.getInetAddress())) {
						worker.processConnection(connected);
					} else {
						connected.getOutputStream().write(noaccess);
						connected.close();
					}
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
