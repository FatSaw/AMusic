package me.bomb.amusic.bukkit.moveapplylistener;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.entity.Player;

public abstract class PackApplyListener {
	
	protected final ConcurrentHashMap<UUID, AtomicBoolean[]> applylisteners = new ConcurrentHashMap<UUID, AtomicBoolean[]>(16,0.75f,1);

	public static void registerApplyListenTask(Player player) {
	}
	
	public static boolean applied(UUID playeruuid) {
		return false;
	}
	
	public static void reset(UUID playeruuid) {
	}
	
	public static void unregisterApplyListenTask(Player player) {
	}
	
	protected void addApplyListenTask(Player player) {
	}

	protected void removeApplyListenTask(Player player) {
	}

}
