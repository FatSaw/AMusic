package me.bomb.amusic.velocity;

import java.util.Arrays;
import java.util.List;

import dev.simplix.protocolize.api.PacketDirection;
import dev.simplix.protocolize.api.mapping.AbstractProtocolMapping;
import dev.simplix.protocolize.api.mapping.ProtocolIdMapping;
import dev.simplix.protocolize.api.packet.AbstractPacket;
import io.netty.buffer.ByteBuf;

import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_13;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_13_2;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_14;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_14_4;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_15;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_15_2;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_16;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_16_5;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_17;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_17_1;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_18;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_19_1;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_19_2;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_19_3;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_19_4;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_20_1;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_20_2;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_20_3;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_20_5;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_20_6;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_21_1;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_21_2;

import static dev.simplix.protocolize.api.util.ProtocolUtil.readString;
import static dev.simplix.protocolize.api.util.ProtocolUtil.readVarInt;
import static dev.simplix.protocolize.api.util.ProtocolUtil.writeVarInt;
import static dev.simplix.protocolize.api.util.ProtocolUtil.writeString;

public final class SoundStopPacket extends AbstractPacket {
	
	private final static int MINECRAFT_1_21_4 = 769;

	public static final List<ProtocolIdMapping> MAPPINGS = Arrays.asList(
			AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_13, MINECRAFT_1_13_2, 0x4C),
			AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_14, MINECRAFT_1_14_4, 0x52),
			AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_15, MINECRAFT_1_15_2, 0x53),
			AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_16, MINECRAFT_1_16_5, 0x52),
			AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_17, MINECRAFT_1_17_1, 0x5D),
			AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_18, MINECRAFT_1_19_1, 0x5E),
			AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_19_2, MINECRAFT_1_19_2, 0x61),
			AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x5F),
			AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_19_4, MINECRAFT_1_20_1, 0x63),
			AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_20_2, MINECRAFT_1_20_2, 0x66),
			AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_20_3, MINECRAFT_1_20_5, 0x68),
			AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_20_6, MINECRAFT_1_21_1, 0x6A),
			AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x71)
	);
	
	protected byte flags;
	protected int category;
	protected String sound;
	
	public SoundStopPacket() {
	}
	
	public SoundStopPacket(int category, String sound) {
		flags = (byte) (sound == null ? 0x01 : 0x02); 
		this.sound = sound;
	}

	@Override
	public void read(ByteBuf buf, PacketDirection direction, int protocolVersion) {
		flags = buf.readByte();
		if((flags & 0x01) == 0x01) {
			category = readVarInt(buf);
		}
		if((flags & 0x02) == 0x02) {
			sound = readString(buf);
		}
	}

	@Override
	public void write(ByteBuf buf, PacketDirection direction, int protocolVersion) {
		buf.writeByte(flags);
		if((flags & 0x01) == 0x01) {
			writeVarInt(buf, category);
		}
		if((flags & 0x02) == 0x02) {
			writeString(buf, sound);
		}
	}

}
