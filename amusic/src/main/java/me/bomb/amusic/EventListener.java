package me.bomb.amusic;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import me.bomb.amusic.moveapplylistener.PackApplyListener;
import me.bomb.amusic.resourceserver.CachedResource;

public final class EventListener implements Listener {
	private final PositionTracker positiontracker;
	protected EventListener(PositionTracker positiontracker) {
		this.positiontracker = positiontracker;
	}
	@EventHandler
	public void playerJoin(PlayerJoinEvent event) {
		PackApplyListener.registerApplyListenTask(event.getPlayer());
	}
	@EventHandler
	public void playerQuit(PlayerQuitEvent event) {
		UUID playeruuid = event.getPlayer().getUniqueId();
		positiontracker.remove(playeruuid);
		CachedResource.remove(playeruuid);
		PackApplyListener.unregisterApplyListenTask(event.getPlayer());
	}
	@EventHandler
	public void playerRespawn(PlayerRespawnEvent event) {
		positiontracker.stopMusic(event.getPlayer());
	}
	@EventHandler
	public void playerWorldChange(PlayerChangedWorldEvent event) {
		positiontracker.stopMusic(event.getPlayer());
	}
}
