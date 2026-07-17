package me.bomb.amusic.bukkit.event;

import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

import me.bomb.amusic.PositionTracker;

public final class PlayerRespawnHandler extends RegisteredListener {
	
	private final HandlerList handlerlist;
	private final PositionTracker positiontracker;

	public PlayerRespawnHandler(Plugin plugin, PositionTracker positiontracker) throws NoClassDefFoundError {
		super(null, null, null, plugin, true);
		this.positiontracker = positiontracker;
		this.handlerlist = PlayerRespawnEvent.getHandlerList();
	}
	
	public void register() {
		this.handlerlist.register(this);
	}
	
	public void unregister() {
		this.handlerlist.unregister(this);
	}
	
	@Override
	public Listener getListener() {
		return null;
	}
	
	@Override
	public Plugin getPlugin() {
		return super.getPlugin();
	}
	
	@Override
	public EventPriority getPriority() {
		return EventPriority.LOWEST;
	}
	
	@Override
	public void callEvent(final Event eve) throws EventException {
		PlayerRespawnEvent event = (PlayerRespawnEvent) eve;
		positiontracker.stopMusic(event.getPlayer().getUniqueId());
	}
	
	@Override
	public boolean isIgnoringCancelled() {
		return true;
	}
}
