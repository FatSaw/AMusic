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
	public void startSound(UUID uuid, short id, byte partid) {
		if(uuid == null) {
			return;
		}
		String musicid = new StringBuilder("amusic.music").append(HexUtils.shortToHex(id)).append(HexUtils.byteToHex(partid)).toString();
		GlowPlayer player = (GlowPlayer) server.getPlayer(uuid);
		player.getSession().send(new NamedSoundEffectMessage(musicid, SoundCategory.VOICE, 0, 0, 0, 1.0f, 1.0f));
	}

}
