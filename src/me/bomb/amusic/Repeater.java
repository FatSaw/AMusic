package me.bomb.amusic;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

final class Repeater {
	private final static HashMap<UUID,Repeater> repeaters = new HashMap<UUID,Repeater>();
	private final RepeatType repeattype;
	private Repeater(RepeatType repeattype) {
		this.repeattype = repeattype;
	}
	protected static void next(UUID uuid ,byte curid) {
		if(!repeaters.containsKey(uuid)) {
			return;
		}
		Repeater repeater = repeaters.get(uuid);
		switch(repeater.repeattype) {
		case PLAYALL: 
			++curid;
		break;
		case REPEATALL:
			if(curid<ResourcePacked.getPackInfo(uuid).songs.size()) {
				++curid;
			} else {
				curid = 0;
			}
		break;
		case RANDOM:
			curid = (byte) ThreadLocalRandom.current().nextInt(0,ResourcePacked.getPackInfo(uuid).songs.size()-1);
		break;
		case REPEATONE:
		break;
		}
		PositionTracker.playMusic(uuid, curid);
	}
	protected static void setRepeater(UUID uuid,RepeatType repeattype) {
		if(repeattype==null) {
			repeaters.remove(uuid);
			return;
		}
		repeaters.put(uuid, new Repeater(repeattype));
	}
	protected static void clear() {
		repeaters.clear();
	}
}
