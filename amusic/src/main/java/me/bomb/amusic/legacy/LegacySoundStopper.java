package me.bomb.amusic.legacy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public abstract class LegacySoundStopper {
	
private static final LegacySoundStopper soundstopper;
	
	static {
		String nmsversion = Bukkit.getServer().getClass().getPackage().getName().substring(23);
		switch (nmsversion) {
		/*case "v1_7_R4":
			soundstopper = new LegacySoundStopper_1_7_R4();
		break;*/
		case "v1_8_R3":
			soundstopper = new LegacySoundStopper_1_8_R3();
		break;
		default:
			soundstopper = null;
		}
	}
	
	public static void stopSounds(Player player) {
		soundstopper.stopSound(player);
	}
	
	protected abstract void stopSound(Player player);
}
