package me.bomb.amusic.glowstone;

import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.SoundCategory;

import me.bomb.amusic.SoundStarter;
import me.bomb.amusic.util.HexUtils;
import net.glowstone.entity.GlowPlayer;
import net.glowstone.net.message.play.game.NamedSoundEffectMessage;

public final class GlowstoneSoundStarter implements SoundStarter {
	
	private final Server server;
	
	protected GlowstoneSoundStarter(Server server) {
		this.server = server;
	}

	@Override
	public void startSound(UUID uuid, UUID soundhash, short id, byte part) {
		this.startSound(uuid, soundhash, id, part, 0d, 0d, 0d, 1.0f, 1.0f);
	}

	@Override
	public void startSound(UUID uuid, UUID soundhash, short id, byte part, double x, double y, double z, float volume, float pitch) {
		if(uuid == null || soundhash == null) {
			return;
		}
		String musicid = new StringBuilder("amusic:internal.").append(soundhash.toString()).append(HexUtils.shortToHex(id)).append(HexUtils.byteToHex(part)).toString();
		GlowPlayer player = (GlowPlayer) server.getPlayer(uuid);
		player.getSession().send(new NamedSoundEffectMessage(musicid, SoundCategory.VOICE, x, y, z, volume, pitch));
	}

}
