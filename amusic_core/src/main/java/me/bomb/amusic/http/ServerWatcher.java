package me.bomb.amusic.http;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.StandardSocketOptions;
import java.nio.channels.ServerSocketChannel;

final class ServerWatcher extends Thread {

	private final SocketAddress address;
	private final int backlog;
	protected final int timeout;
	protected volatile boolean run = false;
	protected ServerSocketChannel server = null;
	private ServerConnect[] connects;

	protected ServerWatcher(final SocketAddress address, final int backlog, final int timeout, ServerConnect... connects) {
		this.address = address;
		this.backlog = backlog;
		this.timeout = timeout;
		this.connects = connects;
	}

	@Override
	public void start() {
		run = true;
		int i = connects.length;
		while(--i > -1) {
			try {
				connects[i].start();
			} catch(IllegalThreadStateException e) {
			}
		}
		super.start();
	}

	public void run() {
		while (run) {
			ServerSocketChannel server;
			try {
				server = ServerSocketChannel.open();
				this.server = server;
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			try {
				server.setOption(StandardSocketOptions.TCP_NODELAY, Boolean.TRUE);
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				server.bind(this.address, backlog);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			try {
				server.configureBlocking(true);
			} catch (IOException e) {
				e.printStackTrace();
			}
			ServerSocket serversocket = server.socket();
			try {
				serversocket.setSoTimeout(this.timeout);
			} catch (SocketException e) {
				e.printStackTrace();
			}
			
			try {
				sleep(100L);
			} catch (InterruptedException e) {
			}
			
			while (!serversocket.isClosed()) {
				int i = connects.length;
				while(--i > -1) {
					try {
						ServerConnect sc = connects[i];
						synchronized (sc) {
							sc.notify();
						}
					} catch(IllegalThreadStateException e) {
					}
				}
				try {
					sleep(100L);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	public void end() {
		run = false;
		if (server == null)
			return;
		try {
			server.close();
		} catch (IOException e) {
		}
	}
	
	public void restart() {
		if (server == null) return;
		try {
			server.close();
		} catch (IOException e) {
		}
		int i = connects.length;
		while(--i > -1) {
			try {
				ServerConnect sc = connects[i];
				synchronized (sc) {
					sc.notify();
				}
			} catch(IllegalThreadStateException e) {
			}
		}
	}

}
