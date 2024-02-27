/**
 * Utility to listen unfreeze moment after resourcepack apply.
 * Solves problem that Resource Pack Status SUCCESSFULLY_LOADED packet sends a bit before fully applied.
 * It listens movement packets which normally sends every second even if no movement,
 * during resourcepack apply movement packets not sends, thats give ability to detect full apply.
 * Versions after 1.13.2 cannot be checked by this utility because no freeze during resourcepack apply.
 */
package me.bomb.amusic.bukkit.moveapplylistener;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.bomb.amusic.bukkit.ConfigOptions;
import me.bomb.amusic.bukkit.moveapplylistener.PackApplyListener;

public abstract class PackApplyListener {
	
	private static final PackApplyListener packsender;
	
	protected final ConcurrentHashMap<UUID, AtomicBoolean[]> applylisteners = new ConcurrentHashMap<UUID, AtomicBoolean[]>(16,0.75f,1);

	static {
		if(ConfigOptions.packapplystatus) {
			String nmsversion = Bukkit.getServer().getClass().getPackage().getName().substring(23);
			switch (nmsversion) {
			case "v1_8_R3":
				packsender = new PackApplyListener_1_8_R3();
				break;
			case "v1_9_R2":
				packsender = new PackApplyListener_1_9_R2();
				break;
			case "v1_10_R1":
				packsender = new PackApplyListener_1_10_R1();
				break;
			case "v1_11_R1":
				packsender = new PackApplyListener_1_11_R1();
				break;
			case "v1_12_R1":
				packsender = new PackApplyListener_1_12_R1();
				break;
			case "v1_13_R2":
				packsender = new PackApplyListener_1_13_R2();
				break;
			default:
				packsender = null;
			}
		} else {
			packsender = null;
		}
	}
	
	public static void registerApplyListenTask(Player player) {
		if(packsender==null) {
			return;
		}
		packsender.addApplyListenTask(player);
	}
	
	public static boolean applied(UUID playeruuid) {
		AtomicBoolean[] ab;
		if(playeruuid!=null && packsender!=null && (ab = packsender.applylisteners.get(playeruuid)) != null) {
			boolean ret = ab[1].get();
			ret&=!ab[0].getAndSet(ret);
			return ret;
		}
		return false;
	}
	
	public static void reset(UUID playeruuid) {
		if(playeruuid==null||packsender==null) {
			return;
		}
		AtomicBoolean[] ab;
		if((ab = packsender.applylisteners.get(playeruuid)) == null) {
			return;
		}
		ab[0].set(false);
		ab[1].set(false);
	}
	
	public static void unregisterApplyListenTask(Player player) {
		if(packsender==null) {
			return;
		}
		packsender.removeApplyListenTask(player);
	}
	
	protected abstract void addApplyListenTask(Player player);
	protected abstract void removeApplyListenTask(Player player);
	
}
