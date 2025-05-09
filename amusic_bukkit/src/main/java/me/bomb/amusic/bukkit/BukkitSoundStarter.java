package me.bomb.amusic.bukkit;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import me.bomb.amusic.SoundStarter;

public final class BukkitSoundStarter implements SoundStarter {
	
	protected BukkitSoundStarter() {
	}

	@Override
	public void startSound(UUID uuid, short id) {
		if(uuid == null) {
			return;
		}
		Player player = Bukkit.getPlayer(uuid);
		player.playSound(player.getLocation(), "amusic.music".concat(Short.toString(id)), SoundCategory.VOICE, 1.0f, 1.0f);
	}

}
