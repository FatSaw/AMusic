package me.bomb.amusic.bukkit;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.bomb.amusic.SoundStarter;

public final class BukkitSoundStarter implements SoundStarter {
	
	protected BukkitSoundStarter() {
		
	}

	@Override
	public void startSound(UUID uuid, String soundname) {
		if(uuid == null) {
			return;
		}
		Player player = Bukkit.getPlayer(uuid);
		player.playSound(player.getLocation(), soundname, 1.0E9f, 1.0f);
	}

}
