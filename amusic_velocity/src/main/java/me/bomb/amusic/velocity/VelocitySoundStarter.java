package me.bomb.amusic.velocity;


import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import me.bomb.amusic.SoundStarter;
import me.bomb.amusic.util.HexUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

public final class VelocitySoundStarter implements SoundStarter {
	
	final static byte[] packetid;
	
	static {
    	int i = 760;
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
    		packetid[i] = id;
    	}
    }
	
	private final ProxyServer server;

	protected VelocitySoundStarter(ProxyServer server) {
		this.server = server;
	}
	
	@Override
	public void startSound(UUID uuid, UUID soundhash, short id, byte part) {
		final Optional<Player> oplayer;
		if(uuid == null || soundhash == null || (oplayer = server.getPlayer(uuid)).isEmpty()) {
			return;
		}
		Player player = oplayer.get();
		final int version = player.getProtocolVersion().getProtocol();
		String musicid = new StringBuilder("minecraft:amusic.internal.").append(soundhash.toString()).append(HexUtils.shortToHex(id)).append(HexUtils.byteToHex(part)).toString();
		if(version > 760) {
			Sound sound = Sound.sound(Key.key(musicid), Sound.Source.VOICE, 1.0f, 1.0f);
			player.playSound(sound, player);
			return;
		}
		int pid;
		if(version < 0 || version >= packetid.length || (pid = packetid[version]) == -1) {
			throw new IllegalStateException("Can not encode protocol ".concat(Integer.toString(version)));
		}
		final Channel channel = ((ConnectedPlayer) player).getConnection().getChannel();
		final ByteBufAllocator allocator = channel.alloc();
		int packetsize = 19;
		if(version > 209) packetsize += 3;
		if(version == 759) packetsize += 8;
		if (version > 47) ++packetsize;
		byte[] songidb = musicid.getBytes(StandardCharsets.UTF_8);
		boolean bytesoundnamelength = (songidb.length & (0xFFFFFFFF << 7)) == 0;
		if(!bytesoundnamelength) ++packetsize;
		packetsize+=songidb.length;
		ByteBuf buf = allocator.directBuffer(packetsize, packetsize);
		buf.writeByte(pid);
		if (bytesoundnamelength) {
			buf.writeByte(songidb.length);
		} else {
			int w = (songidb.length & 0x7F | 0x80) << 8 | (songidb.length >>> 7);
			buf.writeShort(w);
		}
		buf.writeBytes(songidb);
		if (version > 47) buf.writeByte(version < 393 ? 0 : 9);
		buf.writeInt(0);
		buf.writeInt(0);
		buf.writeInt(0);
		buf.writeFloat(version < 393 ? 1.0E9f : 1.0f);
		if (version < 210) {
			buf.writeByte(63);
		} else {
			buf.writeFloat(1.0F);
		}
		if (version == 759) {
			buf.writeLong(0L);
		}
		if (channel.isActive()) {
			channel.writeAndFlush(buf);
		} else {
			buf.release();
		}
	}

	@Override
	public void startSound(UUID uuid, UUID soundhash, short id, byte part, double x, double y, double z, float volume, float pitch) {
		final Optional<Player> oplayer;
		if(uuid == null || soundhash == null || (oplayer = server.getPlayer(uuid)).isEmpty()) {
			return;
		}
		Player player = oplayer.get();
		final int version = player.getProtocolVersion().getProtocol();
		String musicid = new StringBuilder("minecraft:amusic.internal.").append(soundhash.toString()).append(HexUtils.shortToHex(id)).append(HexUtils.byteToHex(part)).toString();
		if(version > 760) {
			Sound sound = Sound.sound(Key.key(musicid), Sound.Source.VOICE, volume, pitch);
			player.playSound(sound, player);
			//player.playSound(sound, x, y, z); //NOT IMPLEMENTED
			return;
		}
		int pid;
		if(version < 0 || version >= packetid.length || (pid = packetid[version]) == -1) {
			throw new IllegalStateException("Can not encode protocol ".concat(Integer.toString(version)));
		}
		final Channel channel = ((ConnectedPlayer) player).getConnection().getChannel();
		final ByteBufAllocator allocator = channel.alloc();
		int packetsize = 19;
		if(version > 209) packetsize += 3;
		if(version == 759) packetsize += 8;
		if (version > 47) ++packetsize;
		byte[] songidb = musicid.getBytes(StandardCharsets.UTF_8);
		boolean bytesoundnamelength = (songidb.length & (0xFFFFFFFF << 7)) == 0;
		if(!bytesoundnamelength) ++packetsize;
		packetsize+=songidb.length;
		ByteBuf buf = allocator.directBuffer(packetsize, packetsize);
		buf.writeByte(pid);
		if (bytesoundnamelength) {
			buf.writeByte(songidb.length);
        } else {
			int w = (songidb.length & 0x7F | 0x80) << 8 | (songidb.length >>> 7);
			buf.writeShort(w);
		}
		buf.writeBytes(songidb);
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
		if (version == 759) {
			buf.writeLong(0L);
		}
		if (channel.isActive()) {
			channel.writeAndFlush(buf);
		} else {
			buf.release();
		}
	}
}
