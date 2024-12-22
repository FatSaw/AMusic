package me.bomb.amusic.velocity;

import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import com.google.common.io.ByteArrayDataOutput;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.messages.PluginMessageEncoder;

import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import me.bomb.amusic.SoundStopper;

public final class ProtocoliseSoundStopper implements SoundStopper {
	
	private final ProxyServer server;
	private final ChannelIdentifier identifier;
	
	protected ProtocoliseSoundStopper(ProxyServer server) {
		this.server = server;
		this.identifier = new LegacyChannelIdentifier("MC|StopSound");
	}

	@Override
	public void stopSound(UUID uuid, short id) {
		ProtocolizePlayer player = Protocolize.playerProvider().player(uuid);
		if(player==null) {
			return;
		}
		final int version = player.protocolVersion();
		if(version < 110) {
			//There is no sound stop packet below protocol version 110, it still can be stopped by world rejoin or more than 4 sounds playing on the same time.
			//Do nothing since proxy server don't have world instance.
			//TODO: Research how sound can be stopped without minimal side effects (maybe 4+ zero volume sounds or 4+ silence sounds)
			return;
		}
		final String soundid = "amusic.music".concat(Short.toString(id));
		if(version > 388) {
			SoundStopPacket packet = new SoundStopPacket(soundid);
			player.sendPacket(packet);
			return;
		}
		Optional<Player> oplayer = server.getPlayer(uuid);
		if(oplayer.isEmpty()) {
			return;
		}
		oplayer.get().sendPluginMessage(identifier, new StopSoundMessageEncoder(soundid));
	}
	
	public final static class StopSoundMessageEncoder implements PluginMessageEncoder {
		private final String soundid;
		protected StopSoundMessageEncoder(String soundid) {
			this.soundid = soundid;
		}
		@Override
		public void encode(@NotNull ByteArrayDataOutput output) {
			output.writeUTF(soundid);
		}
	}

}
