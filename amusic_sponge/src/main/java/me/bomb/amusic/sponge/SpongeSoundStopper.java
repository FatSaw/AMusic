package me.bomb.amusic.sponge;

import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Server;
import org.spongepowered.api.effect.sound.SoundCategories;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.entity.living.player.Player;


import me.bomb.amusic.SoundStopper;

public final class SpongeSoundStopper implements SoundStopper {
	
	private final Server server;
	
	protected SpongeSoundStopper(Server server) {
		this.server = server;
	}

	@Override
	public void stopSound(UUID uuid, short id) {
		if(uuid == null) {
			return;
		}
		Optional<Player> oplayer = server.getPlayer(uuid);
		if(oplayer.isPresent()) {
			Player player = oplayer.get();
			SoundType sound = new SoundType() {
				@Override
				public String getName() {
					return "amusic.music".concat(Short.toString(id));
				}
				@Override
				public String getId() {
					return "amusic.music".concat(Short.toString(id));
				}
			};
			player.stopSounds(sound);
		}
		
	}

}
