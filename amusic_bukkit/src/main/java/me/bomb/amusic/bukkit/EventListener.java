package me.bomb.amusic.bukkit;

import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.bukkit.command.UploadmusicCommand;
import me.bomb.amusic.resourceserver.ResourceManager;

public final class EventListener implements Listener {
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
	@EventHandler
	public void playerJoin(PlayerJoinEvent event) {
		if(playerips == null) return;
		Player player = event.getPlayer();
		playerips.put(player, player.getAddress().getAddress());
	}
	@EventHandler
	public void playerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		UUID playeruuid = player.getUniqueId();
		positiontracker.remove(playeruuid);
		resourcemanager.remove(playeruuid);
		if(uploadmusiccmd != null) uploadmusiccmd.logoutUploader(player);
		if(playerips == null) return;
		playerips.remove(player);
	}
	@EventHandler
	public void playerRespawn(PlayerRespawnEvent event) {
		positiontracker.stopMusic(event.getPlayer().getUniqueId());
	}
	@EventHandler
	public void playerWorldChange(PlayerChangedWorldEvent event) {
		positiontracker.stopMusic(event.getPlayer().getUniqueId());
	}
}
