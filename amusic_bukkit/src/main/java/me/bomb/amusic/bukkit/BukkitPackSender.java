package me.bomb.amusic.bukkit;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.bomb.amusic.PackSender;

public final class BukkitPackSender implements PackSender {

	@Override
	public void send(UUID uuid, String url, byte[] sha1) {
		if(uuid == null) {
			return;
		}
		Player player = Bukkit.getPlayer(uuid);
		player.setResourcePack(url, sha1);
	}

}
