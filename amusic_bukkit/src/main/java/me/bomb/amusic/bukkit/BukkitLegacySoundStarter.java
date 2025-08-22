package me.bomb.amusic.bukkit;

import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.entity.Player;

import me.bomb.amusic.SoundStarter;
import me.bomb.amusic.util.HexUtils;

public final class BukkitLegacySoundStarter implements SoundStarter {
	
	private final Server server;
	
	protected BukkitLegacySoundStarter(Server server) {
		this.server = server;
	}

	@Override
	public void startSound(UUID uuid, short id, byte partid) {
		if(uuid == null) {
			return;
		}
		String musicid = new StringBuilder("amusic.music").append(HexUtils.shortToHex(id)).append(HexUtils.byteToHex(partid)).toString();
		Player player = server.getPlayer(uuid);
		player.playSound(player.getLocation(), musicid, 1.0E9f, 1.0f);
	}

}
