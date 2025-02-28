package me.bomb.amusic.glowstone;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.bomb.amusic.SoundStopper;

public class GlowstoneLegacySoundStopper implements SoundStopper {
	
	protected GlowstoneLegacySoundStopper() {
	}

	@Override
	public void stopSound(UUID uuid, short id) {
		if(uuid == null) {
			return;
		}
		Player player = Bukkit.getPlayer(uuid);
		player.stopSound("amusic.music".concat(Short.toString(id)));
	}
}
