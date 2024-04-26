package me.bomb.amusic.bukkit;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.bomb.amusic.SoundStopper;

public final class BukkitSoundStopper implements SoundStopper {
	
	protected BukkitSoundStopper() {
	}

	@Override
	public void stopSound(UUID uuid, byte id) {
		if(uuid == null) {
			return;
		}
		Player player = Bukkit.getPlayer(uuid);
		player.stopSound("amusic.music".concat(Byte.toString(id)));
	}

}
