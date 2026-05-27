package me.bomb.amusic.velocity;


import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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
    		if(i > 0x4 && i < 0x30) id = 0x29;
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
	public void startSound(UUID uuid, UUID soundhash, short id, byte partid) {
		Optional<Player> oplayer = server.getPlayer(uuid);
		if(oplayer.isEmpty()) {
			return;
		}
		Player player = oplayer.get();
		final int version = player.getProtocolVersion().getProtocol();
		String musicid = new StringBuilder("amusic.music").append(soundhash.toString()).append(HexUtils.shortToHex(id)).append(HexUtils.byteToHex(partid)).toString();
		if(version > 760) {
			Sound sound = Sound.sound(Key.key(musicid), Sound.Source.VOICE, version < 393 ? 1.0E9f : 1.0f, 1.0f);
			player.playSound(sound, player);
			return;
		}
		
		int packetsize = 19;
		if(version > 209) packetsize += 3;
		if(version == 759) packetsize += 4;
		if (version > 47) ++packetsize;
		byte[] songidb = musicid.getBytes(StandardCharsets.UTF_8);
		boolean bytesoundnamelength = (songidb.length & (0xFFFFFFFF << 7)) == 0;
		if(!bytesoundnamelength) ++packetsize;
		packetsize+=songidb.length;
		ByteBuf buf =  Unpooled.buffer(packetsize, packetsize);
		int pid;
		if(version < 0 || version >= packetid.length || (pid = packetid[version]) == -1) {
			throw new IllegalStateException("Can not encode protocol ".concat(Integer.toString(version)));
		}
		buf.writeByte(pid);
		if (bytesoundnamelength) {
            buf.writeByte(songidb.length);
        } else {
            int w = (songidb.length & 0x7F | 0x80) << 8 | (songidb.length >>> 7);
            buf.writeShort(w);
        }
		buf.writeBytes(songidb);
		if (version > 47) buf.writeByte(9);
        buf.writeInt(0);
        buf.writeInt(Integer.MIN_VALUE);
        buf.writeInt(0);
        buf.writeFloat(version < 393 ? 1.0E9f : 1.0f);
        if (version < 210) {
            buf.writeByte(63);
        } else {
            buf.writeFloat(1.0F);
        }
        if (version >= 759) {
            buf.writeLong(0L);
        }
		((ConnectedPlayer) player).getConnection().write(buf);
	}
}
