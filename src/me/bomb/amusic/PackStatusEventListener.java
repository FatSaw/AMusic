package me.bomb.amusic;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

public final class PackStatusEventListener implements Listener {
	private final PositionTracker positiontracker;
	protected PackStatusEventListener(PositionTracker positiontracker) {
		this.positiontracker = positiontracker;
	}
	@EventHandler
	public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
		UUID uuid = event.getPlayer().getUniqueId();
		switch (event.getStatus()) {
		case DECLINED:
		case FAILED_DOWNLOAD:
			positiontracker.remove(uuid);
		case SUCCESSFULLY_LOADED:
			CachedResource.remove(uuid);
			break;
		case ACCEPTED:
			CachedResource.setAccepted(uuid);
			break;
		}
	}
}
