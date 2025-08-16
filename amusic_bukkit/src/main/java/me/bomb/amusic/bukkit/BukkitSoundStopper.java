package me.bomb.amusic.bukkit;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import me.bomb.amusic.SoundStopper;

public final class BukkitSoundStopper implements SoundStopper {
	
	private final Server server;
	
	protected BukkitSoundStopper(Server server) {
		this.server = server;
	}

	@Override
	public void stopSound(UUID uuid, short id) {
		if(uuid == null) {
			return;
		}
		Player player = server.getPlayer(uuid);
		player.stopSound("amusic.music".concat(Short.toString(id)), SoundCategory.VOICE);
	}

}
