package me.bomb.amusic.source;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class SoundSource {

	public abstract SourceEntry get(String entrykey);
	
	protected static short calculateDuration(byte[] t) {
		int rate = -1, length = -1, size = t.length;
		for (int i = size - 15; i >= 0 && length < 0; i--) {
			if (t[i] == (byte) 'O' && t[i + 1] == (byte) 'g' && t[i + 2] == (byte) 'g' && t[i + 3] == (byte) 'S') {
				byte[] byteArray = new byte[] { t[i + 6], t[i + 7], t[i + 8], t[i + 9], t[i + 10], t[i + 11], t[i + 12], t[i + 13] };
				ByteBuffer bb = ByteBuffer.wrap(byteArray);
				bb.order(ByteOrder.LITTLE_ENDIAN);
				length = bb.getInt(0);
			}
		}
		for (int i = 0; i < size - 14 && rate < 0; i++) {
			if (t[i] == (byte) 'v' && t[i + 1] == (byte) 'o' && t[i + 2] == (byte) 'r' && t[i + 3] == (byte) 'b' && t[i + 4] == (byte) 'i' && t[i + 5] == (byte) 's') {
				byte[] byteArray = new byte[] { t[i + 11], t[i + 12], t[i + 13], t[i + 14] };
				ByteBuffer bb = ByteBuffer.wrap(byteArray);
				bb.order(ByteOrder.LITTLE_ENDIAN);
				rate = bb.getInt(0);
			}
		}
		int res = length / rate;
		return res > Short.MAX_VALUE ? Short.MAX_VALUE : (short) res;
	}
	
}
