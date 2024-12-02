package me.bomb.amusic.resourceserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

public final class ResourceServer extends Thread {
	
	private final static byte[] noaccess = "HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
	
	private final ConcurrentHashMap<Object,InetAddress> onlineips;
	private final InetAddress ip;
	private final int port, backlog;
	private boolean run = false;
	private ServerSocket server;
	private final ResourceManager resourcemanager;
	
	public ResourceServer(ConcurrentHashMap<Object,InetAddress> onlineips, InetAddress ip, int port, int backlog, ResourceManager resourcemanager) {
		this.onlineips = onlineips;
		this.ip = ip;
		this.port = port;
		this.backlog = backlog;
		this.resourcemanager = resourcemanager;
	}
	
	@Override
	public void start() {
		run = true;
		super.start();
	}

	public void run() {
		while (run) {
			try {
				server = new ServerSocket();
				server.bind(new InetSocketAddress(ip, port), backlog);
			} catch (IOException|SecurityException|IllegalArgumentException e) {
				e.printStackTrace();
				return;
			}
			while (!server.isClosed()) {
				try {
					if (this.onlineips == null) {
						new ResourceSender(server.accept(), resourcemanager);
					} else {
						Socket connected = server.accept();
						if (onlineips.values().contains(connected.getInetAddress())) {
							new ResourceSender(connected, resourcemanager);
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
		try {
			server.close();
		} catch (IOException e) {
		}
	}

}
