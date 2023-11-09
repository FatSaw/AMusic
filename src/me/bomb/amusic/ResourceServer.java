package me.bomb.amusic;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

final class ResourceServer extends Thread {
	private HashSet<InetAddress> downloaders = new HashSet<InetAddress>();
	private boolean run = false;
	private ServerSocket server;
	private int ophc = 0;
	private final BukkitTask accesscontroler;
	
	protected ResourceServer(AMusic plugin) {
		if (ConfigOptions.strictdownloaderlist) {
			accesscontroler = new BukkitRunnable() {
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
			}.runTaskTimer(plugin, 20L, 20L);
		} else {
			accesscontroler = null;
		}
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
				server = new ServerSocket(ConfigOptions.port);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			Socket connected = null;
			while (!server.isClosed()) {
				try {
					if (ConfigOptions.strictdownloaderlist) {
						connected = server.accept();
						boolean canaccess = false;
						synchronized (downloaders) {
							canaccess = downloaders.contains(connected.getInetAddress());
						}
						if (canaccess) {
							new ResourceSender(connected);
						} else {
							connected.close();
						}
					} else {
						new ResourceSender(server.accept());
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
		accesscontroler.cancel();
	}

}
