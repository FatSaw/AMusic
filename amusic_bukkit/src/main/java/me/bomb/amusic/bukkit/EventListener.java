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
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;

import com.drupaldoesnotexists.bundlelib.CrossBundleLib;

import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.resourceserver.ResourceManager;

public final class EventListener implements Listener {
	private final ResourceManager resourcemanager;
	private final PositionTracker positiontracker;
	private final ConcurrentHashMap<Object,InetAddress> playerips;
	private final CrossBundleLib lib;
	
	protected EventListener(ResourceManager resourcemanager, PositionTracker positiontracker, ConcurrentHashMap<Object,InetAddress> playerips, CrossBundleLib lib) {
		this.resourcemanager = resourcemanager;
		this.positiontracker = positiontracker;
		this.playerips = playerips;
		this.lib = lib;
	}
	@EventHandler
	public void playerJoin(PlayerJoinEvent event) {
		try {
			lib.inject(event.getPlayer());
		} catch (Exception e) {
			e.printStackTrace();
		}
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
			resourcemanager.remove(uuid); //Removes resource send if pack applied from client cache
		}
	}
}
