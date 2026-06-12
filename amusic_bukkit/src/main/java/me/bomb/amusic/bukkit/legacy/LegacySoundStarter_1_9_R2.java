package me.bomb.amusic.bukkit.legacy;

import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftPlayer;

import me.bomb.amusic.SoundStarter;
import me.bomb.amusic.util.HexUtils;
import net.minecraft.server.v1_9_R2.PacketPlayOutCustomSoundEffect;
import net.minecraft.server.v1_9_R2.SoundCategory;

public final class LegacySoundStarter_1_9_R2 implements SoundStarter {
	
	private final Server server;
	
	public LegacySoundStarter_1_9_R2(Server server) {
		this.server = server;
	}
	
	@Override
	public void startSound(UUID uuid, UUID soundhash, short id, byte part) {
		this.startSound(uuid, soundhash, id, part, 0d, 0d, 0d, 1.0E9f, 1.0f);
	}

	@Override
	public void startSound(UUID uuid, UUID soundhash, short id, byte part, double x, double y, double z, float volume, float pitch) {
		if(uuid == null || soundhash == null) {
			return;
		}
		String musicid = new StringBuilder("amusic:internal.").append(soundhash.toString()).append(HexUtils.shortToHex(id)).append(HexUtils.byteToHex(part)).toString();
		CraftPlayer player = (CraftPlayer) server.getPlayer(uuid);
		player.getHandle().playerConnection.sendPacket(new PacketPlayOutCustomSoundEffect(musicid, SoundCategory.VOICE, x, y, z, volume, pitch));
	}

}
