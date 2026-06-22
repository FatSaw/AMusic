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
	public void startSound(UUID uuid, UUID soundhash, short id, byte part) {
		final Optional<Player> oplayer;
		if(uuid == null || soundhash == null || !(oplayer = server.getPlayer(uuid)).isPresent()) {
			return;
		}
		Player player = oplayer.get();
		SoundType sound = new AMusicSoundType(uuid, soundhash, id, part);
		player.playSound(sound, SoundCategories.VOICE, player.getPosition(), 1.0f, 1.0f);
	}

	@Override
	public void startSound(UUID uuid, UUID soundhash, short id, byte part, double x, double y, double z, float volume, float pitch) {
		final Optional<Player> oplayer;
		if(uuid == null || soundhash == null || !(oplayer = server.getPlayer(uuid)).isPresent()) {
			return;
		}
		SoundType sound = new AMusicSoundType(uuid, soundhash, id, part);
		oplayer.get().playSound(sound, SoundCategories.VOICE, new Vector3d(x, y, z), volume, pitch);
	}

}
