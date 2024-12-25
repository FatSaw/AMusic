package me.bomb.amusic.velocity;

import org.jetbrains.annotations.NotNull;

import com.google.common.io.ByteArrayDataOutput;
import com.velocitypowered.api.proxy.messages.PluginMessageEncoder;

public final class StringPluginMessageEncoder implements PluginMessageEncoder {
	private final String soundid;
	protected StringPluginMessageEncoder(String soundid) {
		this.soundid = soundid;
	}
	@Override
	public void encode(@NotNull ByteArrayDataOutput output) {
		output.writeUTF(soundid);
	}
}
