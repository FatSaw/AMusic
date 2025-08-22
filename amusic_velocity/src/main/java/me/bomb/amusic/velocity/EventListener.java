package me.bomb.amusic.velocity;

import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent.Status;
import com.velocitypowered.api.proxy.Player;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.velocity.command.UploadmusicCommand;

public final class EventListener {
	private final AMusic amusic;
	private final ResourceManager resourcemanager;
	private final PositionTracker positiontracker;
	private final ConcurrentHashMap<Object,InetAddress> playerips;
	private final UploadmusicCommand uploadmusic;
	protected EventListener(AMusic amusic, ResourceManager resourcemanager, PositionTracker positiontracker, ConcurrentHashMap<Object,InetAddress> playerips, UploadmusicCommand uploadmusic) {
		this.amusic = amusic;
		this.resourcemanager = resourcemanager;
		this.positiontracker = positiontracker;
		this.playerips = playerips;
		this.uploadmusic = uploadmusic;
	}
	@Subscribe
	public void onLoginEvent(LoginEvent event) {
		if(playerips == null) return;
		Player player = event.getPlayer();
		playerips.put(player, player.getRemoteAddress().getAddress());
	}
	@Subscribe
	public void onDisconnectEvent(DisconnectEvent event) {
		Player player = event.getPlayer();
		UUID playeruuid = player.getUniqueId();
		amusic.logout(playeruuid);
		positiontracker.remove(playeruuid);
		resourcemanager.remove(playeruuid);
		if(uploadmusic != null) uploadmusic.logoutUploader(player);
		if(playerips == null) return;
		playerips.remove(player);
	}
	@Subscribe
	public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
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
