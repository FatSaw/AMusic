package me.bomb.amusic.bukkit.event;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

import me.bomb.amusic.resourceserver.ResourceManager;

public final class PlayerResourcePackStatusHandler extends RegisteredListener {

	private final HandlerList handlerlist;
	private final ResourceManager resourcemanager;

	public PlayerResourcePackStatusHandler(Plugin plugin, ResourceManager resourcemanager) throws NoClassDefFoundError {
		super(null, null, null, plugin, true);
		this.resourcemanager = resourcemanager;
		this.handlerlist = PlayerResourcePackStatusEvent.getHandlerList();
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
		final PlayerResourcePackStatusEvent event = (PlayerResourcePackStatusEvent) eve;
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		Status status = event.getStatus();
		if(status==Status.ACCEPTED) {
			resourcemanager.setAccepted(uuid);
			return;
		}
		if(status==Status.DECLINED||status==Status.FAILED_DOWNLOAD) {
			resourcemanager.remove(uuid);
			return;
		}
		if(status==Status.SUCCESSFULLY_LOADED) {
			resourcemanager.remove(uuid); //Removes resource send if pack applied from client cache
		}
	}
	
	@Override
	public boolean isIgnoringCancelled() {
		return true;
	}
}
