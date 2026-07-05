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
import me.bomb.amusic.SoundStopper;
import me.bomb.amusic.util.HexUtils;
import me.bomb.amusic.util.SendSilence;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;

public final class VelocitySoundStopper implements SoundStopper {
	
	private final static byte[] packetid;
	private final static byte[] stopsound;
	
	static {
    	int i = 760;
    	packetid = new byte[i];
    	byte id;
    	while(--i > -1) {
    		id = -1;
    		if (i > 0x6a && i < 0x155) id = 0x18;
    		if(i > 0x188 && i < 0x195) id = 0x4C;
    		if(i > 0x1dc && i < 0x1f3) id = 0x52;
    		if(i > 0x23c && i < 0x243) id = 0x53;
    		if(i > 0x2de && i < 0x2f3) id = 0x52;
    		if(i > 0x2f2 && i < 0x2f5) id = 0x5D;
    		if(i > 0x2f4 && i < 0x2f8) id = 0x5E;
    		packetid[i] = id;
    	}
    	
    	stopsound = new byte[] {0x0C, 0x4D, 0x43, 0x7C, 0x53, 0x74, 0x6F, 0x70, 0x53, 0x6F, 0x75, 0x6E, 0x64};
    }

	private final ProxyServer server;
	
	protected VelocitySoundStopper(ProxyServer server) {
		this.server = server;
	}
	
	@Override
	public void stopSound(UUID uuid, UUID soundhash, short id, byte part) {
		Optional<Player> oplayer = server.getPlayer(uuid);
		if(oplayer.isEmpty()) {
			return;
		}
		Player player = oplayer.get();
		final int version = player.getProtocolVersion().getProtocol();
		if(version < 110) {
			final Channel channel = ((ConnectedPlayer) player).getConnection().getChannel();
			new SendSilence(channel, version, (byte)5).run();
			try {
				Thread.sleep(250L);
			} catch (InterruptedException e) {
			}
			return;
		}
		String musicid = new StringBuilder("minecraft:amusic.internal.").append(soundhash.toString()).append(HexUtils.shortToHex(id)).append(HexUtils.byteToHex(part)).toString();
		if(version > 760) {
			SoundStop sound = SoundStop.namedOnSource(Key.key(musicid), Sound.Source.VOICE);
			player.stopSound(sound);
			return;
		}
		int pid;
		if(version < 0 || version >= packetid.length || (pid = packetid[version]) == -1) {
			throw new IllegalStateException("Can not encode protocol ".concat(Integer.toString(version)));
		}
		final Channel channel = ((ConnectedPlayer) player).getConnection().getChannel();
		final ByteBufAllocator allocator = channel.alloc();
		byte[] songidb = musicid.getBytes(StandardCharsets.UTF_8);
		boolean bytesoundnamelength = (songidb.length & (0xFFFFFFFF << 7)) == 0;
		if(version > 388) {
			int packetsize = 4;
			if(!bytesoundnamelength) ++packetsize;
			packetsize+=songidb.length;
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
			if (channel.isActive()) {
				channel.writeAndFlush(buf);
			} else {
				buf.release();
			}
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
		if (channel.isActive()) {
			channel.writeAndFlush(buf);
		} else {
			buf.release();
		}
	}

}
