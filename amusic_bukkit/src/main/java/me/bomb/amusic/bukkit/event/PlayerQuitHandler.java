package me.bomb.amusic.bukkit.event;

import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

import me.bomb.amusic.LocalAMusic;
import me.bomb.amusic.bukkit.command.UploadmusicCommand;

public class PlayerQuitHandler extends RegisteredListener {
	
	private final HandlerList handlerlist;
	private final LocalAMusic amusic;
	private final ConcurrentHashMap<Object,InetAddress> playerips;
	private final UploadmusicCommand uploadmusiccmd;

	public PlayerQuitHandler(Plugin plugin, LocalAMusic amusic, ConcurrentHashMap<Object,InetAddress> playerips, UploadmusicCommand uploadmusiccmd) throws NoClassDefFoundError {
		super(null, null, null, plugin, true);
		this.amusic = amusic;
		this.playerips = playerips;
		this.uploadmusiccmd = uploadmusiccmd;
		this.handlerlist = PlayerQuitEvent.getHandlerList();
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
		PlayerQuitEvent event = (PlayerQuitEvent) eve;
		Player player = event.getPlayer();
		UUID playeruuid = player.getUniqueId();
		amusic.logout(playeruuid);
		if(uploadmusiccmd != null) uploadmusiccmd.logoutUploader(player);
		if(this.playerips != null) this.playerips.remove(playeruuid);
	}
	
	@Override
	public boolean isIgnoringCancelled() {
		return true;
	}
}
