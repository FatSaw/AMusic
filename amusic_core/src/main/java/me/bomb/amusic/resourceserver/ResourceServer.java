package me.bomb.amusic.resourceserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public final class ResourceServer extends Thread {
	private final ConcurrentHashMap<Object,InetAddress> onlineips;
	private final int port;
	private boolean run = false;
	private ServerSocket server;
	private final ResourceManager resourcemanager;
	
	public ResourceServer(ConcurrentHashMap<Object,InetAddress> onlineips, int port, ResourceManager resourcemanager) {
		this.onlineips = onlineips;
		this.port = port;
		this.resourcemanager = resourcemanager;
		start();
	}
	
	@Override
	public void start() {
		run = true;
		super.start();
	}

	public void run() {
		while (run) {
			try {
				server = new ServerSocket(port);
			} catch (IOException|SecurityException|IllegalArgumentException e) {
				e.printStackTrace();
				return;
			}
			Socket connected = null;
			while (!server.isClosed()) {
				try {
					if (this.onlineips == null) {
						new ResourceSender(server.accept(), resourcemanager);
					} else {
						connected = server.accept();
						if (onlineips.values().contains(connected.getInetAddress())) {
							new ResourceSender(connected, resourcemanager);
						} else {
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
