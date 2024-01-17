package me.bomb.amusic;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;

public final class PackStatusEventListener implements Listener {
	private final PositionTracker positiontracker;
	protected PackStatusEventListener(PositionTracker positiontracker) {
		this.positiontracker = positiontracker;
	}
	@EventHandler
	public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
		UUID uuid = event.getPlayer().getUniqueId();
		Status status = event.getStatus();
		if(status==Status.ACCEPTED) {
			CachedResource.setAccepted(uuid);
			return;
		}
		if(status==Status.DECLINED||status==Status.FAILED_DOWNLOAD) {
			positiontracker.remove(uuid);
		} else if(status==Status.SUCCESSFULLY_LOADED) {
			PackApplyListener.reset(uuid);
		}
		CachedResource.remove(uuid);
	}
}
