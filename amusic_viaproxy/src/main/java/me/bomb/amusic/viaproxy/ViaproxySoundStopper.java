package me.bomb.amusic.viaproxy;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.viaversion.viaversion.api.connection.UserConnection;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import me.bomb.amusic.SoundStopper;
import me.bomb.amusic.util.HexUtils;
import me.bomb.amusic.util.SendSilence;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

public final class ViaproxySoundStopper implements SoundStopper {

	private final static byte[] packetid;
	private final static byte[] stopsound;

	static {
		int i = 777;
		packetid = new byte[i];
		byte id;
		while (--i > -1) {
			id = -1;
			if (i > 0x6a && i < 0x155) id = 0x18;
			if (i > 0x188 && i < 0x195) id = 0x4C;
			if (i > 0x1dc && i < 0x1f3) id = 0x52;
			if (i > 0x23c && i < 0x243) id = 0x53;
			if (i > 0x2de && i < 0x2f3) id = 0x52;
			if (i > 0x2f2 && i < 0x2f5) id = 0x5D;
			if (i > 0x2f4 && i < 0x2f8) id = 0x5E;
			if (i == 0x2f8) id = 0x61;
			if (i == 0x2f9) id = 0x5F;
			if (i > 0x2f9 && i < 0x2fc) id = 0x63;
			if (i == 0x2fc) id = 0x66;
			if (i == 0x2fd) id = 0x68;
			if (i > 0x2fd && i < 0x300) id = 0x6A;
			if (i > 0x2ff && i < 0x302) id = 0x71;
			if (i > 0x301 && i < 0x305) id = 0x70;
			if (i > 0x304 && i < 0x309) id = 0x75;
			packetid[i] = id;
		}

		stopsound = new byte[] {0x0C, 0x4D, 0x43, 0x7C, 0x53, 0x74, 0x6F, 0x70, 0x53, 0x6F, 0x75, 0x6E, 0x64};
	}

	private final ConcurrentHashMap<UUID, ProxyConnection> players;

	public ViaproxySoundStopper(ConcurrentHashMap<UUID, ProxyConnection> players) {
		this.players = players;
	}

	@Override
	public void stopSound(UUID uuid, UUID soundhash, short id, byte part) {
		ProxyConnection player;
		if (uuid == null || soundhash == null || (player = players.get(uuid)) == null) {
			return;
		}
		final int version = player.getClientVersion().getVersion();
		final Channel channel = player.getC2P();
		if (version < 110) {
			new SendSilence(channel, version, (byte)5).run();
			try {
				Thread.sleep(250L);
			} catch (InterruptedException e) {
			}
			return;
		}
		int pid;
		if (version < 0 || version >= packetid.length || (pid = packetid[version]) == -1) {
			throw new IllegalStateException("Can not encode protocol ".concat(Integer.toString(version)));
		}
		final UserConnection connection = player.getUserConnection();
		final ByteBufAllocator allocator = channel.alloc();
		String musicid = new StringBuilder("minecraft:amusic.internal.").append(soundhash.toString()).append(HexUtils.shortToHex(id)).append(HexUtils.byteToHex(part)).toString();
		byte[] songidb = musicid.getBytes(StandardCharsets.UTF_8);
		boolean bytesoundnamelength = (songidb.length & (0xFFFFFFFF << 7)) == 0;
		if (version > 388) {
			int packetsize = 4;
			if (!bytesoundnamelength)
				++packetsize;
			packetsize += songidb.length;
			ByteBuf buf = allocator.directBuffer(packetsize, packetsize);
			
			buf.writeByte(pid);
			buf.writeByte(0x03);
			buf.writeByte(9);
			if (bytesoundnamelength) {
				buf.writeByte(songidb.length);
			} else {
				int w = (songidb.length & 0x7F | 0x80) << 8 | (songidb.length >>> 7);
				buf.writeShort(w);
			}
			buf.writeBytes(songidb);
			connection.sendRawPacket(buf);
			return;
		}
		int packetsize = 16;
		if (!bytesoundnamelength)
			++packetsize;
		ByteBuf buf = allocator.directBuffer(packetsize, packetsize);
		buf.writeByte(pid);
		buf.writeBytes(stopsound);
		buf.writeByte(0x00);
		if (bytesoundnamelength) {
			buf.writeByte(songidb.length);
		} else {
			int w = (songidb.length & 0x7F | 0x80) << 8 | (songidb.length >>> 7);
			buf.writeShort(w);
		}
		buf.writeBytes(songidb);
		connection.sendRawPacket(buf);
	}

}
