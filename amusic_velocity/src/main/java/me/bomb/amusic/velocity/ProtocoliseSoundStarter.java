package me.bomb.amusic.velocity;

import java.util.UUID;

import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import me.bomb.amusic.SoundStarter;
import me.bomb.amusic.util.HexUtils;

import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_13;

public final class ProtocoliseSoundStarter implements SoundStarter {
	
	protected ProtocoliseSoundStarter() {
	}

	@Override
	public void startSound(UUID uuid, short id, byte partid) {
		ProtocolizePlayer player = Protocolize.playerProvider().player(uuid);
		if(player==null) {
			return;
		}
		final int version = player.protocolVersion();
		String musicid = new StringBuilder("amusic.music").append(HexUtils.shortToHex(id)).append(HexUtils.byteToHex(partid)).toString();
		NamedSoundEffectPacket customsound = new NamedSoundEffectPacket(musicid, 9, 0, Integer.MIN_VALUE, 0, version < MINECRAFT_1_13 ? 1.0E9f : 1.0f, 1.0f);
		player.sendPacket(customsound);
	}

}
