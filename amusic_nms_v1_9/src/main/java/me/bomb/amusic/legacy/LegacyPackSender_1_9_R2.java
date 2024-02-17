package me.bomb.amusic.legacy;

import org.bukkit.craftbukkit.v1_9_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

import net.minecraft.server.v1_9_R2.PacketPlayOutResourcePackSend;

final class LegacyPackSender_1_9_R2 extends LegacyPackSender {
	
	protected LegacyPackSender_1_9_R2() {
	}

	@Override
	protected void sendPack(Player player, String url, String sha1) {
		((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutResourcePackSend(url, sha1));
	}

}
