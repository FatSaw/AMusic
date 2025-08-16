package me.bomb.amusic.bukkit.legacy;

import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.entity.Player;

import me.bomb.amusic.PackSender;

public final class LegacyPackSender_1_7_R4 implements PackSender {

	private final Server server;
	
	public LegacyPackSender_1_7_R4(Server server) {
		this.server = server;
	}
	
	@Override
	public void send(UUID uuid, String url, byte[] sha1) {
		if(uuid == null) {
			return;
		}
		Player player = server.getPlayer(uuid);
		player.setResourcePack(url);
	}
	
}
