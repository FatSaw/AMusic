package me.bomb.amusic;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

abstract class LegacyPackSender {
	
	private static final LegacyPackSender packsender;
	
	static {
		String nmsversion = Bukkit.getServer().getClass().getPackage().getName().substring(23);
		switch (nmsversion) {
		case "v1_8_R3":
			packsender = new LegacyPackSender_1_8_R3();
		break;
		case "v1_9_R2":
			packsender = new LegacyPackSender_1_9_R2();
		break;
		case "v1_10_R1":
			packsender = new LegacyPackSender_1_10_R1();
		break;
		default:
			packsender = null;
		}
	}
	
	protected static final void sendResourcePack(Player player,String url,byte[] sha1) {
		StringBuilder sb = new StringBuilder();
		for (byte b : sha1) {
			int value = b & 0xFF;
			if (value < 16) {
				sb.append("0");
			}
			sb.append(Integer.toHexString(value).toLowerCase());
		}
		packsender.sendPack(player, url, sb.toString());
	}
	
	protected abstract void sendPack(Player player,String url,String sha1);
	
}
