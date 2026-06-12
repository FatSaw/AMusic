package me.bomb.amusic.bukkit;

import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import me.bomb.amusic.SoundStopper;
import me.bomb.amusic.util.HexUtils;

public final class BukkitSoundStopper implements SoundStopper {
	
	private final Server server;
	
	protected BukkitSoundStopper(Server server) {
		this.server = server;
	}

	@Override
	public void stopSound(UUID uuid, UUID soundhash, short id, byte partid) {
		if(uuid == null) {
			return;
		}
		String musicid = new StringBuilder("amusic:internal.").append(soundhash.toString()).append(HexUtils.shortToHex(id)).append(HexUtils.byteToHex(partid)).toString();
		Player player = server.getPlayer(uuid);
		player.stopSound(musicid, SoundCategory.VOICE);
	}

}
