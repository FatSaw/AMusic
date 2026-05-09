package me.bomb.amusic.bukkit.legacy;

import static me.bomb.amusic.util.HexUtils.fromBytesToHex;

import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import me.bomb.amusic.PackSender;
import net.minecraft.server.v1_8_R3.PacketPlayOutResourcePackSend;

public final class LegacyPackSender_1_8_R3 implements PackSender {
	
	private final Server server;
	
	public LegacyPackSender_1_8_R3(Server server) {
		this.server = server;
	}

	@Override
	public void send(UUID uuid, String url, byte[] sha1) {
		if(uuid == null) {
			return;
		}
		Player player = server.getPlayer(uuid);
		((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutResourcePackSend(url, fromBytesToHex(sha1)));
	}

}
