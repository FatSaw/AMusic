package me.bomb.amusic.velocity.event;

import java.net.InetAddress;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.permission.AMusicPermission;

public final class LoginHandler implements EventHandler<LoginEvent> {
	
	private final AMusic amusic;
	private final ConcurrentHashMap<UUID, EnumSet<AMusicPermission>> playerspermission;
	private final ConcurrentHashMap<Object,InetAddress> playerips;
	private final String joinplaylist;
	
	public LoginHandler(AMusic amusic, ConcurrentHashMap<UUID, EnumSet<AMusicPermission>> playerspermission, ConcurrentHashMap<Object,InetAddress> playerips, String joinplaylist) {
		this.amusic = amusic;
		this.playerspermission = playerspermission;
		this.playerips = playerips;
		this.joinplaylist = joinplaylist;
	}

	@Override
	public void execute(LoginEvent event) {
		Player player = event.getPlayer();
		UUID playeruuid = player.getUniqueId();
		EnumSet<AMusicPermission> permissions = EnumSet.noneOf(AMusicPermission.class);
		for (AMusicPermission permission : AMusicPermission.values()) {
			if(player.hasPermission(permission.permission)) permissions.add(permission);
		}
		this.playerspermission.put(playeruuid, permissions);
		if(playerips != null) playerips.put(player, player.getRemoteAddress().getAddress());
		if(joinplaylist != null) amusic.loadPack(new UUID[] {playeruuid}, joinplaylist, false, null);
	}

}
