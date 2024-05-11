package me.bomb.amusic.velocity;

import java.util.UUID;

import dev.simplix.protocolize.api.Location;
import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import me.bomb.amusic.SoundStarter;

public class ProtocoliseSoundStarter implements SoundStarter {
	
	protected ProtocoliseSoundStarter() {
	}

	@Override
	public void startSound(UUID uuid, short id) {
		ProtocolizePlayer player = Protocolize.playerProvider().player(uuid);
		if(player==null) {
			return;
		}
		Location location = player.location();
		NamedSoundEffectPacket customsound = new NamedSoundEffectPacket("amusic.music".concat(Short.toString(id)), 0, location.x(), location.y(), location.z(), 1.0E9f, 1.0f);
		player.sendPacket(customsound);
	}

}
