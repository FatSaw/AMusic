package me.bomb.amusic.sponge;

import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;

import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.sponge.command.UploadmusicCommand;



public final class EventListener {
	private final ResourceManager resourcemanager;
	private final PositionTracker positiontracker;
	private final ConcurrentHashMap<Object,InetAddress> playerips;
	private final UploadmusicCommand uploadmusiccmd;
	protected EventListener(ResourceManager resourcemanager, PositionTracker positiontracker, ConcurrentHashMap<Object,InetAddress> playerips, UploadmusicCommand uploadmusiccmd) {
		this.resourcemanager = resourcemanager;
		this.positiontracker = positiontracker;
		this.playerips = playerips;
		this.uploadmusiccmd = uploadmusiccmd;
	}
	@Listener
	public void playerJoin(ClientConnectionEvent.Join event) {
		if(playerips == null) return;
		Player player = event.getTargetEntity();
		playerips.put(player, player.getConnection().getAddress().getAddress());
	}
	@Listener
	public void playerDisconnect(ClientConnectionEvent.Disconnect event) {
		Player player = event.getTargetEntity();
		UUID playeruuid = player.getUniqueId();
		positiontracker.remove(playeruuid);
		resourcemanager.remove(playeruuid);
		if(uploadmusiccmd != null) uploadmusiccmd.logoutUploader(player);
		if(playerips == null) return;
		playerips.remove(player);
	}
	@Listener
	public void playerRespawn(RespawnPlayerEvent event) {
		positiontracker.stopMusic(event.getTargetEntity().getUniqueId());
	}
	@Listener
	public void playerWorldChange(MoveEntityEvent.Teleport event) {
		if(event.getFromTransform().getLocation().getExtent() == event.getToTransform().getLocation().getExtent()) {
			return;
		}
		positiontracker.stopMusic(event.getTargetEntity().getUniqueId());
	}
}
