package me.bomb.amusic.bukkit.legacy;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftPlayer;

import me.bomb.amusic.SoundStarter;
import net.minecraft.server.v1_9_R2.PacketPlayOutCustomSoundEffect;
import net.minecraft.server.v1_9_R2.SoundCategory;

public final class LegacySoundStarter_1_9_R2 implements SoundStarter {
	
	@Override
	public void startSound(UUID uuid, short id) {
		if(uuid == null) {
			return;
		}
		CraftPlayer player = (CraftPlayer) Bukkit.getPlayer(uuid);
		player.getHandle().playerConnection.sendPacket(new PacketPlayOutCustomSoundEffect("amusic.music".concat(Short.toString(id)), SoundCategory.VOICE, 0.0d, 0.0d, 0.0d, 1.0E9f, 1.0f));
	}

}
