package me.bomb.amusic.viaproxy;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.mojang.authlib.GameProfile;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import me.bomb.amusic.AMusic;
import net.raphimc.viaproxy.plugins.events.ClientLoggedInEvent;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

public final class LoginLogoutHandler implements Consumer<ClientLoggedInEvent> {
	
	private final AMusic amusic;
	private final ConcurrentHashMap<UUID,ProxyConnection> players;
	private final ConcurrentHashMap<Object,InetAddress> playerips;
	private final String joinplaylist;
	private final ConcurrentHashMap<String, UUID> uuidByPlayername;
	
	public LoginLogoutHandler(AMusic amusic, ConcurrentHashMap<UUID,ProxyConnection> players, ConcurrentHashMap<Object,InetAddress> playerips, String joinplaylist, ConcurrentHashMap<String, UUID> uuidByPlayername) {
		this.amusic = amusic;
		this.players = players;
		this.playerips = playerips;
		this.joinplaylist = joinplaylist;
		this.uuidByPlayername = uuidByPlayername;
	}
	
	@Override
	public void accept(ClientLoggedInEvent event) {
		ProxyConnection proxyConnection = event.getProxyConnection();
		Channel channel = proxyConnection.getC2P();
		InetSocketAddress socketAddress = (InetSocketAddress) channel.remoteAddress();
		InetAddress address = socketAddress.getAddress();
		GameProfile gameprofile = proxyConnection.getGameProfile();
		String playername = gameprofile.getName();
		UUID playeruuid = gameprofile.getId();
		channel.closeFuture().addListener(new DisconnectHandler(this.amusic, this.players, this.playerips, this.uuidByPlayername, playeruuid, playername));
		players.put(playeruuid, proxyConnection);
		uuidByPlayername.put(playername, playeruuid);
		if(playerips != null) playerips.put(playeruuid, address);
		if(joinplaylist != null) amusic.loadPack(new UUID[] {playeruuid}, joinplaylist, false, null);
	}
	
	public final static class DisconnectHandler implements ChannelFutureListener {
		private final AMusic amusic;
		private final ConcurrentHashMap<UUID,ProxyConnection> players;
		private final ConcurrentHashMap<Object,InetAddress> playerips;
		private final ConcurrentHashMap<String, UUID> uuidByPlayername;
		private final UUID playeruuid;
		private final String playername;

		protected DisconnectHandler(AMusic amusic, ConcurrentHashMap<UUID,ProxyConnection> players, ConcurrentHashMap<Object,InetAddress> playerips, ConcurrentHashMap<String, UUID> uuidByPlayername, UUID playeruuid, String playername) {
			this.amusic = amusic;
			this.players = players;
			this.playerips = playerips;
			this.uuidByPlayername = uuidByPlayername;
			this.playeruuid = playeruuid;
			this.playername = playername;
		}
		
		@Override
		public void operationComplete(ChannelFuture channel) throws Exception {
			amusic.logout(playeruuid);
			players.remove(playeruuid);
			uuidByPlayername.remove(playername);
			if(playerips == null) return;
			playerips.remove(playeruuid);
		}
		
	}

}
