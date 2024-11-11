package me.bomb.amusic.yt.legacy;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import me.bomb.amusic.PackSender;
import net.minecraft.server.v1_10_R1.PacketPlayOutResourcePackSend;

public final class LegacyPackSender_1_10_R1 implements PackSender {
	
	public LegacyPackSender_1_10_R1() {
	}

	@Override
	public void send(UUID uuid, String url, byte[] sha1) {
		if(uuid == null) {
			return;
		}
		Player player = Bukkit.getPlayer(uuid);
		StringBuilder sb = new StringBuilder();
		for (byte b : sha1) {
			int value = b & 0xFF;
			if (value < 16) {
				sb.append("0");
			}
			sb.append(Integer.toHexString(value).toLowerCase());
		}
		((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutResourcePackSend(url, sb.toString()));
	}

}
