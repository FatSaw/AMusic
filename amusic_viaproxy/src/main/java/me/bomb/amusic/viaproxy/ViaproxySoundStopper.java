package me.bomb.amusic.viaproxy;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.viaversion.viaversion.api.connection.UserConnection;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.bomb.amusic.SoundStopper;
import me.bomb.amusic.util.HexUtils;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

public final class ViaproxySoundStopper implements SoundStopper {

	private final static byte[] packetid;
	private final static byte[] stopsound;
	private final static ByteBuf legacysoundstop1, legacysoundstop2;

	static {
		int i = 760;
		packetid = new byte[i];
		byte id;
		while (--i > -1) {
			id = -1;
			if (i > 0x04 && i < 0x2f) id = 0x17;
			if (i > 0x6a && i < 0x155) id = 0x18;
			if (i > 0x188 && i < 0x195) id = 0x4C;
			if (i > 0x1dc && i < 0x1f3) id = 0x52;
			if (i > 0x23c && i < 0x243) id = 0x53;
			if (i > 0x2de && i < 0x2f3) id = 0x52;
			if (i > 0x2f2 && i < 0x2f5) id = 0x5D;
			if (i > 0x2f4 && i < 0x2f9) id = 0x5E;
			packetid[i] = id;
		}

		stopsound = new byte[] {0x0C, 0x4D, 0x43, 0x7C, 0x53, 0x74, 0x6F, 0x70, 0x53, 0x6F, 0x75, 0x6E, 0x64};
		
		legacysoundstop1 = Unpooled.buffer(42, 42);
		legacysoundstop1.writeByte(0x29);
		legacysoundstop1.writeByte(23);
		legacysoundstop1.writeBytes("amusic:internal.silence".getBytes(StandardCharsets.US_ASCII));
		legacysoundstop1.writeInt(0);
		legacysoundstop1.writeInt(Integer.MIN_VALUE);
		legacysoundstop1.writeInt(0);
		legacysoundstop1.writeFloat(1.0E9f);
		legacysoundstop1.writeByte((byte) 63);

		legacysoundstop2 = Unpooled.buffer(43, 43);
		legacysoundstop2.writeByte(0x19);
		legacysoundstop2.writeByte(23);
		legacysoundstop2.writeBytes("amusic:internal.silence".getBytes(StandardCharsets.US_ASCII));
		legacysoundstop2.writeByte(9);
		legacysoundstop2.writeInt(0);
		legacysoundstop2.writeInt(Integer.MIN_VALUE);
		legacysoundstop2.writeInt(0);
		legacysoundstop2.writeFloat(1.0E9f);
		legacysoundstop2.writeByte(63);
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
		UserConnection connection = player.getUserConnection();
		if (version < 110) {
			// There is no sound stop packet below protocol version 110, it still can be
			// stopped by world rejoin or more than 4 sounds playing on the same time.
			// 4 silence sounds should be processed by client in different ticks
			byte i = 5;
			if (version < 48) {
				while (--i > -1) {
					connection.sendRawPacket(legacysoundstop1);
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
					}
				}
			} else if (version > 106) {
				while (--i > -1) {
					connection.sendRawPacket(legacysoundstop2);
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
					}
				}
			}
			return;
		}
		String musicid = new StringBuilder("amusic:internal.").append(soundhash.toString()).append(HexUtils.shortToHex(id)).append(HexUtils.byteToHex(part)).toString();

		if (version > 760) {
			return;
		}
		int pid;
		if (version < 0 || version >= packetid.length || (pid = packetid[version]) == -1) {
			throw new IllegalStateException("Can not encode protocol ".concat(Integer.toString(version)));
		}
		
		byte[] songidb = musicid.getBytes(StandardCharsets.UTF_8);
		if (version > 388) {
			int packetsize = 4;
			boolean bytesoundnamelength = (songidb.length & (0xFFFFFFFF << 7)) == 0;
			if (!bytesoundnamelength)
				++packetsize;
			packetsize += songidb.length;
			ByteBuf buf = Unpooled.buffer(packetsize, packetsize);
			
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

		if(songidb.length != 0x3A) {
			return;
		}
		ByteBuf buf = Unpooled.buffer(74, 74);
		buf.writeByte(pid);
		buf.writeBytes(stopsound);
		if(version < 6) {
			buf.writeByte(0x3B);
			buf.writeByte(0x00);
			buf.writeBytes(songidb);
		} else {
			buf.writeByte(0x00);
			buf.writeByte(0x3A);
			buf.writeBytes(songidb);
		}
		connection.sendRawPacket(buf);
	}

}
