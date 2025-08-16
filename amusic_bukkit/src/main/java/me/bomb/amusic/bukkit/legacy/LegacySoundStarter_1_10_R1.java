package me.bomb.amusic.bukkit.legacy;

import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftPlayer;

import me.bomb.amusic.SoundStarter;
import net.minecraft.server.v1_10_R1.PacketPlayOutCustomSoundEffect;
import net.minecraft.server.v1_10_R1.SoundCategory;

public final class LegacySoundStarter_1_10_R1 implements SoundStarter {

	private final Server server;
	
	public LegacySoundStarter_1_10_R1(Server server) {
		this.server = server;
	}
	
	@Override
	public void startSound(UUID uuid, short id) {
		if(uuid == null) {
			return;
		}
		CraftPlayer player = (CraftPlayer) server.getPlayer(uuid);
		player.getHandle().playerConnection.sendPacket(new PacketPlayOutCustomSoundEffect("amusic.music".concat(Short.toString(id)), SoundCategory.VOICE, 0.0d, 0.0d, 0.0d, 1.0E9f, 1.0f));
	}

}
