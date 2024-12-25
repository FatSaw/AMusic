package me.bomb.amusic.velocity;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;

import me.bomb.amusic.PackSender;

public final class VelocityPackSender implements PackSender {

	private final ProxyServer server;
	private final ChannelIdentifier identifier;
	
	protected VelocityPackSender(ProxyServer server) {
		this.server = server;
		this.identifier = new LegacyChannelIdentifier("MC|RPack");
	}
	
	@Override
	public void send(UUID uuid, String url, byte[] sha1) {
		Optional<Player> oplayer = server.getPlayer(uuid);
		if(oplayer.isEmpty()) {
			return;
		}
		Player player = oplayer.get();
		if (player.getProtocolVersion().getProtocol() < 6) {
			byte[] urlb = url.getBytes(StandardCharsets.UTF_8), buf = new byte[2 + urlb.length];
			short length = (short) urlb.length;
			System.arraycopy(urlb, 0, buf, 2, length);
			urlb[0] = (byte) length;
			length >>= 8;
			urlb[1] = (byte) length;
			player.sendPluginMessage(identifier, buf);
			return;
		}
		try {
			player.sendResourcePack(url, sha1);
		} catch (IllegalStateException e) {
		}
	}

}
