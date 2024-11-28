package me.bomb.amusic.uploader;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;

public final class UploaderServer extends Thread {
	private final UploadManager uploadmanager;
	//private final ConcurrentHashMap<Object, InetAddress> onlineips;
	private final InetAddress ip;
	private final int port, backlog;
	private boolean run = false;
	private ServerSocket server;

	public UploaderServer(UploadManager uploadmanager, ConcurrentHashMap<Object, InetAddress> onlineips, InetAddress ip, int port, int backlog) {
		this.uploadmanager = uploadmanager;
		//this.onlineips = onlineips;
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
				server = new ServerSocket();
				server.bind(new InetSocketAddress(ip, port), backlog);
			} catch (IOException | SecurityException | IllegalArgumentException e) {
				e.printStackTrace();
				return;
			}
			while (!server.isClosed()) {
				try {
					new PageSender(uploadmanager, server.accept()).start();
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
		this.uploadmanager.end();
	}

}
