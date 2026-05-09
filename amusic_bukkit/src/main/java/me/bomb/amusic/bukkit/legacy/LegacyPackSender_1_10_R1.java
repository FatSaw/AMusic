package me.bomb.amusic.bukkit.legacy;

import static me.bomb.amusic.util.HexUtils.fromBytesToHex;

import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import me.bomb.amusic.PackSender;
import net.minecraft.server.v1_10_R1.PacketPlayOutResourcePackSend;

public final class LegacyPackSender_1_10_R1 implements PackSender {
	
	private final Server server;
	
	public LegacyPackSender_1_10_R1(Server server) {
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
