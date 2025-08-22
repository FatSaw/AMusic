package me.bomb.amusic.glowstone;

import java.io.IOException;
import java.util.UUID;

import org.bukkit.Server;

import com.flowpowered.network.util.ByteBufUtils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.bomb.amusic.SoundStopper;
import me.bomb.amusic.util.HexUtils;
import net.glowstone.entity.GlowPlayer;
import net.glowstone.net.message.play.game.PluginMessage;

public final class GlowstoneSoundStopper implements SoundStopper {
	
	private final Server server;
	
	protected GlowstoneSoundStopper(Server server) {
		this.server = server;
	}

	@Override
	public void stopSound(UUID uuid, short id, byte partid) {
		if(uuid == null) {
			return;
		}
		String musicid = new StringBuilder("amusic.music").append(HexUtils.shortToHex(id)).append(HexUtils.byteToHex(partid)).toString();
		GlowPlayer player = (GlowPlayer) server.getPlayer(uuid);
		try {
			ByteBuf buffer = Unpooled.buffer();
			ByteBufUtils.writeUTF8(buffer, "voice"); //Source
	        ByteBufUtils.writeUTF8(buffer, musicid); //Sound
	        player.getSession().sendAndRelease(new PluginMessage("MC|StopSound", buffer.array()), buffer);

		} catch (IOException e) {
		}
	}

}
