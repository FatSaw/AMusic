package me.bomb.amusic.bukkit;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;

import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.bukkit.moveapplylistener.PackApplyListener;
import me.bomb.amusic.resourceserver.ResourceManager;

public final class PackStatusEventListener implements Listener {

	private final ResourceManager resourcemanager;
	private final PositionTracker positiontracker;
	protected PackStatusEventListener(ResourceManager resourcemanager, PositionTracker positiontracker) {
		this.resourcemanager = resourcemanager;
		this.positiontracker = positiontracker;
	}
	@EventHandler
	public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		Status status = event.getStatus();
		if(status==Status.ACCEPTED) {
			resourcemanager.setAccepted(uuid);
			return;
		}
		if(status==Status.DECLINED||status==Status.FAILED_DOWNLOAD) {
			positiontracker.remove(uuid);
			resourcemanager.remove(uuid);
			return;
		}
		if(status==Status.SUCCESSFULLY_LOADED) {
			PackApplyListener.reset(uuid);
			if(resourcemanager.remove(uuid)) { //Removes resource send if pack applied from client cache
				LangOptions.loadmusic_finished_cache.sendMsgPlayer(player);
			} else {
				LangOptions.loadmusic_finished_upload.sendMsgPlayer(player);
			}
		}
	}
}
