package me.bomb.amusic.velocity.event;

import java.net.InetAddress;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.permission.AMusicPermission;

public final class DisconnectHandler implements EventHandler<DisconnectEvent> {
	
	private final AMusic amusic;
	private final ConcurrentHashMap<UUID, EnumSet<AMusicPermission>> playerspermission;
	private final ConcurrentHashMap<Object,InetAddress> playerips;
	
	public DisconnectHandler(AMusic amusic, ConcurrentHashMap<UUID, EnumSet<AMusicPermission>> playerspermission, ConcurrentHashMap<Object,InetAddress> playerips) {
		this.amusic = amusic;
		this.playerspermission = playerspermission;
		this.playerips = playerips;
	}

	@Override
	public void execute(DisconnectEvent event) {
		Player player = event.getPlayer();
		UUID playeruuid = player.getUniqueId();
		this.playerspermission.remove(playeruuid);
		amusic.logout(playeruuid);
		if(playerips == null) return;
		playerips.remove(player);
	}

}
