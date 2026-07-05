package me.bomb.amusic.velocity;

import static me.bomb.amusic.util.HexUtils.fromBytesToHex;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import me.bomb.amusic.PackSender;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;

public final class VelocityPackSender implements PackSender {
	
	private final static byte[] rpack;
	
	static {
		rpack = new byte[] {0x08, 0x4D, 0x43, 0x7C, 0x52, 0x50, 0x61, 0x63, 0x6B};
	}

	private final ProxyServer server;
	
	protected VelocityPackSender(ProxyServer server) {
		this.server = server;
	}
	
	@Override
	public void send(UUID uuid, String url, byte[] sha1) {
		Optional<Player> oplayer = server.getPlayer(uuid);
		if(oplayer.isEmpty()) {
			return;
		}
		Player player = oplayer.get();
		if (player.getProtocolVersion().getProtocol() < 6) {
			final Channel channel = ((ConnectedPlayer) player).getConnection().getChannel();
			final ByteBufAllocator allocator = channel.alloc();
			byte[] urlb = url.getBytes(StandardCharsets.UTF_8);
			int packetsize = urlb.length;
			packetsize += 12;
			ByteBuf buf = allocator.directBuffer(packetsize, packetsize);
			buf.writeByte(0x3F);
			buf.writeBytes(rpack);
			short ulength = (short) urlb.length;
			buf.writeByte(ulength >>> 8);
			buf.writeByte(ulength);
			buf.writeBytes(urlb);
			if (channel.isActive()) {
				channel.writeAndFlush(buf);
			} else {
				buf.release();
			}
			return;
		}
		ResourcePackInfo info = ResourcePackInfo.resourcePackInfo(UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8)), URI.create(url), fromBytesToHex(sha1));
		ResourcePackRequest request = ResourcePackRequest.resourcePackRequest().packs(info).build();
		try {
			player.sendResourcePacks(request);
		} catch (IllegalStateException e) {
		}
	}

}
