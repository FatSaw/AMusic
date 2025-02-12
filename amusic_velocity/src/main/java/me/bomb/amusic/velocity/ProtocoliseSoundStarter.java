package me.bomb.amusic.velocity;

import java.util.UUID;

import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import me.bomb.amusic.SoundStarter;

import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_13;

public final class ProtocoliseSoundStarter implements SoundStarter {
	
	protected ProtocoliseSoundStarter() {
	}

	@Override
	public void startSound(UUID uuid, short id) {
		ProtocolizePlayer player = Protocolize.playerProvider().player(uuid);
		if(player==null) {
			return;
		}
		final int version = player.protocolVersion();
		
		NamedSoundEffectPacket customsound = new NamedSoundEffectPacket("amusic.music".concat(Short.toString(id)), 9, 0, Integer.MIN_VALUE, 0, version < MINECRAFT_1_13 ? 1.0E9f : 1.0f, 1.0f);
		player.sendPacket(customsound);
	}

}
