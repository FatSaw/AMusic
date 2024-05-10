package me.bomb.amusic.bukkit;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import com.drupaldoesnotexists.bundlelib.CrossBundleLib;

import me.bomb.amusic.SoundStarter;
import net.minecraft.server.v1_16_R3.MinecraftKey;
import net.minecraft.server.v1_16_R3.Packet;
import net.minecraft.server.v1_16_R3.PacketPlayOutNamedSoundEffect;
import net.minecraft.server.v1_16_R3.PacketPlayOutStopSound;
import net.minecraft.server.v1_16_R3.PlayerConnection;
import net.minecraft.server.v1_16_R3.SoundCategory;
import net.minecraft.server.v1_16_R3.SoundEffect;

public class BundleLibSoundStarter implements SoundStarter {
	
	private CrossBundleLib lib;
	
	protected BundleLibSoundStarter(CrossBundleLib lib) {
		this.lib = lib;
	}

	@Override
	public void startSound(UUID uuid, byte id) {
		Player player = Bukkit.getPlayer(uuid);
		Location loc = player.getLocation();
		PacketPlayOutNamedSoundEffect soundpacket = new PacketPlayOutNamedSoundEffect(new SoundEffect(new MinecraftKey("amusic.music".concat(Byte.toString(id)))), SoundCategory.MASTER, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), 1.0E9f, 1.0F);
		PacketPlayOutStopSound stopsound = new PacketPlayOutStopSound(null, SoundCategory.MASTER);
		PlayerConnection connection = ((CraftPlayer)player).getHandle().playerConnection;
		try {
			connection.sendPacket((Packet<?>) lib.createBundle(stopsound, soundpacket));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
