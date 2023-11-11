package me.bomb.amusic;

import org.bukkit.craftbukkit.v1_10_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import net.minecraft.server.v1_10_R1.PacketPlayOutResourcePackSend;

final class LegacyPackSender_1_10_R1 extends LegacyPackSender {
	
	protected LegacyPackSender_1_10_R1() {
	}

	@Override
	protected void sendPack(Player player, String url, String sha1) {
		((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutResourcePackSend(url, sha1));
	}

}
