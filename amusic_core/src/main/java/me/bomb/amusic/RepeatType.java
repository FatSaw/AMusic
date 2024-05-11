package me.bomb.amusic;

import java.util.concurrent.ThreadLocalRandom;

public enum RepeatType {
	REPEATONE {
		@Override
		public short next(short curid,short maxid) {
			return curid;
		}
	}, REPEATALL {
		@Override
		public short next(short curid,short maxid) {
			if (++curid >= maxid) {
				curid = 0;
			}
			return curid;
		}
	}, PLAYALL {
		@Override
		public short next(short curid,short maxid) {
			return ++curid;
		}
	}, RANDOM {
		@Override
		public short next(short curid,short maxid) {
			return (short) ThreadLocalRandom.current().nextInt(maxid);
		}
	};
	public abstract short next(short curid,short maxid);
}
