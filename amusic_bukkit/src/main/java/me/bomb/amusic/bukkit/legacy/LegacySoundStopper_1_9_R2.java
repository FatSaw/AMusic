package me.bomb.amusic.bukkit.legacy;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

import io.netty.buffer.Unpooled;
import me.bomb.amusic.SoundStopper;
import net.minecraft.server.v1_9_R2.PlayerConnection;
import net.minecraft.server.v1_9_R2.EntityPlayer;
import net.minecraft.server.v1_9_R2.PacketDataSerializer;
import net.minecraft.server.v1_9_R2.PacketPlayOutCustomPayload;

public final class LegacySoundStopper_1_9_R2 implements SoundStopper {

	@Override
	public void stopSound(UUID uuid, short id) {
		if(uuid == null) {
			return;
		}
		Player player = Bukkit.getPlayer(uuid);
		EntityPlayer entityplayer = ((CraftPlayer)player).getHandle();
		PlayerConnection connection = entityplayer.playerConnection;
		PacketDataSerializer packetdataserializer = new PacketDataSerializer(Unpooled.buffer());
        packetdataserializer.a("");
        packetdataserializer.a("amusic.music".concat(Short.toString(id)));
        connection.sendPacket(new PacketPlayOutCustomPayload("MC|StopSound", packetdataserializer));
	}

}
