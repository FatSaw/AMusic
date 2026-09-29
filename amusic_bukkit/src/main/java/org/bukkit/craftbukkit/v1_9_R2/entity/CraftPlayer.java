package org.bukkit.craftbukkit.v1_9_R2.entity;

import org.bukkit.entity.Player;

import net.minecraft.server.v1_9_R2.EntityPlayer;
/**
 * Fake nms
 */
public abstract class CraftPlayer implements Player {
	public EntityPlayer getHandle() {
		return null;
	}
}
