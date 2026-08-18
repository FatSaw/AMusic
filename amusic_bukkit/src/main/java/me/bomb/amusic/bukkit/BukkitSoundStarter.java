package me.bomb.amusic.bukkit;

import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import me.bomb.amusic.SoundStarter;
import me.bomb.amusic.util.HexUtils;

public final class BukkitSoundStarter implements SoundStarter {
	
	private final Server server;
	
	protected BukkitSoundStarter(Server server) {
		this.server = server;
	}

	@Override
	public void startSound(UUID uuid, UUID soundhash, short id, byte part) {
		if(uuid == null || soundhash == null) {
			return;
		}
		String musicid = new StringBuilder("minecraft:amusic.internal.").append(soundhash.toString()).append(HexUtils.shortToHex(id)).append(HexUtils.byteToHex(part)).toString();
		Player player = server.getPlayer(uuid);
		player.playSound(player.getLocation(), musicid, SoundCategory.VOICE, 1.0f, 1.0f);
	}

}
