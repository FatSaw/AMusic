package me.bomb.amusic.bukkit.legacy;

import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import io.netty.buffer.Unpooled;
import me.bomb.amusic.SoundStopper;
import net.minecraft.server.v1_10_R1.EntityPlayer;
import net.minecraft.server.v1_10_R1.PacketDataSerializer;
import net.minecraft.server.v1_10_R1.PlayerConnection;
import net.minecraft.server.v1_10_R1.PacketPlayOutCustomPayload;

public final class LegacySoundStopper_1_10_R1 implements SoundStopper {

	private final Server server;
	
	public LegacySoundStopper_1_10_R1(Server server) {
		this.server = server;
	}
	
	@Override
	public void stopSound(UUID uuid, short id) {
		if(uuid == null) {
			return;
		}
		Player player = server.getPlayer(uuid);
		EntityPlayer entityplayer = ((CraftPlayer)player).getHandle();
		PlayerConnection connection = entityplayer.playerConnection;
		PacketDataSerializer packetdataserializer = new PacketDataSerializer(Unpooled.buffer());
        packetdataserializer.a("");
        packetdataserializer.a("amusic.music".concat(Short.toString(id)));
        connection.sendPacket(new PacketPlayOutCustomPayload("MC|StopSound", packetdataserializer));
		
	}

}
