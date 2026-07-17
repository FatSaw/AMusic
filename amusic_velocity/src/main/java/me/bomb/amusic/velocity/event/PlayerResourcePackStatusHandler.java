package me.bomb.amusic.velocity.event;

import java.util.UUID;

import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent.Status;
import com.velocitypowered.api.proxy.Player;

import me.bomb.amusic.resourceserver.ResourceManager;

public final class PlayerResourcePackStatusHandler implements EventHandler<PlayerResourcePackStatusEvent> {

	private final ResourceManager resourcemanager;
	
	public PlayerResourcePackStatusHandler(ResourceManager resourcemanager) {
		this.resourcemanager = resourcemanager;
	}
	
	@Override
	public void execute(PlayerResourcePackStatusEvent event) {
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		Status status = event.getStatus();
		if(status==Status.ACCEPTED) {
			resourcemanager.setAccepted(uuid);
			return;
		}
		if(status==Status.DECLINED||status==Status.FAILED_DOWNLOAD) {
			resourcemanager.remove(uuid);
			return;
		}
		if(status==Status.SUCCESSFUL) {
			resourcemanager.remove(uuid); //Removes resource send if pack applied from client cache
		}
	}

}
