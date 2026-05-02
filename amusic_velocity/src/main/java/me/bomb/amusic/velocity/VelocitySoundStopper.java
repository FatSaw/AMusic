package me.bomb.amusic.velocity;

import java.util.Optional;
import java.util.UUID;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import me.bomb.amusic.SoundStopper;
import me.bomb.amusic.util.HexUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;

public final class VelocitySoundStopper implements SoundStopper {

	private final ProxyServer server;
	
	protected VelocitySoundStopper(ProxyServer server) {
		this.server = server;
	}
	
	@Override
	public void stopSound(UUID uuid, short id, byte partid) {
		Optional<Player> oplayer = server.getPlayer(uuid);
		if(oplayer.isEmpty()) {
			return;
		}
		String musicid = new StringBuilder("amusic.music").append(HexUtils.shortToHex(id)).append(HexUtils.byteToHex(partid)).toString();
		Player player = oplayer.get();
		
		SoundStop sound = SoundStop.namedOnSource(Key.key(musicid), Sound.Source.VOICE);
		player.stopSound(sound);
	}

}
