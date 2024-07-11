package org.bukkit.craftbukkit.v1_8_R3.entity;

import org.bukkit.entity.Player;

import net.minecraft.server.v1_8_R3.EntityPlayer;
/**
 * Fake nms
 */
public abstract class CraftPlayer implements Player {
	public EntityPlayer getHandle() {
		return null;
	}
}
