package me.bomb.amusic.velocity.event;

import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.GeyserHook;

public final class ProxyShutdownHandler implements EventHandler<ProxyShutdownEvent> {

	private final AMusic amusic;
	private final GeyserHook geyser;
	
	public ProxyShutdownHandler(AMusic amusic, GeyserHook geyser) {
		this.amusic = amusic;
		this.geyser = geyser;
	}
	
	@Override
	public void execute(ProxyShutdownEvent event) {
		if(this.geyser != null) {
			this.geyser.unregister();
		}
		this.amusic.disable();
	}
	
}
