package net.minecraft.server.v1_7_R4;
/**
 * Fake nms
 */
public final class WorldServer extends World {
	public EntityTracker getTracker() {
		return null;
	}
	public PlayerChunkMap getPlayerChunkMap() {
		return null;
	}
	public BlockPosition getSpawn() {
		return null;
	}
}
