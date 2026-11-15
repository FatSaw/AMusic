package me.bomb.amusic.bukkit;

import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import me.bomb.amusic.api.SoundStarter;

public final class BukkitSoundStarter implements SoundStarter {
	
	private final Server server;
	
	protected BukkitSoundStarter(Server server) {
		this.server = server;
	}

	@Override
	public void startSound(UUID uuid, UUID soundhash) {
		if(uuid == null || soundhash == null) {
			return;
		}
		String musicid = AMUSIC_SOUNDID_PREFIX.concat(soundhash.toString());
		Player player = server.getPlayer(uuid);
		player.playSound(player.getLocation(), musicid, SoundCategory.VOICE, 1.0f, 1.0f);
	}

}
