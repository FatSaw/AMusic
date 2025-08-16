package me.bomb.amusic.glowstone;

import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.entity.Player;

import me.bomb.amusic.SoundStopper;

public class GlowstoneLegacySoundStopper implements SoundStopper {
	
	private final Server server;
	
	protected GlowstoneLegacySoundStopper(Server server) {
		this.server = server;
	}

	@Override
	public void stopSound(UUID uuid, short id) {
		if(uuid == null) {
			return;
		}
		Player player = server.getPlayer(uuid);
		player.stopSound("amusic.music".concat(Short.toString(id)));
	}
}
