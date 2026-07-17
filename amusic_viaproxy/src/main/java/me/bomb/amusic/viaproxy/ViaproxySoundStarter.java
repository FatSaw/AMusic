package me.bomb.amusic.viaproxy;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.viaversion.viaversion.api.connection.UserConnection;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import me.bomb.amusic.SoundStarter;
import me.bomb.amusic.util.HexUtils;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

public final class ViaproxySoundStarter implements SoundStarter {
	
	final static byte[] packetid;
	
	static {
    	int i = 777;
    	packetid = new byte[i];
    	byte id;
    	while(--i > -1) {
    		id = -1;
    		if(i > 0x03 && i < 0x30) id = 0x29;
    		if(i > 0x6a && i < 0x155) id = 0x19;
    		if(i > 0x188 && i < 0x195) id = 0x1A;
    		if(i > 0x1dc && i < 0x1f3) id = 0x19;
    		if(i > 0x23c && i < 0x243) id = 0x1A;
    		if(i > 0x2de && i < 0x2e1) id = 0x19;
    		if(i > 0x2ee && i < 0x2f3) id = 0x18;
    		if(i > 0x2f2 && i < 0x2f7) id = 0x19;
    		if(i == 0x2f7) id = 0x16;
    		if(i == 0x2f8) id = 0x17;
    		if(i == 0x2f9) id = 0x5E;
    		if(i > 0x2f9 && i < 0x2fc) id = 0x62;
    		if(i == 0x2fc) id = 0x64;
    		if(i == 0x2fd) id = 0x66;
    		if(i > 0x2fd && i < 0x300) id = 0x68;
    		if(i > 0x2ff && i < 0x302) id = 0x6F;
    		if(i > 0x301 && i < 0x305) id = 0x6E;
    		if(i > 0x304 && i < 0x307) id = 0x73;
    		if(i > 0x306 && i < 0x309) id = 0x75;
    		packetid[i] = id;
    	}
    }
	
	private final ConcurrentHashMap<UUID,ProxyConnection> players;
	
	public ViaproxySoundStarter(ConcurrentHashMap<UUID,ProxyConnection> players) {
		this.players = players;
	}

	@Override
	public void startSound(UUID uuid, UUID soundhash, short id, byte part) {
		ProxyConnection player;
		if(uuid == null || soundhash == null || (player = players.get(uuid)) == null) {
			return;
		}
		final int version = player.getClientVersion().getVersion();
		int pid;
		if(version < 0 || version >= packetid.length || (pid = packetid[version]) == -1) {
			throw new IllegalStateException("Can not encode protocol ".concat(Integer.toString(version)));
		}
		final Channel channel = player.getC2P();
		final UserConnection connection = player.getUserConnection();
		final ByteBufAllocator allocator = channel.alloc();
		String musicid = new StringBuilder("minecraft:amusic.internal.").append(soundhash.toString()).append(HexUtils.shortToHex(id)).append(HexUtils.byteToHex(part)).toString();
		int packetsize = 19;
		if(version > 760) packetsize += 2;
		if(version > 758) packetsize += 8;
		if(version > 209) packetsize += 3;
		if (version > 47) ++packetsize;
		byte[] songidb = musicid.getBytes(StandardCharsets.UTF_8);
		boolean bytesoundnamelength = (songidb.length & (0xFFFFFFFF << 7)) == 0;
		if(!bytesoundnamelength) ++packetsize;
		packetsize+=songidb.length;
		ByteBuf buf = allocator.directBuffer(packetsize, packetsize);
		buf.writeByte(pid);
		if(version > 760) buf.writeByte(0);
		if (bytesoundnamelength) {
			buf.writeByte(songidb.length);
		} else {
			int w = (songidb.length & 0x7F | 0x80) << 8 | (songidb.length >>> 7);
			buf.writeShort(w);
		}
		buf.writeBytes(songidb);
		if(version > 760) buf.writeByte(0);
		if (version > 47) buf.writeByte(version < 393 ? 0 : 9);
		buf.writeInt(0);
		buf.writeInt(0);
		buf.writeInt(0);
		buf.writeFloat(version < 393 ? 1.0E9f : 1.0f);
		if (version < 210) {
			buf.writeByte(63);
		} else {
			buf.writeFloat(1.0f);
		}
		if (version > 758) {
			buf.writeLong(0L);
		}
		connection.sendRawPacket(buf);
	}

	@Override
	public void startSound(UUID uuid, UUID soundhash, short id, byte part, double x, double y, double z, float volume, float pitch) {
		ProxyConnection player;
		if(uuid == null || soundhash == null || (player = players.get(uuid)) == null) {
			return;
		}
		final int version = player.getClientVersion().getVersion();
		int pid;
		if(version < 0 || version >= packetid.length || (pid = packetid[version]) == -1) {
			throw new IllegalStateException("Can not encode protocol ".concat(Integer.toString(version)));
		}
		final Channel channel = player.getC2P();
		final UserConnection connection = player.getUserConnection();
		final ByteBufAllocator allocator = channel.alloc();
		String musicid = new StringBuilder("minecraft:amusic.internal.").append(soundhash.toString()).append(HexUtils.shortToHex(id)).append(HexUtils.byteToHex(part)).toString();
		int packetsize = 19;
		if(version > 760) packetsize += 2;
		if(version > 758) packetsize += 8;
		if(version > 209) packetsize += 3;
		if (version > 47) ++packetsize;
		byte[] songidb = musicid.getBytes(StandardCharsets.UTF_8);
		boolean bytesoundnamelength = (songidb.length & (0xFFFFFFFF << 7)) == 0;
		if(!bytesoundnamelength) ++packetsize;
		packetsize+=songidb.length;
		ByteBuf buf = allocator.directBuffer(packetsize, packetsize);
		buf.writeByte(pid);
		if(version > 760) buf.writeByte(0);
		if (bytesoundnamelength) {
			buf.writeByte(songidb.length);
		} else {
			int w = (songidb.length & 0x7F | 0x80) << 8 | (songidb.length >>> 7);
			buf.writeShort(w);
		}
		buf.writeBytes(songidb);
		if(version > 760) buf.writeByte(0);
		if (version > 47) buf.writeByte(version < 393 ? 0 : 9);
		buf.writeInt((int) (x * 8.0D));
		buf.writeInt((int) (y * 8.0D));
		buf.writeInt((int) (z * 8.0D));
		buf.writeFloat(volume);
		if (version < 210) {
			buf.writeByte((int) (pitch * 63.0F));
		} else {
			buf.writeFloat(pitch);
		}
		if (version > 758) {
			buf.writeLong(0L);
		}
		connection.sendRawPacket(buf);
	}

}
