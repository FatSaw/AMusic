package me.bomb.amusic.glowstone;

import static me.bomb.amusic.util.HexUtils.fromBytesToHex;

import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.entity.Player;

import me.bomb.amusic.PackSender;

public final class GlowstonePackSender implements PackSender {
	
	private final Server server;
	
	protected GlowstonePackSender(Server server) {
		this.server = server;
	}

	@Override
	public void send(UUID uuid, String url, byte[] sha1) {
		if(uuid == null) {
			return;
		}
		Player player = server.getPlayer(uuid);
		player.setResourcePack(url, fromBytesToHex(sha1));
	}

}
