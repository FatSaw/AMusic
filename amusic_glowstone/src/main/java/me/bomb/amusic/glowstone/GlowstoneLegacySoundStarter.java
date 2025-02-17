package me.bomb.amusic.glowstone;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;

import me.bomb.amusic.SoundStarter;
import net.glowstone.entity.GlowPlayer;
import net.glowstone.net.message.play.game.NamedSoundEffectMessage;

public final class GlowstoneLegacySoundStarter implements SoundStarter {
	
	protected GlowstoneLegacySoundStarter() {
	}

	@Override
	public void startSound(UUID uuid, short id) {
		if(uuid == null) {
			return;
		}
		GlowPlayer player = (GlowPlayer) Bukkit.getPlayer(uuid);
		player.getSession().send(new NamedSoundEffectMessage("amusic.music".concat(Short.toString(id)), SoundCategory.VOICE, 0, 0, 0, 1.0E9f, 1.0f));

	}

}
