package org.bukkit.craftbukkit.v1_7_R4.entity;

import org.bukkit.entity.Player;

import net.minecraft.server.v1_7_R4.EntityPlayer;
/**
 * Fake nms
 */
public abstract class CraftPlayer implements Player {
	public EntityPlayer getHandle() {
		return null;
	}
}
