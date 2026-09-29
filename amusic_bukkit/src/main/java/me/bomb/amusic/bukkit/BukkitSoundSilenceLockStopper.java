package me.bomb.amusic.bukkit;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import me.bomb.amusic.api.SoundStopper;

public final class BukkitSoundSilenceLockStopper implements SoundStopper {
	
	private final Server server;
	
	protected BukkitSoundSilenceLockStopper(Server server) {
		this.server = server;
	}
	
	public void stopSound(UUID uuid, UUID soundhash, short id, byte part) {
		if(uuid == null) {
			return;
		}
		Player player = server.getPlayer(uuid);
		Location location = player.getLocation();
		final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);
		new Runnable() {
			private final static String silence = "minecraft:amusic.internal.silence";
			private byte remaining = 5;
			@Override
			public void run() {
				player.playSound(location, silence, 1.0E9f, 1.0f);
				if(!player.isOnline() || --this.remaining < 1) {
					scheduler.shutdown();
					return;
				}
				scheduler.schedule(this, 50L, TimeUnit.MILLISECONDS);
			}
		}.run();
	}

}
