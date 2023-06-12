package me.bomb.amusic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

final class ResourceServer extends Thread {
	private static final byte[] responsepart0 = "HTTP/1.1 200 OK\r\nServer: AMusic server\r\nContent-Type: application/zip\r\nContent-Length: ".getBytes();
	private static final byte[] responsepart1 = "\r\nConnection: close\r\n\r\n".getBytes();
	private HashSet<InetAddress> downloaders = new HashSet<InetAddress>();
	private boolean end = false;
	private ServerSocket server;
	int ophc = 0;

	public ResourceServer(AMusic plugin) {
		if (ConfigOptions.strictdownloaderlist) {
			plugin.addTask(new BukkitRunnable() {
				@Override
				public void run() {
					Collection<? extends Player> onlineplayers = Bukkit.getOnlinePlayers();
					int aopch = onlineplayers.hashCode();
					if (ophc != aopch) {
						ophc = aopch;
						HashSet<InetAddress> adownloaders = new HashSet<InetAddress>();
						for (Player player : onlineplayers) {
							adownloaders.add(player.getAddress().getAddress());
						}
						downloaders = adownloaders;
					}
				}
			}.runTaskTimer(plugin, 20L, 20L));
		}
		start();
	}

	public void run() {
		while (true) {
			try {
				server = new ServerSocket(ConfigOptions.port);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			Socket connected = null;
			while (!server.isClosed()) {
				try {
					if (ConfigOptions.strictdownloaderlist) {
						synchronized (downloaders) {
							connected = server.accept();
							if (downloaders.contains(connected.getInetAddress())) {
								new ResourceSender(connected);
							} else {
								connected.close();
							}
						}
					} else {
						new ResourceSender(server.accept());
					}
				} catch (IOException e) {
				}
			}
			if (end) {
				break;
			}
			try {
				sleep(1000L);
			} catch (InterruptedException e) {
			}
		}
	}

	@SuppressWarnings("deprecation")
	public void close() {
		end = true;
		try {
			server.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		stop();
	}

	private class ResourceSender extends Thread {
		private final Socket client;

		private ResourceSender(Socket client) {
			this.client = client;
			start();
		}

		public void run() {
			UUID token = null;
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				StringTokenizer tokenizer = new StringTokenizer(in.readLine());
				String httpMethod = tokenizer.nextToken();
				String httpQueryString = tokenizer.nextToken();
				int uuidend;
				if (!httpMethod.equals("GET") || (uuidend = httpQueryString.lastIndexOf(".zip")) < 36) {
					client.close();
					return;
				}
				token = UUID.fromString(httpQueryString.substring(1, uuidend));
				byte i = 0;
				while (CachedResource.waitAcception(token) && 0 != ++i) {
					try {
						sleep(100);
					} catch (InterruptedException e) {
					}
				}
				byte[] ares;
				if (CachedResource.waitAcception(token) || (ares = CachedResource.get(token)).length == 0) {
					client.close();
					return;
				}
				OutputStream out = this.client.getOutputStream();
				out.write(responsepart0);
				out.write(Integer.toString(ares.length).getBytes());
				out.write(responsepart1);
				out.write(ares);
				client.close();
			} catch (IndexOutOfBoundsException | IllegalArgumentException | IOException e) {
			}

		}
	}

}
