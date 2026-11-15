package me.bomb.amusic.bukkit;

import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.entity.Player;

import me.bomb.amusic.api.SoundStopper;

public class BukkitLegacySoundStopper implements SoundStopper {
	
	private final Server server;
	
	protected BukkitLegacySoundStopper(Server server) {
		this.server = server;
	}

	@Override
	public void stopSound(UUID uuid, UUID soundhash) {
		if(uuid == null) {
			return;
		}
		String musicid = AMUSIC_SOUNDID_PREFIX.concat(soundhash.toString());
		Player player = server.getPlayer(uuid);
		player.stopSound(musicid);
	}
}
