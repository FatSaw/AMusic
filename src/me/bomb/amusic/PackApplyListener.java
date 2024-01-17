package me.bomb.amusic;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

abstract class PackApplyListener {
	
	private static final PackApplyListener packsender;
	
	protected final ConcurrentHashMap<UUID, AtomicBoolean[]> applylisteners = new ConcurrentHashMap<UUID, AtomicBoolean[]>(16,0.75f,1);

	static {
		if(ConfigOptions.packapplystatus) {
			String nmsversion = Bukkit.getServer().getClass().getPackage().getName().substring(23);
			switch (nmsversion) {
			case "v1_7_R4":
				PackApplyListener apacksender = null;
				try {
					apacksender = new PackApplyListener_1_7_R4();
				} catch (NoSuchFieldException | SecurityException e) {
				}
				packsender = apacksender;
				break;
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
	
	protected static void registerApplyListenTask(Player player) {
		if(packsender==null) {
			return;
		}
		packsender.addApplyListenTask(player);
	}
	
	protected static boolean applied(UUID playeruuid) {
		AtomicBoolean[] ab;
		if(playeruuid!=null && packsender!=null && (ab = packsender.applylisteners.get(playeruuid)) != null) {
			boolean ret = ab[1].get();
			ret&=!ab[0].getAndSet(ret);
			return ret;
		}
		return false;
	}
	
	protected static void reset(UUID playeruuid) {
		if(playeruuid==null||packsender==null) {
			return;
		}
		AtomicBoolean[] ab;
		if((ab = packsender.applylisteners.get(playeruuid)) == null) {
			return;
		}
		ab[1].set(false);
	}
	
	protected static void unregisterApplyListenTask(Player player) {
		if(packsender==null) {
			return;
		}
		packsender.removeApplyListenTask(player);
	}
	
	protected abstract void addApplyListenTask(Player player);
	protected abstract void removeApplyListenTask(Player player);
	
}
