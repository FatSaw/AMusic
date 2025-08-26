package me.bomb.amusic.util;

import static java.nio.charset.StandardCharsets.US_ASCII;

public final class HexUtils {
	
	private static final byte[] HEX = "0123456789abcdef".getBytes(US_ASCII);
	
	private HexUtils() {
	}
	
	/**
     * Converts a byte array to a hexadecimal string.
     *
     * @param bytes a byte array
     * @return {@code bytes} as a hexadecimal string
     */
	public static String fromBytesToHex(byte[] bytes) {
        int i = bytes.length;
        int j = i << 1;
        byte[] hexChars = new byte[j];
        while (--i > -1) {
            int v = bytes[i] & 0xFF;
            hexChars[--j] = HEX[v & 0x0F];
            hexChars[--j] = HEX[v >>> 4];
        }
        return new String(hexChars, US_ASCII);
    }
	
	public static String byteToHex(byte abyte) {
        byte[] hexChars = new byte[2];
        hexChars[1] = HEX[abyte & 0x0F];
        abyte >>>= 4;
        hexChars[0] = HEX[abyte & 0x0F];
        return new String(hexChars, US_ASCII);
	}
	public static String shortToHex(short ashort) {
        byte[] hexChars = new byte[4];
        hexChars[3] = HEX[ashort & 0x0F];
        ashort >>>= 4;
        hexChars[2] = HEX[ashort & 0x0F];
        ashort >>>= 4;
        hexChars[1] = HEX[ashort & 0x0F];
        ashort >>>= 4;
        hexChars[0] = HEX[ashort & 0x0F];
        return new String(hexChars, US_ASCII);
	}
	public static String intToHex(int aint) {
        byte[] hexChars = new byte[8];
        hexChars[7] = HEX[aint & 0x0F];
        aint >>>= 4;
        hexChars[6] = HEX[aint & 0x0F];
        aint >>>= 4;
        hexChars[5] = HEX[aint & 0x0F];
        aint >>>= 4;
        hexChars[4] = HEX[aint & 0x0F];
        aint >>>= 4;
        hexChars[3] = HEX[aint & 0x0F];
        aint >>>= 4;
        hexChars[2] = HEX[aint & 0x0F];
        aint >>>= 4;
        hexChars[1] = HEX[aint & 0x0F];
        aint >>>= 4;
        hexChars[0] = HEX[aint & 0x0F];
        return new String(hexChars, US_ASCII);
	}
	public static String longToHex(long along) {
        byte[] hexChars = new byte[16];
        hexChars[15] = HEX[(int) (along & 0x0F)];
        along >>>= 4;
        hexChars[14] = HEX[(int) (along & 0x0F)];
        along >>>= 4;
        hexChars[13] = HEX[(int) (along & 0x0F)];
        along >>>= 4;
        hexChars[12] = HEX[(int) (along & 0x0F)];
        along >>>= 4;
        hexChars[11] = HEX[(int) (along & 0x0F)];
        along >>>= 4;
        hexChars[10] = HEX[(int) (along & 0x0F)];
        along >>>= 4;
        hexChars[9] = HEX[(int) (along & 0x0F)];
        along >>>= 4;
        hexChars[8] = HEX[(int) (along & 0x0F)];
        along >>>= 4;
        hexChars[7] = HEX[(int) (along & 0x0F)];
        along >>>= 4;
        hexChars[6] = HEX[(int) (along & 0x0F)];
        along >>>= 4;
        hexChars[5] = HEX[(int) (along & 0x0F)];
        along >>>= 4;
        hexChars[4] = HEX[(int) (along & 0x0F)];
        along >>>= 4;
        hexChars[3] = HEX[(int) (along & 0x0F)];
        along >>>= 4;
        hexChars[2] = HEX[(int) (along & 0x0F)];
        along >>>= 4;
        hexChars[1] = HEX[(int) (along & 0x0F)];
        along >>>= 4;
        hexChars[0] = HEX[(int) along & 0x0F];
        return new String(hexChars, US_ASCII);
	}
	
	public static String getHex(byte abyte) {
        return new String(new byte[] {HEX[abyte & 0x0F]}, US_ASCII);
	}
	
	/**
     * Converts a hexadecimal string to a byte array.
     *
     * @param hex a string of hexadecimal digits
     * @return {@code hex} as a byte array or null if contains not hex character
     */
	public static byte[] fromHexToBytes(String hex) {
		char[] hexc = hex.toCharArray();
		int i = hexc.length;
		int j = i >> 1;
		if(((byte)i & 0x01) == 0x01) {
			++j;
		}
		byte[] bytes = new byte[j];
		while (--j > -1) {
			byte v1 = hexCharToByte(hexc[--i]);
			byte v2;
			try {
				v2 = hexCharToByte(hexc[--i]);
			} catch (IndexOutOfBoundsException e) {
				v2 = 0;
			}
			if(v1 == -1 || v2 == -1) {
				return null;
			}
			v1 |= v2 << 4;
			bytes[j] = v1;
		}
		return bytes;
	}
	
	private static byte hexCharToByte(int c) {
		if((0xFFFFFFD8 & c) == 0x40) {
			c &= 0x07;
			--c;
			if(((0x06 & c) == 0x06)) {
				return -1;
			} else {
				return (byte) (c + 0x0A);
			}
		} else if((0xFFFFFFF8 & c) == 0x30 || (0xFFFFFFFE & c) == 0x38) {
			return (byte) (c & 0x0F);
		} else {
			return -1;
		}
	}

}
