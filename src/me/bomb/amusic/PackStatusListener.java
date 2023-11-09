package me.bomb.amusic;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class PackStatusListener implements Listener {
	private final PositionTracker positiontracker;
	protected PackStatusListener(PositionTracker positiontracker) {
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
	@EventHandler
	public void playerQuit(PlayerQuitEvent event) {
		UUID playeruuid = event.getPlayer().getUniqueId();
		positiontracker.remove(playeruuid);
		CachedResource.remove(playeruuid);
	}
	@EventHandler
	public void playerRespawn(PlayerRespawnEvent event) {
		positiontracker.stopMusic(event.getPlayer());
	}
}
