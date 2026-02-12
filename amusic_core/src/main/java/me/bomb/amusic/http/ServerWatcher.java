package me.bomb.amusic.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import javax.net.ServerSocketFactory;

final class ServerWatcher extends Thread {

	private final ServerSocketFactory serverfactory;
	private final InetAddress ip;
	private final int port, backlog;
	protected final int timeout;
	protected volatile boolean run = false;
	protected ServerSocket server = null;
	private ServerConnect[] connects;

	protected ServerWatcher(final InetAddress ip, final int port, final int backlog, final int timeout, final ServerSocketFactory serverfactory, ServerConnect... connects) {
		this.serverfactory = serverfactory;
		this.ip = ip;
		this.port = port;
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
			try {
				server = serverfactory.createServerSocket(port, backlog, ip);
				server.setSoTimeout(timeout);
			} catch (IOException | SecurityException | IllegalArgumentException | NullPointerException e) {
				e.printStackTrace();
				return;
			}
			
			try {
				sleep(1000L);
			} catch (InterruptedException e) {
			}
			
			
			
			while (!server.isClosed()) {
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
					sleep(1000L);
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
