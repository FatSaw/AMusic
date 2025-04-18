package me.bomb.amusic.velocity;

import java.util.Optional;
import java.util.UUID;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import me.bomb.amusic.SoundStopper;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

public final class VelocitySoundStopper implements SoundStopper {

	private final ProxyServer server;
	
	protected VelocitySoundStopper(ProxyServer server) {
		this.server = server;
	}
	
	@Override
	public void stopSound(UUID uuid, short id) {
		Optional<Player> oplayer = server.getPlayer(uuid);
		if(oplayer.isEmpty()) {
			return;
		}
		Player player = oplayer.get();
		final int version = player.getProtocolVersion().getProtocol();
		Sound sound = Sound.sound(Key.key("amusic.music".concat(Short.toString(id))), Sound.Source.VOICE, version < 393 ? 1.0E9f : 1.0f, 1.0f);
		player.stopSound(sound); //Velocity stopSound not implemented
		
	}

}
