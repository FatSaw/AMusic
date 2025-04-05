package me.bomb.amusic.util;

import static java.nio.charset.StandardCharsets.US_ASCII;

public final class HexUtils {
	
	private static final byte[] HEX = "0123456789ABCDEF".getBytes(US_ASCII);
	
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
