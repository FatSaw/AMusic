package me.bomb.amusic.sponge;

import java.util.UUID;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.living.humanoid.player.ResourcePackStatusEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.ResourcePackStatusEvent.ResourcePackStatus;

import me.bomb.amusic.resourceserver.ResourceManager;

public final class PackStatusEventListener  {

	private final ResourceManager resourcemanager;
	
	protected PackStatusEventListener(ResourceManager resourcemanager) {
		this.resourcemanager = resourcemanager;
	}
	
	@Listener
	public void onResourcePackStatus(ResourcePackStatusEvent event) {
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		ResourcePackStatus status = event.getStatus();
		if(status==ResourcePackStatus.ACCEPTED) {
			resourcemanager.setAccepted(uuid);
			return;
		}
		if(status==ResourcePackStatus.DECLINED||status==ResourcePackStatus.FAILED) {
			resourcemanager.remove(uuid);
			return;
		}
		if(status==ResourcePackStatus.SUCCESSFULLY_LOADED) {
			resourcemanager.remove(uuid); //Removes resource send if pack applied from client cache
		}
	}
}
