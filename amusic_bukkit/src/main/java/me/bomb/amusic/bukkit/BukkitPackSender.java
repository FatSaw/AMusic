package me.bomb.amusic.bukkit;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import me.bomb.amusic.PackSender;

public final class BukkitPackSender implements PackSender {
	
	private final Server server;
	
	protected BukkitPackSender(Server server) {
		this.server = server;
	}

	@Override
	public void send(UUID uuid, String url, byte[] sha1) {
		if(uuid == null) {
			return;
		}
		Player player = server.getPlayer(uuid);
		player.setResourcePack(url, sha1);
	}

}
