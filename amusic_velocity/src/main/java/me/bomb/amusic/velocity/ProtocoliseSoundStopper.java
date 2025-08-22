package me.bomb.amusic.velocity;

import java.util.Optional;
import java.util.UUID;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;

import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import me.bomb.amusic.SoundStopper;

public final class ProtocoliseSoundStopper implements SoundStopper {
	
	private final ProxyServer server;
	private final ChannelIdentifier identifier;
	private final NamedSoundEffectPacket legacystopsound;
	private final boolean legacysupport;
	
	protected ProtocoliseSoundStopper(ProxyServer server, boolean legacysupport) {
		this.server = server;
		this.identifier = new LegacyChannelIdentifier("MC|StopSound");
		this.legacystopsound = legacysupport ? new NamedSoundEffectPacket("amusic.silence", 0, 0, Integer.MIN_VALUE, 0, 1.0E9f, 1.0f) : null;
		this.legacysupport = legacysupport;
	}

	@Override
	public void stopSound(UUID uuid, short id, byte partid) {
		ProtocolizePlayer player = Protocolize.playerProvider().player(uuid);
		if(player==null) {
			return;
		}
		final int version = player.protocolVersion();
		if(legacysupport && version < 110) {
			//There is no sound stop packet below protocol version 110, it still can be stopped by world rejoin or more than 4 sounds playing on the same time.
			//4 silence sounds should be processed by client in different ticks
			byte i = 5;
			while(--i > -1) {
				player.sendPacket(legacystopsound);
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
				}
			}
			return;
		}
		final String soundid = "amusic.music".concat(Short.toString(id));
		if(version > 388) {
			SoundStopPacket packet = new SoundStopPacket(9, soundid);
			player.sendPacket(packet);
			return;
		}
		Optional<Player> oplayer = server.getPlayer(uuid);
		if(oplayer.isEmpty()) {
			return;
		}
		oplayer.get().sendPluginMessage(identifier, new StringPluginMessageEncoder(soundid));
	}
	
	@Override
	public boolean isLock() {
		return legacysupport;
	}

}
