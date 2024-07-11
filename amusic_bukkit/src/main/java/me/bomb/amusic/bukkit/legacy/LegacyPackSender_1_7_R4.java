package me.bomb.amusic.bukkit.legacy;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.bomb.amusic.PackSender;

public final class LegacyPackSender_1_7_R4 implements PackSender {

	@Override
	public void send(UUID uuid, String url, byte[] sha1) {
		if(uuid == null) {
			return;
		}
		Player player = Bukkit.getPlayer(uuid);
		player.setResourcePack(url);
	}
	
}
