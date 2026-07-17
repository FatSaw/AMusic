package me.bomb.amusic.velocity.event;

import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;

import me.bomb.amusic.AMusic;

public final class LoginHandler implements EventHandler<LoginEvent> {
	
	private final AMusic amusic;
	private final ConcurrentHashMap<Object,InetAddress> playerips;
	private final String joinplaylist;
	
	public LoginHandler(AMusic amusic, ConcurrentHashMap<Object,InetAddress> playerips, String joinplaylist) {
		this.amusic = amusic;
		this.playerips = playerips;
		this.joinplaylist = joinplaylist;
	}

	@Override
	public void execute(LoginEvent event) {
		Player player = event.getPlayer();
		if(playerips != null) playerips.put(player, player.getRemoteAddress().getAddress());
		if(joinplaylist != null) amusic.loadPack(new UUID[] {player.getUniqueId()}, joinplaylist, false, null);
	}

}
