package me.bomb.amusic.sponge;

import java.util.UUID;

import org.spongepowered.api.effect.sound.SoundType;

import me.bomb.amusic.util.HexUtils;

public final class AMusicSoundType implements SoundType {
	
	private final String soundid;
	
	public AMusicSoundType(UUID uuid, UUID soundhash, short id, byte part) {
		this.soundid = new StringBuilder("minecraft:amusic.internal.").append(soundhash.toString()).append(HexUtils.shortToHex(id)).append(HexUtils.byteToHex(part)).toString();
	}

	@Override
	public String getId() {
		return this.soundid;
	}

	@Override
	public String getName() {
		return this.soundid;
	}

}
