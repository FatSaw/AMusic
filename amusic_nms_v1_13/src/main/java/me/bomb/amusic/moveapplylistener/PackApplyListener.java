package me.bomb.amusic.moveapplylistener;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.entity.Player;

public abstract class PackApplyListener {
	
	protected final ConcurrentHashMap<UUID, AtomicBoolean[]> applylisteners = new ConcurrentHashMap<UUID, AtomicBoolean[]>(16,0.75f,1);

	protected void addApplyListenTask(Player player) {
	}

	protected void removeApplyListenTask(Player player) {
	}

}
