package me.bomb.amusic.http;

import java.io.IOException;
import java.net.SocketAddress;
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
				server.configureBlocking(true);
				server.socket().setSoTimeout(this.timeout);
				server.bind(this.address, backlog);
				this.server = server;
			} catch (IOException e1) {
				e1.printStackTrace();
				try {
					sleep(10000L);
				} catch (InterruptedException e2) {
				}
				continue;
			}
			
			try {
				sleep(100L);
			} catch (InterruptedException e) {
			}
			
			while (server.isOpen()) {
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
