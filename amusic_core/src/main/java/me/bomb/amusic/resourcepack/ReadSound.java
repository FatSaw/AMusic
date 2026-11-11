package me.bomb.amusic.resourcepack;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.spi.FileSystemProvider;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.UUID;
import java.util.zip.CRC32;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class ReadSound implements Runnable {
	
	private final static HashSet<OpenOption> READ;
	private final static byte[] JAVA_PATH = "assets/minecraft/sounds/amusic/".getBytes(US_ASCII), BEDROCK_PATH = "sounds/amusic/".getBytes(US_ASCII), OGG = ".ogg".getBytes(US_ASCII), HEX = "0123456789abcdef".getBytes(US_ASCII);
	private final static int ZIP_LOCAL_HEADER = 0x04034b50, ZIP_GLOBAL_HEADER = 0x02014b50;
	private final static short ZIP_VERSION = 0x0014;
	private final static byte[] SOUNDS_JAVA_ENTRY, SOUNDS_NAME_JAVA_ENTRY_1, SOUNDS_NAME_JAVA_ENTRY_2, SOUNDS_BEDROCK_ENTRY, SOUNDS_NAME_BEDROCK_ENTRY_1, SOUNDS_NAME_BEDROCK_ENTRY_2;
	
	
	private final static int SOUNDS_JAVA_ENTRY_SOUNDID_OFFSET_1, SOUNDS_JAVA_ENTRY_SOUNDID_OFFSET_2, SOUNDS_JAVA_NAMED_ENTRY_SOUNDID_OFFSET, SOUNDS_BEDROCK_ENTRY_SOUNDID_OFFSET_1, SOUNDS_BEDROCK_ENTRY_SOUNDID_OFFSET_2, SOUNDS_BEDROCK_NAMED_ENTRY_SOUNDID_OFFSET;

	public final static int SOUNDID_SIZE;
	
	static {
		READ = new HashSet<OpenOption>(1);
		READ.add(StandardOpenOption.READ);
		final String soundid = "00000000-0000-0000-0000-000000000000";
		SOUNDID_SIZE = soundid.getBytes(US_ASCII).length;
		String str = "\t\"amusic.internal.";
		SOUNDS_JAVA_ENTRY_SOUNDID_OFFSET_1 = str.getBytes(US_ASCII).length;
		str = str.concat(soundid);
		str = str.concat("\": {\n\t\t\"category\": \"master\",\n\t\t\"sounds\": [\n\t\t\t{\n\t\t\t\t\"attenuation_distance\": 2147483647,\n\t\t\t\t\"name\": \"minecraft:amusic/");
		SOUNDS_JAVA_ENTRY_SOUNDID_OFFSET_2 = str.getBytes(US_ASCII).length;
		str = str.concat(soundid);
		str = str.concat("\",\n\t\t\t\t\"stream\": true\n\t\t\t}\n\t\t]\n\t},\n");
		SOUNDS_JAVA_ENTRY = str.getBytes(US_ASCII);
		SOUNDS_NAME_JAVA_ENTRY_1 = "\t\"amusic._.".getBytes(US_ASCII);
		str = "\": {\n\t\t\"category\": \"master\",\n\t\t\"sounds\": [\n\t\t\t{\n\t\t\t\t\"attenuation_distance\": 2147483647,\n\t\t\t\t\"name\": \"minecraft:amusic/";
		SOUNDS_JAVA_NAMED_ENTRY_SOUNDID_OFFSET = SOUNDS_NAME_JAVA_ENTRY_1.length + str.getBytes(US_ASCII).length;
		str = str.concat(soundid);
		str = str.concat("\",\n\t\t\t\t\"stream\": true\n\t\t\t}\n\t\t]\n\t},\n");
		SOUNDS_NAME_JAVA_ENTRY_2 = str.getBytes(US_ASCII);
		
		str = "\t\t\"amusic.internal.";
		SOUNDS_BEDROCK_ENTRY_SOUNDID_OFFSET_1 = str.getBytes(US_ASCII).length;
		str = str.concat(soundid);
		str = str.concat("\": {\n\t\t\t\"category\": \"voice\",\n\t\t\t\"min_distance\": 3.4028235e+38,\n\t\t\t\"max_distance\": 3.4028235e+38,\n\t\t\t\"sounds\": [\n\t\t\t\t{\n\t\t\t\t\t\"name\": \"sounds/amusic/");
		SOUNDS_BEDROCK_ENTRY_SOUNDID_OFFSET_2 = str.getBytes(US_ASCII).length;
		str = str.concat(soundid);
		str = str.concat("\",\n\t\t\t\t\t\"stream\": true,\n\t\t\t\t\t\"is3D\": false\n\t\t\t\t}\n\t\t\t]\n\t\t},\n");
		SOUNDS_BEDROCK_ENTRY = str.getBytes(US_ASCII);
		SOUNDS_NAME_BEDROCK_ENTRY_1 = "\t\t\"amusic._.".getBytes(US_ASCII);
		str = "\": {\n\t\t\t\"category\": \"voice\",\n\t\t\t\"min_distance\": 3.4028235e+38,\n\t\t\t\"max_distance\": 3.4028235e+38,\n\t\t\t\"sounds\": [\n\t\t\t\t{\n\t\t\t\t\t\"name\": \"sounds/amusic/";
		SOUNDS_BEDROCK_NAMED_ENTRY_SOUNDID_OFFSET = SOUNDS_NAME_BEDROCK_ENTRY_1.length + str.getBytes(US_ASCII).length;
		str = str.concat(soundid);
		str = str.concat("\",\n\t\t\t\t\t\"stream\": true,\n\t\t\t\t\t\"is3D\": false\n\t\t\t\t}\n\t\t\t]\n\t\t},\n");
		SOUNDS_NAME_BEDROCK_ENTRY_2 = str.getBytes(US_ASCII);
		
	}
	
	
	public final static int ZIP_LOCAL_HEADER_SIZE = 30;
	public final static int ZIP_GLOBAL_HEADER_SIZE = 46;
	public final static int ZIP_GLOBAL_HEADERS_SIZE = JAVA_PATH.length + BEDROCK_PATH.length + ((ZIP_GLOBAL_HEADER_SIZE + SOUNDID_SIZE + OGG.length) << 1);
	public final static int SOUNDS_JAVA_ENTRY_SIZE = SOUNDS_JAVA_ENTRY.length;
	public final static int SOUNDS_BEDROCK_ENTRY_SIZE = SOUNDS_BEDROCK_ENTRY.length;
	public final static int SOUNDS_NAME_JAVA_ENTRY_SIZE = SOUNDS_NAME_JAVA_ENTRY_1.length + SOUNDS_NAME_JAVA_ENTRY_2.length;
	public final static int SOUNDS_NAME_BEDROCK_ENTRY_SIZE = SOUNDS_NAME_BEDROCK_ENTRY_1.length + SOUNDS_NAME_BEDROCK_ENTRY_2.length;
	
	private static byte[] fromUUIDBytesToUUIDHexBytes(byte[] bytes) {
		if(bytes == null || bytes.length != 16) {
			throw new IllegalArgumentException();
		}
		int i = 16, j = SOUNDID_SIZE;
		int v;
		byte[] hexChars = new byte[SOUNDID_SIZE];

		v = bytes[--i] & 0xFF;
		hexChars[--j] = HEX[v & 0x0F];
		hexChars[--j] = HEX[v >>> 4];
		v = bytes[--i] & 0xFF;
		hexChars[--j] = HEX[v & 0x0F];
		hexChars[--j] = HEX[v >>> 4];
		v = bytes[--i] & 0xFF;
		hexChars[--j] = HEX[v & 0x0F];
		hexChars[--j] = HEX[v >>> 4];
		v = bytes[--i] & 0xFF;
		hexChars[--j] = HEX[v & 0x0F];
		hexChars[--j] = HEX[v >>> 4];
		v = bytes[--i] & 0xFF;
		hexChars[--j] = HEX[v & 0x0F];
		hexChars[--j] = HEX[v >>> 4];
		v = bytes[--i] & 0xFF;
		hexChars[--j] = HEX[v & 0x0F];
		hexChars[--j] = HEX[v >>> 4];
		hexChars[--j] = '-';
		v = bytes[--i] & 0xFF;
		hexChars[--j] = HEX[v & 0x0F];
		hexChars[--j] = HEX[v >>> 4];
		v = bytes[--i] & 0xFF;
		hexChars[--j] = HEX[v & 0x0F];
		hexChars[--j] = HEX[v >>> 4];
		hexChars[--j] = '-';
		v = bytes[--i] & 0xFF;
		hexChars[--j] = HEX[v & 0x0F];
		hexChars[--j] = HEX[v >>> 4];
		v = bytes[--i] & 0xFF;
		hexChars[--j] = HEX[v & 0x0F];
		hexChars[--j] = HEX[v >>> 4];
		hexChars[--j] = '-';
		v = bytes[--i] & 0xFF;
		hexChars[--j] = HEX[v & 0x0F];
		hexChars[--j] = HEX[v >>> 4];
		v = bytes[--i] & 0xFF;
		hexChars[--j] = HEX[v & 0x0F];
		hexChars[--j] = HEX[v >>> 4];
		hexChars[--j] = '-';
		v = bytes[--i] & 0xFF;
		hexChars[--j] = HEX[v & 0x0F];
		hexChars[--j] = HEX[v >>> 4];
		v = bytes[--i] & 0xFF;
		hexChars[--j] = HEX[v & 0x0F];
		hexChars[--j] = HEX[v >>> 4];
		v = bytes[--i] & 0xFF;
		hexChars[--j] = HEX[v & 0x0F];
		hexChars[--j] = HEX[v >>> 4];
		v = bytes[--i] & 0xFF;
		hexChars[--j] = HEX[v & 0x0F];
		hexChars[--j] = HEX[v >>> 4];
		return hexChars;
	}
	
	public static ByteBuffer slice(ByteBuffer buffer, int offset, int length) {
		buffer = buffer.duplicate();
		buffer.position(offset);
		buffer.limit(offset + length);
		return buffer;
	}
	
	public static String toSoundName(String filename) {
		int j = filename.lastIndexOf(".");
		if (j != -1) {
			filename = filename.substring(0, j);
		}
		byte[] songnameb = filename.getBytes(UTF_8);
		j = songnameb.length;
		if(j > 0xFF) {
			j = 0xFF;
		}
		return new String(songnameb, 0, j, UTF_8);
	}
	
	/**
	 * @param str - string to validate
	 * @return ASCII byte[] if [a-z0-9_/.-]
	 */
	public static byte[] asMCKeyBytes(String str) {
		int j;
		byte[] strb = str.getBytes(US_ASCII);
		j = strb.length;
		while(--j > -1) {
			byte ch = strb[j];
			if(ch < 0x2D || ch > 0x7A || (ch < 0x5F && ch > 0x39) || ch == 0x60) break;
		}
		return j == -1 ? strb : null;
	}
	
	private final FileSystemProvider fsp;
	private final Path file;
	private final ByteBuffer localzip, datazip, globalzip, soundjsonentry, sounddefenitionsentry;
	private final CRC32 crc32;
	private final MessageDigest md5hash;
	private final byte[] soundname;
	
	protected UUID soundid;
	
	public ReadSound(FileSystemProvider fsp, Path file, ByteBuffer localzip, ByteBuffer datazip, ByteBuffer globalzip, ByteBuffer soundjsonentry, ByteBuffer sounddefenitionsentry, byte[] soundname) {
		this.fsp = fsp;
		this.file = file;
		this.localzip = localzip;
		this.datazip = datazip;
		this.globalzip = globalzip;
		this.soundjsonentry = soundjsonentry;
		this.sounddefenitionsentry = sounddefenitionsentry;
		this.soundname = soundname;
		
		this.crc32 = new CRC32();
		try {
			this.md5hash = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public void writeLocalZipHeader(int crc32, int size) {
		this.localzip.order(ByteOrder.LITTLE_ENDIAN);
		this.localzip.putInt(ZIP_LOCAL_HEADER); //HEADER
		this.localzip.putShort(ZIP_VERSION); //EXTRACT VERSION
		this.localzip.putLong(0L); //GENERAL PURPOSE BIT FLAG + COMPRESSION METHOD + TIME + DATE
		this.localzip.putInt(crc32); //CRC
		this.localzip.putInt(size); //COMPRESSED SIZE
		this.localzip.putInt(size); //UNCOMPRESSED SIZE
		this.localzip.putInt(0); //FILENAME LENGTH + EXTRA
    }
	
	public void writeGlobalZipHeader(int crc32, int size, int offset, byte[] path, byte[] filename, byte[] format) {
		short totalnamelength = (short) (path.length + filename.length + format.length);
		this.globalzip.order(ByteOrder.LITTLE_ENDIAN);
		this.globalzip.putInt(ZIP_GLOBAL_HEADER); //HEADER
		this.globalzip.putShort(ZIP_VERSION); //VERSION MADE BY
		this.globalzip.putShort(ZIP_VERSION); //VERSION EXTRACT
		this.globalzip.putLong(0L); //GENERAL PURPOSE BIT FLAG + COMPRESSION METHOD + TIME + DATE
		this.globalzip.putInt(crc32); //CRC
		this.globalzip.putInt(size); //COMPRESSED SIZE
		this.globalzip.putInt(size); //UNCOMPRESSED SIZE
		this.globalzip.putShort(totalnamelength); //FILENAME LENGTH
		this.globalzip.putLong(0L); //EXTRA FIELD LENGTH + FILE COMMENT LENGTH + DISK NUMBER + INTERNAL FILE ATTRIBUTES
		this.globalzip.putInt(0); //EXTERNAL FILE ATTRIBUTES
		this.globalzip.putInt(offset); //LOCAL HEADER OFFSET
		this.globalzip.put(path); //PARH
		this.globalzip.put(filename); //FILENAME
		this.globalzip.put(format); //FILENAME
	}
	
	public void writeSoundEntryJava(byte[] soundid) {
		int pos = this.soundjsonentry.position();
		this.soundjsonentry.put(SOUNDS_JAVA_ENTRY);
		int opos = this.soundjsonentry.position();
		this.soundjsonentry.position(pos + SOUNDS_JAVA_ENTRY_SOUNDID_OFFSET_1);
		this.soundjsonentry.put(soundid);
		this.soundjsonentry.position(pos + SOUNDS_JAVA_ENTRY_SOUNDID_OFFSET_2);
		this.soundjsonentry.put(soundid);
		this.soundjsonentry.position(opos);
	}
	
	public void writeSoundEntryBedrock(byte[] soundid) {
		int pos = this.sounddefenitionsentry.position();
		this.sounddefenitionsentry.put(SOUNDS_BEDROCK_ENTRY);
		int opos = this.sounddefenitionsentry.position();
		this.sounddefenitionsentry.position(pos + SOUNDS_BEDROCK_ENTRY_SOUNDID_OFFSET_1);
		this.sounddefenitionsentry.put(soundid);
		this.sounddefenitionsentry.position(pos + SOUNDS_BEDROCK_ENTRY_SOUNDID_OFFSET_2);
		this.sounddefenitionsentry.put(soundid);
		this.sounddefenitionsentry.position(opos);
	}
	
	public void writeSoundNamedEntryJava(byte[] soundid, byte[] soundname) {
		int pos = this.soundjsonentry.position();
		this.soundjsonentry.put(SOUNDS_NAME_JAVA_ENTRY_1);
		this.soundjsonentry.put(soundname);
		this.soundjsonentry.put(SOUNDS_NAME_JAVA_ENTRY_2);
		this.soundjsonentry.position(pos + soundname.length + SOUNDS_JAVA_NAMED_ENTRY_SOUNDID_OFFSET);
		this.soundjsonentry.put(soundid);
	}
	
	public void writeSoundNamedEntryBedrock(byte[] soundid, byte[] soundname) {
		int pos = this.sounddefenitionsentry.position();
		this.sounddefenitionsentry.put(SOUNDS_NAME_BEDROCK_ENTRY_1);
		this.sounddefenitionsentry.put(soundname);
		this.sounddefenitionsentry.put(SOUNDS_NAME_BEDROCK_ENTRY_2);
		this.sounddefenitionsentry.position(pos + soundname.length + SOUNDS_BEDROCK_NAMED_ENTRY_SOUNDID_OFFSET);
		this.sounddefenitionsentry.put(soundid);
	}

	@Override
	public void run() {
		int size = 0;
		try (SeekableByteChannel channel = this.fsp.newByteChannel(this.file, ReadSound.READ)) {
			ByteBuffer datazip = this.datazip.duplicate();
			int read;
			while (datazip.hasRemaining()) {
		        read = channel.read(datazip);
		        if (read < 0) {
		            throw new EOFException();
		        }
		        size += read;
		    }
		} catch (IOException e) {
		    throw new IllegalStateException(e);
		}
		this.crc32.update(this.datazip.duplicate());
		this.md5hash.update(this.datazip.duplicate());
		final int crc = (int) this.crc32.getValue();
		final int offset = this.localzip.position();
		final byte[] filename;
		{
			final byte[] md5 = this.md5hash.digest();
			byte i = 0x10;
			long lsb = (md5[--i] & 0xFFL) | (md5[--i] & 0xFFL) << 8 | (md5[--i] & 0xFFL) << 16 | (md5[--i] & 0xFFL) << 24 | (md5[--i] & 0xFFL) << 32 | (md5[--i] & 0xFFL) << 40 | (md5[--i] & 0xFFL) << 48 | (md5[--i] & 0xFFL) << 56;
			this.soundid = new UUID((md5[--i] & 0xFFL) | (md5[--i] & 0xFFL) << 8 | (md5[--i] & 0xFFL) << 16 | (md5[--i] & 0xFFL) << 24 | (md5[--i] & 0xFFL) << 32 | (md5[--i] & 0xFFL) << 40 | (md5[--i] & 0xFFL) << 48 | (md5[--i] & 0xFFL) << 56, lsb);
			filename = ReadSound.fromUUIDBytesToUUIDHexBytes(md5);
		}
		this.writeLocalZipHeader(crc, size);
		this.writeGlobalZipHeader(crc, size, offset, JAVA_PATH, filename, OGG);
		this.writeGlobalZipHeader(crc, size, offset, BEDROCK_PATH, filename, OGG);
		this.writeSoundEntryJava(filename);
		this.writeSoundEntryBedrock(filename);
		if(this.soundname == null) return;
		this.writeSoundNamedEntryJava(filename, this.soundname);
		this.writeSoundNamedEntryBedrock(filename, this.soundname);
	}

}
