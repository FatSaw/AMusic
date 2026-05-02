package me.bomb.amusic.sponge;

import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.sponge.command.UploadmusicCommand;

public final class EventListener {
	private final AMusic amusic;
	private final ResourceManager resourcemanager;
	private final PositionTracker positiontracker;
	private final ConcurrentHashMap<Object,InetAddress> playerips;
	private final UploadmusicCommand uploadmusiccmd;
	private final String joinplaylist;
	protected EventListener(AMusic amusic, ResourceManager resourcemanager, PositionTracker positiontracker, ConcurrentHashMap<Object,InetAddress> playerips, UploadmusicCommand uploadmusiccmd, String joinplaylist) {
		this.amusic = amusic;
		this.resourcemanager = resourcemanager;
		this.positiontracker = positiontracker;
		this.playerips = playerips;
		this.uploadmusiccmd = uploadmusiccmd;
		this.joinplaylist = joinplaylist;
	}
	@Listener
	public void playerJoin(ClientConnectionEvent.Join event) {
		Player player = event.getTargetEntity();
		if(playerips != null) playerips.put(player, player.getConnection().getAddress().getAddress());
		if(joinplaylist != null) amusic.loadPack(new UUID[] {player.getUniqueId()}, joinplaylist, false, null);
	}
	@Listener
	public void playerDisconnect(ClientConnectionEvent.Disconnect event) {
		Player player = event.getTargetEntity();
		UUID playeruuid = player.getUniqueId();
		amusic.logout(playeruuid);
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
