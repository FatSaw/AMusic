package me.bomb.amusic.uploader;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

public final class SslUploaderServer extends Thread implements Uploader {

	private final static byte[] noaccess = "HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
	private final SSLServerSocketFactory sslserverfactory;
	private final UploadManager uploadmanager;
	private final ConcurrentHashMap<Object, InetAddress> onlineips;
	private final InetAddress ip;
	private final int port, backlog;
	private boolean run = false;
	private SSLServerSocket server;

	public SslUploaderServer( final UploadManager uploadmanager, final ConcurrentHashMap<Object, InetAddress> onlineips, final InetAddress ip, final int port, final int backlog, final SSLServerSocketFactory sslserverfactory) {
		this.sslserverfactory = sslserverfactory;
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
		this.uploadmanager.start();
	}

	public void run() {
		while (run) {
			try {
				server = (SSLServerSocket)sslserverfactory.createServerSocket(port, backlog, ip);
				//server.setNeedClientAuth(true); //
			} catch (IOException | SecurityException | IllegalArgumentException | NullPointerException e) {
				e.printStackTrace();
				return;
			}
			while (!server.isClosed()) {
				try {
					
					SSLSocket connected = (SSLSocket) server.accept();
					if (this.onlineips == null) {
						new SSLPageSender(uploadmanager, connected).start();
					} else {
						if (onlineips.values().contains(connected.getInetAddress())) {
							new SSLPageSender(uploadmanager, connected).start();
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
		this.uploadmanager.end();
	}

}
