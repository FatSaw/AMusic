package me.bomb.amusic.glowstone;

import java.io.IOException;
import java.util.UUID;

import org.bukkit.Bukkit;

import com.flowpowered.network.util.ByteBufUtils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.bomb.amusic.SoundStopper;
import net.glowstone.entity.GlowPlayer;
import net.glowstone.net.message.play.game.PluginMessage;

public final class GlowstoneSoundStopper implements SoundStopper {
	
	protected GlowstoneSoundStopper() {
	}

	@Override
	public void stopSound(UUID uuid, short id) {
		if(uuid == null) {
			return;
		}
		GlowPlayer player = (GlowPlayer) Bukkit.getPlayer(uuid);
		try {
			ByteBuf buffer = Unpooled.buffer();
			ByteBufUtils.writeUTF8(buffer, "voice"); //Source
	        ByteBufUtils.writeUTF8(buffer, "amusic.music".concat(Short.toString(id))); //Sound
	        player.getSession().sendAndRelease(new PluginMessage("MC|StopSound", buffer.array()), buffer);

		} catch (IOException e) {
		}
	}

}
