package me.bomb.amusic.bukkit;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import me.bomb.amusic.SoundStarter;

public final class BukkitSoundStarter implements SoundStarter {
	
	private final Server server;
	
	protected BukkitSoundStarter(Server server) {
		this.server = server;
	}

	@Override
	public void startSound(UUID uuid, short id) {
		if(uuid == null) {
			return;
		}
		Player player = server.getPlayer(uuid);
		player.playSound(player.getLocation(), "amusic.music".concat(Short.toString(id)), SoundCategory.VOICE, 1.0f, 1.0f);
	}

}
