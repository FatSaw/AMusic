package me.bomb.amusic.velocity;

import java.util.UUID;

import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import me.bomb.amusic.SoundStopper;

public class ProtocoliseSoundStopper implements SoundStopper {
	
	protected ProtocoliseSoundStopper() {
	}

	@Override
	public void stopSound(UUID uuid, short id) {
		ProtocolizePlayer player = Protocolize.playerProvider().player(uuid);
		if(player==null) {
			return;
		}
		SoundStopPacket soundstop = new SoundStopPacket("amusic.music".concat(Short.toString(id)));
		player.sendPacket(soundstop);
	}

}
