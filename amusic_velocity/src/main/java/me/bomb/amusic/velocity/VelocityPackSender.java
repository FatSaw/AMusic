package me.bomb.amusic.velocity;

import java.util.Optional;
import java.util.UUID;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import me.bomb.amusic.PackSender;

public final class VelocityPackSender implements PackSender {

	private final ProxyServer server;
	
	protected VelocityPackSender(ProxyServer server) {
		this.server = server;
	}
	
	@Override
	public void send(UUID uuid, String url, byte[] sha1) {
		Optional<Player> oplayer = server.getPlayer(uuid);
		if(oplayer.isEmpty()) {
			return;
		}
		Player player = oplayer.get();
		player.sendResourcePack(url, sha1);
	}

}
