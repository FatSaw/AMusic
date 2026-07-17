package me.bomb.amusic.velocity.event;

import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.velocity.command.UploadmusicCommand;

public final class DisconnectHandler implements EventHandler<DisconnectEvent> {
	
	private final AMusic amusic;
	private final ConcurrentHashMap<Object,InetAddress> playerips;
	private final UploadmusicCommand uploadmusic;
	
	public DisconnectHandler(AMusic amusic, ConcurrentHashMap<Object,InetAddress> playerips, UploadmusicCommand uploadmusic) {
		this.amusic = amusic;
		this.playerips = playerips;
		this.uploadmusic = uploadmusic;
	}

	@Override
	public void execute(DisconnectEvent event) {
		Player player = event.getPlayer();
		UUID playeruuid = player.getUniqueId();
		amusic.logout(playeruuid);
		if(uploadmusic != null) uploadmusic.logoutUploader(player);
		if(playerips == null) return;
		playerips.remove(player);
	}

}
