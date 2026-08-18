package me.bomb.amusic.velocity.event;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.GeyserHook;

public final class ProxyShutdownHandler implements EventHandler<ProxyShutdownEvent> {

	private final AMusic amusic;
	private final GeyserHook geyser;
	private final CommandManager cmdmanager;
	private final CommandMeta loadmusicmeta, playmusicmeta, repeatmeta;
	private final EventManager eventmanager;
	private final Object plugin;
	private final LoginHandler login;
	private final DisconnectHandler disconnect;
	private final PlayerResourcePackStatusHandler resourcepackstatus;
	
	public ProxyShutdownHandler(AMusic amusic, GeyserHook geyser, CommandManager cmdmanager, CommandMeta loadmusicmeta, CommandMeta playmusicmeta, CommandMeta repeatmeta, EventManager eventmanager, Object plugin, LoginHandler login, DisconnectHandler disconnect, PlayerResourcePackStatusHandler resourcepackstatus) {
		this.amusic = amusic;
		this.geyser = geyser;
		this.cmdmanager = cmdmanager;
		this.loadmusicmeta = loadmusicmeta;
		this.playmusicmeta = playmusicmeta;
		this.repeatmeta = repeatmeta;
		this.eventmanager = eventmanager;
		this.plugin = plugin;
		this.login = login;
		this.disconnect = disconnect;
		this.resourcepackstatus = resourcepackstatus;
	}
	
	@Override
	public void execute(ProxyShutdownEvent event) {
		if(this.geyser != null) {
			this.geyser.unregister();
		}
		if(this.cmdmanager != null) {
			if(this.loadmusicmeta != null) this.cmdmanager.unregister(this.loadmusicmeta);
			if(this.playmusicmeta != null) this.cmdmanager.unregister(this.playmusicmeta);
			if(this.repeatmeta != null) this.cmdmanager.unregister(this.repeatmeta);
		}
		if(this.login != null) this.eventmanager.unregisterListener(this.plugin, this.login);
		if(this.disconnect != null) this.eventmanager.unregisterListener(this.plugin, this.disconnect);
		if(this.resourcepackstatus != null) this.eventmanager.unregisterListener(this.plugin, this.resourcepackstatus);
		this.eventmanager.unregisterListener(this.plugin, event);
		this.amusic.disable();
	}
	
}
