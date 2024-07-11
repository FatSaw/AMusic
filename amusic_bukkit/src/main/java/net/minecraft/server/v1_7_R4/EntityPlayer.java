package net.minecraft.server.v1_7_R4;

import java.util.Collection;

/**
 * Fake nms
 */
public final class EntityPlayer extends Entity {
	public PlayerConnection playerConnection;
	public Entity vehicle;
	public int dimension, expTotal, expLevel;
	public double locX, locY, locZ;
	public float yaw, pitch, exp;
	public Container defaultContainer;
	public PlayerAbilities abilities;
	public PlayerInventory inventory;
	public World world;
	public PlayerInteractManager playerInteractManager;

	public WorldServer r() {
		return null;
	}

	public int getId() {
		return 0;
	}

	public Collection<MobEffect> getEffects() {
		return null;
	}
	public void updateInventory(Container container) {
		
	}
	public final float getHealth() {
		return 0.0f;
	}
	public FoodMetaData getFoodData() {
		return null;
	}
}
