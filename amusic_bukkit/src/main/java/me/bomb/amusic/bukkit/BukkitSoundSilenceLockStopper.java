package me.bomb.amusic.bukkit;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.bomb.amusic.SoundStopper;

public final class BukkitSoundSilenceLockStopper implements SoundStopper {
	
	
	protected BukkitSoundSilenceLockStopper() {
	}
	
	public void stopSound(UUID uuid, short id) {
		if(uuid == null) {
			return;
		}
		Player player = Bukkit.getPlayer(uuid);
		Location location = player.getLocation();
		byte i = 5;
		while(--i > -1) {
			player.playSound(location, "amusic.silence", 1.0E9f, 1.0f);
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
			}
		}
	}
	
	@Override
	public boolean isStopAll() {
		return true;
	}
	
	@Override
	public boolean isLock() {
		return true;
	}

}
