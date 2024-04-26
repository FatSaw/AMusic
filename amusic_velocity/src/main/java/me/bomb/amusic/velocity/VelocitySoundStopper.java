package me.bomb.amusic.velocity;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import me.bomb.amusic.SoundStopper;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;

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
		String soundname = "amusic.music".concat(Byte.toString(id));
		Sound sound = new Sound() {
			@Override
			public float volume() {
				return 1.0E9f;
			}
			
			@Override
			public @NotNull Source source() {
				return Source.MASTER;
			}
			
			@Override
			public @NotNull OptionalLong seed() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public float pitch() {
				return 1.0f;
			}
			
			@Override
			public @NotNull Key name() {
				return new Key() {
					
					@Override
					public @NotNull String value() {
						return soundname;
					}
					
					@Override
					public @NotNull String namespace() {
						// TODO Auto-generated method stub
						return null;
					}
					
					@Override
					public @NotNull String asString() {
						// TODO Auto-generated method stub
						return soundname;
					}
				};
			}
			
			@Override
			public @NotNull SoundStop asStop() {
				// TODO Auto-generated method stub
				return null;
			}
		};
		player.stopSound(sound);
	}

}
