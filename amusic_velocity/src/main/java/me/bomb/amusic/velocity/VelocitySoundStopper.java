package me.bomb.amusic.velocity;

import java.util.Optional;
import java.util.UUID;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import me.bomb.amusic.SoundStopper;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

public class VelocitySoundStopper implements SoundStopper {

	private final ProxyServer server;
	
	protected VelocitySoundStopper(ProxyServer server) {
		this.server = server;
	}
	
	@Override
	public void stopSound(UUID uuid, byte id) {
		Optional<Player> oplayer = server.getPlayer(uuid);
		if(oplayer.isEmpty()) {
			return;
		}
		Player player = oplayer.get();
		Sound sound = Sound.sound(Key.key("amusic.music".concat(Byte.toString(id))), Sound.Source.MASTER, 1.0E9f, 1.0f);
		player.stopSound(sound); //Velocity stopSound not implemented
		
	}

}
