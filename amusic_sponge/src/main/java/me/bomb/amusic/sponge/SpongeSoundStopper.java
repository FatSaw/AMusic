package me.bomb.amusic.sponge;

import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Server;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.entity.living.player.Player;


import me.bomb.amusic.SoundStopper;
import me.bomb.amusic.util.HexUtils;

public final class SpongeSoundStopper implements SoundStopper {
	
	private final Server server;
	
	protected SpongeSoundStopper(Server server) {
		this.server = server;
	}

	@Override
	public void stopSound(UUID uuid, short id, byte partid) {
		if(uuid == null) {
			return;
		}
		final String musicid = new StringBuilder("amusic.music").append(HexUtils.shortToHex(id)).append(HexUtils.byteToHex(partid)).toString();
		Optional<Player> oplayer = server.getPlayer(uuid);
		if(oplayer.isPresent()) {
			Player player = oplayer.get();
			SoundType sound = new SoundType() {
				@Override
				public String getName() {
					return musicid;
				}
				@Override
				public String getId() {
					return musicid;
				}
			};
			player.stopSounds(sound);
		}
		
	}

}
