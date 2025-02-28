package me.bomb.amusic.bukkit;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.bomb.amusic.SoundStopper;

public class BukkitLegacySoundStopper implements SoundStopper {
	
	protected BukkitLegacySoundStopper() {
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
