package me.bomb.amusic.viaproxy;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.viaversion.viaversion.api.connection.UserConnection;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.bomb.amusic.PackSender;
import me.bomb.amusic.util.HexUtils;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

public final class ViaproxyPackSender implements PackSender {

	final static byte[] packetid;
	private final static byte[] rpack;
	
	static {
    	int i = 760;
    	packetid = new byte[i];
    	byte id;
    	while(--i > -1) {
    		id = -1;
    		if(i == 0x05) id = 0x17;
    		if(i > 0x2e && i < 0x30) id = 0x48;
    		if(i > 0x6a && i < 0x13d) id = 0x32;
    		if(i > 0x14e && i < 0x150) id = 0x33;
    		if(i > 0x14f && i < 0x155) id = 0x34;
    		if(i > 0x188 && i < 0x195) id = 0x37;
    		if(i > 0x1dc && i < 0x1f3) id = 0x39;
    		if(i > 0x23c && i < 0x243) id = 0x3A;
    		if(i > 0x2de && i < 0x2e2) id = 0x39;
    		if(i > 0x2ee && i < 0x2f3) id = 0x38;
    		if(i > 0x2f2 && i < 0x2f7) id = 0x3C;
    		if(i == 0x2f7) id = 0x3A;
    		packetid[i] = id;
    	}
		rpack = new byte[] {0x08, 0x4D, 0x43, 0x7C, 0x52, 0x50, 0x61, 0x63, 0x6B};
	}
	
	private final ConcurrentHashMap<UUID,ProxyConnection> players;
	
	public ViaproxyPackSender(ConcurrentHashMap<UUID,ProxyConnection> players) {
		this.players = players;
	}

	@Override
	public void send(UUID uuid, String url, byte[] sha1) {
		ProxyConnection player;
		if(uuid == null || url == null || sha1 == null || sha1.length != 20 || (player = players.get(uuid)) == null) {
			return;
		}
		final int version = player.getClientVersion().getVersion();
		UserConnection connection = player.getUserConnection();
		int pid;
		if(version < 0 || version >= packetid.length || (pid = packetid[version]) == -1) {
			throw new IllegalStateException("Can not encode protocol ".concat(Integer.toString(version)));
		}
		byte[] urlb = url.getBytes(StandardCharsets.UTF_8);
		int packetsize = urlb.length;
		if(version < 6) {
			packetsize += 12;
			ByteBuf buf = Unpooled.buffer(packetsize, packetsize);
			buf.writeByte(pid);
			buf.writeBytes(rpack);
			short ulength = (short) urlb.length;
			buf.writeByte(ulength);
			ulength >>>= 8;
			buf.writeByte(ulength);
			buf.writeBytes(urlb);
			connection.sendRawPacket(buf);
			return;
		}
		sha1 = HexUtils.fromBytesToHexBytes(sha1);
		packetsize+=43;
		int i = urlb.length;
		boolean shortlength = (i & -128) != 0, hasmessageforced = version > 0x2f2;
		if(shortlength) {
			++packetsize;
		}
		if(hasmessageforced) {
			packetsize+=2;
		}
		ByteBuf buf = Unpooled.buffer(packetsize, packetsize);
		buf.writeByte(pid);
		if(shortlength) {
			buf.writeByte(i & 127 | 128);
			i >>>= 7;
		}
		buf.writeByte(i);
		buf.writeBytes(urlb);
		buf.writeByte(40);
		buf.writeBytes(sha1);
		if(hasmessageforced) {
			buf.writeByte(0x00);
			buf.writeByte(0x00);
		}
		connection.sendRawPacket(buf);
	}
}
