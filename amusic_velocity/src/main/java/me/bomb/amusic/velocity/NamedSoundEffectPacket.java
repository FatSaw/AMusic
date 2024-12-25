package me.bomb.amusic.velocity;

import java.util.Arrays;
import java.util.List;

import dev.simplix.protocolize.api.PacketDirection;
import dev.simplix.protocolize.api.mapping.AbstractProtocolMapping;
import dev.simplix.protocolize.api.mapping.ProtocolIdMapping;
import dev.simplix.protocolize.api.packet.AbstractPacket;
import io.netty.buffer.ByteBuf;

import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_8;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_9;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_10;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_12_2;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_13;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_13_2;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_14;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_14_4;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_15;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_15_2;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_16;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_16_1;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_16_2;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_16_5;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_17;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_18_2;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_19;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_19_1;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_19_2;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_19_3;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_19_4;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_20_1;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_20_2;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_20_3;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_20_4;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_20_5;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_21_1;
import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_21_2;

import static dev.simplix.protocolize.api.util.ProtocolUtil.readString;
import static dev.simplix.protocolize.api.util.ProtocolUtil.readVarInt;
import static dev.simplix.protocolize.api.util.ProtocolUtil.writeVarInt;
import static dev.simplix.protocolize.api.util.ProtocolUtil.writeString;

public final class NamedSoundEffectPacket extends AbstractPacket {
	
	private final static int MINECRAFT_1_7_10 = 5, MINECRAFT_1_21_4 = 769;

    public static final List<ProtocolIdMapping> MAPPINGS = Arrays.asList(
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_7_10, MINECRAFT_1_8, 0x29),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_9, MINECRAFT_1_12_2, 0x19),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_13, MINECRAFT_1_13_2, 0x1A),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_14, MINECRAFT_1_14_4, 0x19),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_15, MINECRAFT_1_15_2, 0x1A),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_16, MINECRAFT_1_16_1, 0x19),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_16_2, MINECRAFT_1_16_5, 0x18),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_17, MINECRAFT_1_18_2, 0x19),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_19, MINECRAFT_1_19, 0x16),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_19_1, MINECRAFT_1_19_2, 0x17),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x5E),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_19_4, MINECRAFT_1_20_1, 0x62),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_20_2, MINECRAFT_1_20_2, 0x64),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_20_3, MINECRAFT_1_20_4, 0x66),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_20_5, MINECRAFT_1_21_1, 0x68),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x6F)
    );
    protected int soundid = 0;
    protected String sound;
    protected boolean hasfixedrange = false;
    protected float fixedrange = 0.0f;
    protected int category;
    protected double x, y, z;
    protected float volume, pitch;
    protected long seed = 0L;
    
    public NamedSoundEffectPacket() {
	}
    
    public NamedSoundEffectPacket(String sound, int category, double x, double y, double z, float volume, float pitch) {
    	this.sound = sound;
    	this.category = category;
    	this.x = x;
    	this.y = y;
    	this.z = z;
    	this.volume = volume;
    	this.pitch = pitch;
    }

    @Override
    public void read(ByteBuf buf, PacketDirection direction, int protocolVersion) {
    	if (protocolVersion > MINECRAFT_1_19_2) {
            soundid = readVarInt(buf);
        }
    	if(soundid==0 || protocolVersion < MINECRAFT_1_19_3) {
        	sound = readString(buf);
        	if (protocolVersion > MINECRAFT_1_19_2) {
        		if(hasfixedrange = buf.readBoolean()) {
            		fixedrange = buf.readFloat();
            	}
        	}
    	}
        if (protocolVersion > MINECRAFT_1_8)
            category = readVarInt(buf);
        x = buf.readInt() / 8D;
        y = buf.readInt() / 8D;
        z = buf.readInt() / 8D;
        volume = buf.readFloat();
        if (protocolVersion < MINECRAFT_1_10) {
            pitch = buf.readUnsignedByte() / 63F;
        } else {
            pitch = buf.readFloat();
        }
    }

    @Override
    public void write(ByteBuf buf, PacketDirection direction, int protocolVersion) {
    	if (protocolVersion > MINECRAFT_1_19_2) {
            writeVarInt(buf, soundid);
        }
    	if(soundid==0 || protocolVersion < MINECRAFT_1_19_3) {
        	writeString(buf, sound);
        	if (protocolVersion > MINECRAFT_1_19_2) {
        		buf.writeBoolean(hasfixedrange);
            	if(hasfixedrange) {
                	buf.writeFloat(fixedrange);
            	}
        	}
    	}
        if (protocolVersion > MINECRAFT_1_8)
            writeVarInt(buf, category);
        buf.writeInt((int) (x * 8));
        buf.writeInt((int) (y * 8));
        buf.writeInt((int) (z * 8));
        buf.writeFloat(volume);
        if (protocolVersion < MINECRAFT_1_10) {
            buf.writeByte((byte) (pitch * 63) & 0xFF);
        } else {
            buf.writeFloat(pitch);
        }
        if (protocolVersion >= MINECRAFT_1_19) {
            buf.writeLong(seed);
        }
    }

}
