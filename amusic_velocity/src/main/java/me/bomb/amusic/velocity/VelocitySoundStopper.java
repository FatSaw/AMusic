package me.bomb.amusic.velocity;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.bomb.amusic.SoundStopper;
import me.bomb.amusic.util.HexUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;

public final class VelocitySoundStopper implements SoundStopper {
	
private final static byte[] packetid;
private final static ChannelIdentifier identifier;
private final static ByteBuf legacysoundstop1, legacysoundstop2;
	
	static {
    	int i = 760;
    	packetid = new byte[i];
    	byte id;
    	while(--i > -1) {
    		id = -1;
    		if(i > 0x188 && i < 0x195) id = 0x4C;
    		if(i > 0x1dc && i < 0x1f3) id = 0x52;
    		if(i > 0x23c && i < 0x243) id = 0x53;
    		if(i > 0x2de && i < 0x2f3) id = 0x52;
    		if(i > 0x2f2 && i < 0x2f5) id = 0x5D;
    		if(i > 0x2f4 && i < 0x2f9) id = 0x5E;
    		packetid[i] = id;
    	}
		identifier = new LegacyChannelIdentifier("MC|StopSound");
    	legacysoundstop1 = Unpooled.buffer(33, 33);
    	legacysoundstop1.writeByte(0x29);
    	legacysoundstop1.writeByte(14);
    	legacysoundstop1.writeBytes("amusic.silence".getBytes(StandardCharsets.US_ASCII));
    	legacysoundstop1.writeInt(0);
        legacysoundstop1.writeInt(Integer.MIN_VALUE);
        legacysoundstop1.writeInt(0);
        legacysoundstop1.writeFloat(1.0E9f);
    	legacysoundstop1.writeByte((byte) 63);

    	legacysoundstop2 = Unpooled.buffer(34, 34);
    	legacysoundstop2.writeByte(0x19);
    	legacysoundstop2.writeByte(14);
    	legacysoundstop2.writeBytes("amusic.silence".getBytes(StandardCharsets.US_ASCII));
    	legacysoundstop2.writeByte(9);
    	legacysoundstop2.writeInt(0);
        legacysoundstop2.writeInt(Integer.MIN_VALUE);
        legacysoundstop2.writeInt(0);
        legacysoundstop2.writeFloat(1.0E9f);
    	legacysoundstop2.writeByte(63);
    }

	private final ProxyServer server;
	
	protected VelocitySoundStopper(ProxyServer server) {
		this.server = server;
	}
	
	@Override
	public void stopSound(UUID uuid, short id, byte partid) {
		Optional<Player> oplayer = server.getPlayer(uuid);
		if(oplayer.isEmpty()) {
			return;
		}
		Player player = oplayer.get();
		final int version = player.getProtocolVersion().getProtocol();
		if(version < 110) {
			//There is no sound stop packet below protocol version 110, it still can be stopped by world rejoin or more than 4 sounds playing on the same time.
			//4 silence sounds should be processed by client in different ticks
			byte i = 5;
			if(version < 48) {
				while(--i > -1) {
					((ConnectedPlayer) player).getConnection().write(legacysoundstop1);
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
					}
				}
			} else if (version > 106) {
				while(--i > -1) {
					((ConnectedPlayer) player).getConnection().write(legacysoundstop2);
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
					}
				}
			}
			return;
		}
		String musicid = new StringBuilder("amusic.music").append(HexUtils.shortToHex(id)).append(HexUtils.byteToHex(partid)).toString();
		
		if(version > 760) {
			SoundStop sound = SoundStop.namedOnSource(Key.key(musicid), Sound.Source.VOICE);
			player.stopSound(sound);
			return;
		}
		
		if(version > 388) {
			int packetsize = 3;
			byte[] songidb = musicid.getBytes(StandardCharsets.UTF_8);
			boolean bytesoundnamelength = (songidb.length & (0xFFFFFFFF << 7)) == 0;
			if(!bytesoundnamelength) ++packetsize;
			
			ByteBuf buf = Unpooled.buffer(packetsize, packetsize);
			buf.writeByte(0x03);
			buf.writeByte(9);
			if (bytesoundnamelength) {
	            buf.writeByte(songidb.length);
	        } else {
	            int w = (songidb.length & 0x7F | 0x80) << 8 | (songidb.length >>> 7);
	            buf.writeShort(w);
	        }
			buf.writeBytes(songidb);
			((ConnectedPlayer) player).getConnection().write(buf);
			return;
		}
		player.sendPluginMessage(identifier, new StringPluginMessageEncoder(musicid));
	}

}
