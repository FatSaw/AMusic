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
import me.bomb.amusic.bukkit.moveapplylistener.PackApplyListener;
import me.bomb.amusic.resourceserver.ResourceManager;

public final class EventListener implements Listener {
	private final ResourceManager resourcemanager;
	private final PositionTracker positiontracker;
	private final ConcurrentHashMap<Object,InetAddress> playerips;
	protected EventListener(ResourceManager resourcemanager, PositionTracker positiontracker, ConcurrentHashMap<Object,InetAddress> playerips) {
		this.resourcemanager = resourcemanager;
		this.positiontracker = positiontracker;
		this.playerips = playerips;
	}
	@EventHandler
	public void playerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		playerips.put(player, player.getAddress().getAddress());
		PackApplyListener.registerApplyListenTask(player);
	}
	@EventHandler
	public void playerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		playerips.remove(player);
		UUID playeruuid = player.getUniqueId();
		positiontracker.remove(playeruuid);
		resourcemanager.remove(playeruuid);
		PackApplyListener.unregisterApplyListenTask(player);
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
