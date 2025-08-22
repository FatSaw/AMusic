package me.bomb.amusic.sponge;

import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Server;
import org.spongepowered.api.effect.sound.SoundCategories;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.entity.living.player.Player;

import com.flowpowered.math.vector.Vector3d;

import me.bomb.amusic.SoundStarter;
import me.bomb.amusic.util.HexUtils;

public final class SpongeLegacySoundStarter implements SoundStarter {
	
	private final Server server;
	
	protected SpongeLegacySoundStarter(Server server) {
		this.server = server;
	}

	@Override
	public void startSound(UUID uuid, short id, byte partid) {
		if(uuid == null) {
			return;
		}
		Optional<Player> oplayer = server.getPlayer(uuid);
		if(oplayer.isPresent()) {
			Player player = oplayer.get();
			SoundType sound = new SoundType() {
				String musicid = new StringBuilder("amusic.music").append(HexUtils.shortToHex(id)).append(HexUtils.byteToHex(partid)).toString();
				@Override
				public String getName() {
					return musicid;
				}
				@Override
				public String getId() {
					return musicid;
				}
			};
			
			player.playSound(sound, SoundCategories.VOICE, Vector3d.ZERO, 1.0E9f, 1.0f);
		}
	}

}
