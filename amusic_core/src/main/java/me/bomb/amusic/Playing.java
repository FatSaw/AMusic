package me.bomb.amusic;

public final class Playing {
	protected final short currenttrack, maxid;
	protected short remaining, remainingf;

	protected Playing(short currenttrack, short maxid, short remaining, boolean processformatedtime) {
		this.currenttrack = currenttrack;
		this.maxid = maxid;
		this.remaining = remaining;
		this.remainingf = processformatedtime ? sectotime(remaining) : 0;
	}
	
	protected short sectotime(short sec) {
		if (sec < 1)
			return 0;
		if (sec > 0x7080) {
			return 0x7EFB;
		}
		short res = 0;
		byte min = 0, hour = 0;
		while ((res += 60) < sec) {
			if (++min < 60) {
				continue;
			}
			min = 0;
			++hour;
		}
		res -= 59;
		sec -= res;
		sec |= ((short) min << 6);
		sec |= ((short) hour << 6);
		return sec;
	}
}
