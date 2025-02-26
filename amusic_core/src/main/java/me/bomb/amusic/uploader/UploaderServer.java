package me.bomb.amusic.uploader;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ServerSocketFactory;

final class UploaderServer extends Thread {

	private final static byte[] noaccess = "HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
	private final ServerSocketFactory serverfactory;
	private final UploadManager uploadmanager;
	private final ConcurrentHashMap<Object, InetAddress> onlineips;
	private final InetAddress ip;
	private final int port, backlog;
	private volatile boolean run = false;
	private ServerSocket server;

	protected UploaderServer(final UploadManager uploadmanager, final ConcurrentHashMap<Object, InetAddress> onlineips, final InetAddress ip, final int port, final int backlog, final ServerSocketFactory serverfactory) {
		this.serverfactory = serverfactory;
		this.uploadmanager = uploadmanager;
		this.onlineips = onlineips;
		this.ip = ip;
		this.port = port;
		this.backlog = backlog;
	}

	@Override
	public void start() {
		run = true;
		super.start();
	}

	public void run() {
		while (run) {
			try {
				server = serverfactory.createServerSocket(port, backlog, ip);
			} catch (IOException | SecurityException | IllegalArgumentException | NullPointerException e) {
				e.printStackTrace();
				return;
			}
			while (!server.isClosed()) {
				try {
					Socket connected = server.accept();
					if (this.onlineips == null) {
						new PageSender(uploadmanager, connected).start();
					} else {
						if (onlineips.values().contains(connected.getInetAddress())) {
							new PageSender(uploadmanager, connected).start();
						} else {
							connected.getOutputStream().write(noaccess);
							connected.close();
						}
					}
					
				} catch (IOException e) {
				}
			}
			try {
				sleep(1000L);
			} catch (InterruptedException e) {
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

}
