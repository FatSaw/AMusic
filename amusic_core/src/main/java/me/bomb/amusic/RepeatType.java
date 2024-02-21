package me.bomb.amusic;

import java.util.concurrent.ThreadLocalRandom;

public enum RepeatType {
	REPEATONE {
		@Override
		public byte next(byte curid,byte maxid) {
			return curid;
		}
	}, REPEATALL {
		@Override
		public byte next(byte curid,byte maxid) {
			if (++curid >= maxid) {
				curid = 0;
			}
			return curid;
		}
	}, PLAYALL {
		@Override
		public byte next(byte curid,byte maxid) {
			return ++curid;
		}
	}, RANDOM {
		@Override
		public byte next(byte curid,byte maxid) {
			return (byte) ThreadLocalRandom.current().nextInt(maxid);
		}
	};
	public abstract byte next(byte curid,byte maxid);
}
