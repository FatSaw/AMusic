package org.bukkit.craftbukkit.v1_11_R1.entity;


import org.bukkit.entity.Player;

import net.minecraft.server.v1_11_R1.EntityPlayer;
/**
 * Fake nms
 */
public abstract class CraftPlayer implements Player {

	public EntityPlayer getHandle() {
		return null;
	}

}
