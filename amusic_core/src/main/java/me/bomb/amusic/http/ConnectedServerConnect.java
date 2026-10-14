package me.bomb.amusic.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

final class ConnectedServerConnect extends ServerConnect {
	
	private final static ByteBuffer noaccess = ByteBuffer.wrap("HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
	
	private final Collection<InetAddress> onlineips;

	protected ConnectedServerConnect(final ServerWatcher serverwatcher, final ServerWorker worker, final Collection<InetAddress> onlineips) {
		super(serverwatcher, worker);
		this.onlineips = onlineips;
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
					InetAddress address = ((InetSocketAddress)connected.getLocalAddress()).getAddress();
					if (onlineips.contains(address)) {
						connected.socket().setSoTimeout(serverwatcher.timeout);
						worker.processConnection(connected);
					} else {
						connected.write(noaccess);
						connected.close();
					}
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
