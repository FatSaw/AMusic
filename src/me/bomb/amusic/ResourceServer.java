package me.bomb.amusic;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

final class ResourceServer extends Thread {
	private final ConcurrentHashMap<Player,InetAddress> onlineips = new ConcurrentHashMap<Player,InetAddress>(16,0.75f,1);
	private boolean run = false;
	private ServerSocket server;
	
	protected ResourceServer(AMusic plugin) {
		if (ConfigOptions.strictdownloaderlist) {
			Bukkit.getPluginManager().registerEvents(new Listener() {
				@EventHandler
				public void playerJoin(PlayerJoinEvent event) {
					Player player = event.getPlayer();
					onlineips.put(player, player.getAddress().getAddress());
				}
				@EventHandler
				public void playerQuit(PlayerQuitEvent event) {
					Player player = event.getPlayer();
					onlineips.remove(player);
				}
			}, plugin);
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
			} catch (IOException|SecurityException|IllegalArgumentException e) {
				e.printStackTrace();
				return;
			}
			Socket connected = null;
			while (!server.isClosed()) {
				try {
					if (ConfigOptions.strictdownloaderlist) {
						connected = server.accept();
						if (onlineips.values().contains(connected.getInetAddress())) {
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
	}

}
