package me.bomb.amusic;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class Repeater {
	private final static Map<UUID,Repeater> repeaters = new HashMap<UUID,Repeater>();
	private final boolean repeat;
	private final boolean one;
	private Repeater(boolean repeat,boolean one) {
		this.repeat = repeat;
		this.one = one;
	}
	protected void next(UUID uuid ,byte curid) {
		if(!repeaters.containsKey(uuid)) {
			return;
		}
		Repeater repeater = repeaters.get(uuid);
		if(repeater.repeat&&repeater.one) {
			PositionTracker.playMusic(uuid, curid, false);
			return;
		}
		if(repeater.repeat&&!repeater.one) {
			PositionTracker.playMusic(uuid, ++curid, true);
			return;
		}
		if(!repeater.repeat&&!repeater.one) {
			PositionTracker.playMusic(uuid, ++curid, false);
			return;
		}
	}
	protected static Repeater getRepeater(UUID uuid) {
		return repeaters.containsKey(uuid)?repeaters.get(uuid):null;
	}
	protected static void setRepeater(UUID uuid,boolean repeat,boolean one) {
		repeaters.put(uuid, new Repeater(repeat, one));
	}
	protected static void removeRepeater(UUID uuid) {
		repeaters.remove(uuid);
	}
	protected static void clear() {
		repeaters.clear();
	}
}
