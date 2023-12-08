package me.bomb.amusic;

import org.bukkit.entity.Player;

import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;

import net.minecraft.server.v1_7_R4.Entity;
import net.minecraft.server.v1_7_R4.EntityPlayer;
import net.minecraft.server.v1_7_R4.MobEffect;
import net.minecraft.server.v1_7_R4.PacketPlayOutAbilities;
import net.minecraft.server.v1_7_R4.PacketPlayOutAttachEntity;
import net.minecraft.server.v1_7_R4.PacketPlayOutEntityEffect;
import net.minecraft.server.v1_7_R4.PacketPlayOutExperience;
import net.minecraft.server.v1_7_R4.PacketPlayOutHeldItemSlot;
import net.minecraft.server.v1_7_R4.PacketPlayOutPosition;
import net.minecraft.server.v1_7_R4.PacketPlayOutRespawn;
import net.minecraft.server.v1_7_R4.PacketPlayOutUpdateHealth;
import net.minecraft.server.v1_7_R4.PlayerConnection;
import net.minecraft.server.v1_7_R4.WorldServer;

final class LegacySoundStopper_1_7_R4 extends LegacySoundStopper {

	@Override
	protected void stopSound(Player player) {
		EntityPlayer entityplayer = ((CraftPlayer)player).getHandle();
		WorldServer world = entityplayer.r();
		PlayerConnection connection = entityplayer.playerConnection;
		world.getTracker().untrackPlayer(entityplayer);
		world.getTracker().untrackEntity(entityplayer);
		world.getPlayerChunkMap().removePlayer(entityplayer);
		connection.sendPacket(new PacketPlayOutRespawn(entityplayer.dimension-1, entityplayer.world.difficulty, entityplayer.world.getWorldData().getType(), entityplayer.playerInteractManager.getGameMode()));
		connection.sendPacket(new PacketPlayOutRespawn(entityplayer.dimension, entityplayer.world.difficulty, entityplayer.world.getWorldData().getType(), entityplayer.playerInteractManager.getGameMode()));
		world.getPlayerChunkMap().addPlayer(entityplayer);
		world.addEntity(entityplayer);
		connection.sendPacket(new PacketPlayOutExperience(entityplayer.exp, entityplayer.expTotal, entityplayer.expLevel));
        connection.sendPacket(new PacketPlayOutUpdateHealth(entityplayer.getHealth(), entityplayer.getFoodData().getFoodLevel(), entityplayer.getFoodData().getSaturationLevel()));
		connection.sendPacket(new PacketPlayOutAbilities(entityplayer.abilities));
		for(Object effect : entityplayer.getEffects()) {
			connection.sendPacket(new PacketPlayOutEntityEffect(entityplayer.getId(),(MobEffect)effect));
        }
		Entity vehicle = entityplayer.vehicle;
		if(vehicle!=null) {
			connection.sendPacket(new PacketPlayOutAttachEntity(0,entityplayer,vehicle));
		}
		connection.sendPacket(new PacketPlayOutPosition(entityplayer.locX, entityplayer.locY, entityplayer.locZ, entityplayer.yaw, entityplayer.pitch, false));
        connection.sendPacket(new PacketPlayOutHeldItemSlot(entityplayer.inventory.itemInHandIndex));
        entityplayer.updateInventory(entityplayer.defaultContainer);
	}

}
