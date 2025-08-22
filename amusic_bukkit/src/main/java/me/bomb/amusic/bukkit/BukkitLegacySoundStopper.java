package me.bomb.amusic.bukkit;

import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.entity.Player;

import me.bomb.amusic.SoundStopper;
import me.bomb.amusic.util.HexUtils;

public class BukkitLegacySoundStopper implements SoundStopper {
	
	private final Server server;
	
	protected BukkitLegacySoundStopper(Server server) {
		this.server = server;
	}

	@Override
	public void stopSound(UUID uuid, short id, byte partid) {
		if(uuid == null) {
			return;
		}
		String musicid = new StringBuilder("amusic.music").append(HexUtils.shortToHex(id)).append(HexUtils.byteToHex(partid)).toString();
		Player player = server.getPlayer(uuid);
		player.stopSound(musicid);
	}
}
