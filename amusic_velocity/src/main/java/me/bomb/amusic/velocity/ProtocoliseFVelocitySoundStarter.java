package me.bomb.amusic.velocity;

import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_13;

import java.util.Optional;
import java.util.UUID;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import me.bomb.amusic.SoundStarter;
import me.bomb.amusic.util.HexUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

public class ProtocoliseFVelocitySoundStarter implements SoundStarter {

	private final ProxyServer server;

	protected ProtocoliseFVelocitySoundStarter(ProxyServer server) {
		this.server = server;
	}

	@Override
	public void startSound(UUID uuid, short id, byte partid) {
		try {
			ProtocolizePlayer player = Protocolize.playerProvider().player(uuid);
			if (player == null) {
				return;
			}
			final int version = player.protocolVersion();
			String musicid = new StringBuilder("amusic.music").append(HexUtils.shortToHex(id)).append(HexUtils.byteToHex(partid)).toString();
			NamedSoundEffectPacket customsound = new NamedSoundEffectPacket(musicid, 9, 0, Integer.MIN_VALUE, 0, version < MINECRAFT_1_13 ? 1.0E9f : 1.0f, 1.0f);
			player.sendPacket(customsound);
		} catch (IllegalStateException e) {
			Optional<Player> oplayer = server.getPlayer(uuid);
			if(oplayer.isEmpty()) {
				return;
			}
			Player player = oplayer.get();
			final int version = player.getProtocolVersion().getProtocol();
			String musicid = new StringBuilder("amusic.music").append(HexUtils.shortToHex(id)).append(HexUtils.byteToHex(partid)).toString();
			Sound sound = Sound.sound(Key.key(musicid), Sound.Source.VOICE, version < 393 ? 1.0E9f : 1.0f, 1.0f);
			player.playSound(sound, player);
		}
	}

}
