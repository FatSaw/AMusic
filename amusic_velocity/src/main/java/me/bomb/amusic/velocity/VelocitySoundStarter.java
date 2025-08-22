package me.bomb.amusic.velocity;

import java.util.Optional;
import java.util.UUID;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import me.bomb.amusic.SoundStarter;
import me.bomb.amusic.util.HexUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

public final class VelocitySoundStarter implements SoundStarter {
	
	private final ProxyServer server;

	protected VelocitySoundStarter(ProxyServer server) {
		this.server = server;
	}
	
	@Override
	public void startSound(UUID uuid, short id, byte partid) {
		Optional<Player> oplayer = server.getPlayer(uuid);
		if(oplayer.isEmpty()) {
			return;
		}
		Player player = oplayer.get();
		final int version = player.getProtocolVersion().getProtocol();
		String musicid = new StringBuilder("amusic.music").append(HexUtils.shortToHex(id)).append(HexUtils.byteToHex(partid)).toString();
		Sound sound = Sound.sound(Key.key(musicid), Sound.Source.VOICE, version < 393 ? 1.0E9f : 1.0f, 1.0f);
		player.playSound(sound); //Velocity playSound not implemented
		
	}

}
