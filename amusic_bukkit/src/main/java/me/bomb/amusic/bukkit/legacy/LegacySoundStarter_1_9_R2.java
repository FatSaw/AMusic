package me.bomb.amusic.bukkit.legacy;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftPlayer;

import me.bomb.amusic.api.SoundStarter;
import net.minecraft.server.v1_9_R2.PacketPlayOutCustomSoundEffect;
import net.minecraft.server.v1_9_R2.SoundCategory;

public final class LegacySoundStarter_1_9_R2 implements SoundStarter {
	
	private final Server server;
	
	public LegacySoundStarter_1_9_R2(Server server) {
		this.server = server;
	}
	
	@Override
	public void startSound(UUID uuid, UUID soundhash) {
		if(uuid == null || soundhash == null) {
			return;
		}
		String musicid = AMUSIC_SOUNDID_PREFIX.concat(soundhash.toString());
		CraftPlayer player = (CraftPlayer) server.getPlayer(uuid);
		Location loc = player.getLocation();
		player.getHandle().playerConnection.sendPacket(new PacketPlayOutCustomSoundEffect(musicid, SoundCategory.VOICE, loc.getX(), loc.getY(), loc.getZ(), 1.0E9f, 1.0f));
	}

}
