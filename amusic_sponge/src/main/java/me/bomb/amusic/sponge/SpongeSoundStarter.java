package me.bomb.amusic.sponge;

import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Server;
import org.spongepowered.api.effect.sound.SoundCategories;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.entity.living.player.Player;

import com.flowpowered.math.vector.Vector3d;

import me.bomb.amusic.SoundStarter;

public final class SpongeSoundStarter implements SoundStarter {
	
	private final Server server;
	
	protected SpongeSoundStarter(Server server) {
		this.server = server;
	}

	@Override
	public void startSound(UUID uuid, short id) {
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
			
			player.playSound(sound, SoundCategories.VOICE, Vector3d.ZERO, 1.0f, 1.0f);
		}
	}

}
