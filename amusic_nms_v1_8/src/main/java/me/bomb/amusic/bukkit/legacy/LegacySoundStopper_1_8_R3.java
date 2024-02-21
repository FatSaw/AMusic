package me.bomb.amusic.bukkit.legacy;

import java.util.Collections;
import java.util.UUID;

import org.bukkit.entity.Player;

import me.bomb.amusic.SoundStopper;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;

import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.MobEffect;
import net.minecraft.server.v1_8_R3.PacketPlayOutAbilities;
import net.minecraft.server.v1_8_R3.PacketPlayOutAttachEntity;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityEffect;
import net.minecraft.server.v1_8_R3.PacketPlayOutExperience;
import net.minecraft.server.v1_8_R3.PacketPlayOutHeldItemSlot;
import net.minecraft.server.v1_8_R3.PacketPlayOutPosition;
import net.minecraft.server.v1_8_R3.PacketPlayOutRespawn;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnPosition;
import net.minecraft.server.v1_8_R3.PacketPlayOutUpdateHealth;
import net.minecraft.server.v1_8_R3.PlayerConnection;
import net.minecraft.server.v1_8_R3.WorldServer;

public final class LegacySoundStopper_1_8_R3 implements SoundStopper {

	@Override
	public void stopSound(UUID uuid, String soundname) {
		if(uuid == null) {
			return;
		}
		Player player = Bukkit.getPlayer(uuid);
		EntityPlayer entityplayer = ((CraftPlayer)player).getHandle();
		WorldServer world = entityplayer.u();
		PlayerConnection connection = entityplayer.playerConnection;
		world.getTracker().untrackPlayer(entityplayer);
		world.getTracker().untrackEntity(entityplayer);
		world.getPlayerChunkMap().removePlayer(entityplayer);
		connection.sendPacket(new PacketPlayOutRespawn(entityplayer.dimension-1, entityplayer.world.getDifficulty(), entityplayer.world.getWorldData().getType(), entityplayer.playerInteractManager.getGameMode()));
		connection.sendPacket(new PacketPlayOutRespawn(entityplayer.dimension, entityplayer.world.getDifficulty(), entityplayer.world.getWorldData().getType(), entityplayer.playerInteractManager.getGameMode()));
		world.getPlayerChunkMap().addPlayer(entityplayer);
		world.addEntity(entityplayer);
		connection.sendPacket(new PacketPlayOutSpawnPosition(world.getSpawn()));
		connection.sendPacket(new PacketPlayOutExperience(entityplayer.exp, entityplayer.expTotal, entityplayer.expLevel));
        connection.sendPacket(new PacketPlayOutUpdateHealth(entityplayer.getHealth(), entityplayer.getFoodData().getFoodLevel(), entityplayer.getFoodData().getSaturationLevel()));
		connection.sendPacket(new PacketPlayOutAbilities(entityplayer.abilities));
		for(MobEffect effect : entityplayer.getEffects()) {
			connection.sendPacket(new PacketPlayOutEntityEffect(entityplayer.getId(),effect));
        }
		Entity vehicle = entityplayer.vehicle;
		if(vehicle!=null) {
			connection.sendPacket(new PacketPlayOutAttachEntity(0,entityplayer,vehicle));
		}
		connection.sendPacket(new PacketPlayOutPosition(entityplayer.locX, entityplayer.locY, entityplayer.locZ, entityplayer.yaw, entityplayer.pitch, Collections.emptySet()));
        connection.sendPacket(new PacketPlayOutHeldItemSlot(entityplayer.inventory.itemInHandIndex));
        entityplayer.updateInventory(entityplayer.defaultContainer);
	}

}
