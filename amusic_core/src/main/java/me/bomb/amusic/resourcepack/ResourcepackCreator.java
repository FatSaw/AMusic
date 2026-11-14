package me.bomb.amusic.resourcepack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

import static java.io.File.separatorChar;

import java.io.EOFException;

public final class ResourcepackCreator {
	
	private final static int ZIP_LOCAL_HEADER = 0x04034b50, ZIP_GLOBAL_HEADER = 0x02014b50, EOCD_HEADER = 0x06054b50;
	private final static short ZIP_VERSION = 0x0014;
	
	private static final byte[] SILENCE, SOUNDS_JAVA_START, SOUNDS_BEDROCK_START, SOUNDS_JAVA_END, SOUNDS_BEDROCK_END, MANIFEST_JAVA, MANIFEST_BEDROCK, SILENCE_JAVA_PATH, SILENCE_BEDROCK_PATH, SOUNDS_JAVA_PATH, SOUNDS_BEDROCK_PATH, MANIFEST_JAVA_PATH, MANIFEST_BEDROCK_PATH;
	
	private static final int EOCD_SIZE, MANIFEST_BEDROCK_IDOFF_1, MANIFEST_BEDROCK_IDOFF_2, ZIP_LOCAL_HEADER_ENTRY_SIZE, ZIP_GLOBAL_HEADER_ENTRY_SIZE;
	
	static {
		EOCD_SIZE = 22;
		int size = 2983;
		byte[] buf = new byte[size];
		InputStream is = ResourcepackCreator.class.getClassLoader().getResourceAsStream("silence.ogg");
		try {
			int off = 0;
			while(off < size) {
				int n = is.read(buf, off, size - off);
				if (n == -1) {
			        throw new EOFException();
			    }
				off += n;
			}
			is.close();
		} catch (IOException e1) {
			if(is != null) {
				try {
					is.close();
				} catch (IOException e2) {
				}
			}
		}
		SILENCE = buf;
		SOUNDS_JAVA_START = "{\n".getBytes(StandardCharsets.US_ASCII);
		SOUNDS_BEDROCK_START = "{\n\t\"format_version\": \"1.14.0\",\n\t\"sound_definitions\": {\n".getBytes(StandardCharsets.US_ASCII);
		SOUNDS_JAVA_END = "\t\"amusic.internal.silence\": {\n\t\t\"category\": \"master\",\n\t\t\"sounds\": [\n\t\t\t{\n\t\t\t\t\"name\": \"minecraft:amusic/silence\",\n\t\t\t\t\"stream\": true\n\t\t\t}\n\t\t]\n\t}\n}".getBytes(StandardCharsets.US_ASCII);
		SOUNDS_BEDROCK_END = "\t\t\"amusic.internal.silence\": {\n\t\t\t\"category\": \"voice\",\n\t\t\t\"sounds\": [\n\t\t\t\t{\n\t\t\t\t\t\"name\": \"sounds/amusic/silence\",\n\t\t\t\t\t\"stream\": true\n\t\t\t\t}\n\t\t\t]\n\t\t}\n\t}\n}".getBytes(StandardCharsets.US_ASCII);
		MANIFEST_JAVA = "{\n\t\"pack\": {\n\t\t\"pack_format\": 1,\n\t\t\"description\": \"AMusic resourcepack\"\n\t}\n}".getBytes(StandardCharsets.US_ASCII);
		
		String str = "{\n\t\"format_version\": 2,\n\t\"header\": {\n\t\t\"name\": \"AMusic resourcepack\",\n\t\t\"description\": \"DESCRIPTION\",\n\t\t\"uuid\": \"";
		MANIFEST_BEDROCK_IDOFF_1 = str.getBytes(StandardCharsets.US_ASCII).length;
		str = str.concat("00000000-0000-0000-0000-000000000000\",\n\t\t\"version\": [1, 0, 0],\n\t\t\"min_engine_version\": [1, 14, 0]\n\t},\n\t\"modules\": [\n\t\t{\n\t\t\t\"type\": \"resources\",\n\t\t\t\"uuid\": \"");
		MANIFEST_BEDROCK_IDOFF_2 = str.getBytes(StandardCharsets.US_ASCII).length;
		str = str.concat("00000000-0000-0000-0000-000000000000\",\n\t\t\t\"version\": [1, 0, 0]\n\t\t}\n\t]\n}");
		MANIFEST_BEDROCK = str.getBytes(StandardCharsets.US_ASCII);
		ZIP_LOCAL_HEADER_ENTRY_SIZE = 30;
		ZIP_GLOBAL_HEADER_ENTRY_SIZE = 46;
		SILENCE_JAVA_PATH = "assets/minecraft/sounds/amusic/silence.ogg".getBytes(StandardCharsets.US_ASCII);
		SILENCE_BEDROCK_PATH = "sounds/amusic/silence.ogg".getBytes(StandardCharsets.US_ASCII);
		SOUNDS_JAVA_PATH = "assets/minecraft/sounds.json".getBytes(StandardCharsets.US_ASCII);
		SOUNDS_BEDROCK_PATH = "sounds/sound_definitions.json".getBytes(StandardCharsets.US_ASCII);
		MANIFEST_JAVA_PATH = "pack.mcmeta".getBytes(StandardCharsets.US_ASCII);
		MANIFEST_BEDROCK_PATH = "manifest.json".getBytes(StandardCharsets.US_ASCII);
	}
	
	public static String filterName(String name) {
		char[] chars = name.toCharArray();
		int finalcount = 0;
		int i = chars.length;
		while(--i > -1) {
			char c = chars[i];
			if(c == separatorChar || c == '\0') {
				chars[i] = '\0';
			} else {
				++finalcount;
			}
		}
		char[] filtered = new char[finalcount];
		int j = 0;
		while(++i < chars.length && j < finalcount) {
			char c = chars[i];
			if(c != '\0') {
				filtered[j] = c;
				++j;
			}
		}
		return new String(filtered);
	}
	
	private final FileSystemProvider fsp;
	private final RegularFileFilter regularfilefilter;
	private final DirectoryFilter directoryfilter;
	private final Path musicdir;
	private final int maxresourcepacksize, maxsoundsize;
	private final float threadcoefficient;
	private final short threadcountlimit;
	
	public ResourcepackCreator(Path musicdir, int maxresourcepacksize, int maxsoundsize, float threadcoefficient, short threadcountlimit) {
		this.fsp = musicdir.getFileSystem().provider();
		this.regularfilefilter = new RegularFileFilter(this.fsp);
		this.directoryfilter = new DirectoryFilter(this.fsp);
		this.musicdir = musicdir;
		this.maxresourcepacksize = maxresourcepacksize;
		this.maxsoundsize = maxsoundsize;
		if(threadcountlimit < 0) {
			threadcountlimit = 0;
		}
		if(threadcoefficient < 0.0f) {
			threadcoefficient = 0.0f;
		}
		if(threadcoefficient > 1.0f) {
			threadcoefficient = 1.0f;
		}
		this.threadcoefficient = threadcoefficient;
		this.threadcountlimit = threadcountlimit;
	}
	
	public ByteBuffer get(String entrykey) {
		Path musicdir = this.musicdir.resolve(filterName(entrykey));
		MessageDigest sha1hash, sha256hash;
		try {
			sha1hash = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
		try {
			sha256hash = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
		if(musicdir == null) {
			return null;
		}

		int totalsize = 0; 
		//BEGIN MERGE PACK LOCALZIP COUNT
		boolean addmanifest = true;
		
		//END MERGE PACK LOCALZIP COUNT
		int localzipoffset = totalsize;
		//BEGIN LOCALZIP COUNT
		DirectoryStream<Path> ds = null;
		HashMap<Path, Integer> filesm = null;
		try {
			final Iterator<Path> it;
			ds = fsp.newDirectoryStream(musicdir, regularfilefilter);
			filesm = new HashMap<>();
			it = ds.iterator();
			while(it.hasNext()) {
				final Path oggfile = it.next();
				try {
					BasicFileAttributes attributes = fsp.readAttributes(oggfile, BasicFileAttributes.class);
					final long size = attributes.size();
					if(attributes.isDirectory() || size > maxsoundsize) {
						continue;
					}
					int isize = (int)size;
					filesm.put(oggfile, isize);
					totalsize += isize;
				} catch (IOException e) {
					continue;
				}
			}
		} catch (NoSuchFileException e) {
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(ds != null) {
				try {
					ds.close();
				} catch (IOException e1) {
				}
			}
		}
		final int count;
		if(filesm == null || (count = filesm.size()) == 0) {
			return null;
		}
		int i;
		i = count;
		final byte[][] soundnames;
		final String[] names;
		final Path[] filepaths;
		final int[] filesizes;
		soundnames = new byte[i][];
		names = new String[i];
		filepaths = new Path[i];
		filesizes = new int[i];
		
		Iterator<Entry<Path, Integer>> fiterator = filesm.entrySet().iterator();
		int soundnameslength = 0, soundnamescount = 0;
		while(--i > -1) {
			final Entry<Path, Integer> filee = fiterator.next();
			final Path file = filee.getKey();
			filesizes[i] = filee.getValue();
			filepaths[i] = file;
			final String songname = ReadSound.toSoundName(file.getFileName().toString());
			names[i] = songname;
			final byte[] soundname = ReadSound.asMCKeyBytes(songname);
			soundnames[i] = soundname;
			if(soundname == null) {
				continue;
			}
			soundnameslength += soundname.length;
			if(soundname != null) {
				++soundnamescount;
			}
		}
		filesm = null;
		fiterator = null;
		totalsize += ReadSound.ZIP_LOCAL_HEADER_SIZE * count;
		
		int silenceoffset = totalsize;
		int silencesize = ZIP_LOCAL_HEADER_ENTRY_SIZE + SILENCE.length;
		totalsize += silencesize;
		int soundjsonoffset = totalsize;
		int soundjsonsize = ZIP_LOCAL_HEADER_ENTRY_SIZE + SOUNDS_JAVA_START.length + SOUNDS_JAVA_END.length + ReadSound.SOUNDS_JAVA_ENTRY_SIZE * count;
		soundjsonsize += soundnameslength;
		soundjsonsize += soundnamescount * ReadSound.SOUNDS_NAME_JAVA_ENTRY_SIZE;
		totalsize += soundjsonsize;
		int sounddefenitionsoffset = totalsize;
		int sounddefenitionssize = ZIP_LOCAL_HEADER_ENTRY_SIZE + SOUNDS_BEDROCK_START.length + SOUNDS_BEDROCK_END.length + ReadSound.SOUNDS_BEDROCK_ENTRY_SIZE * count;
		sounddefenitionssize += soundnameslength;
		sounddefenitionssize += soundnamescount * ReadSound.SOUNDS_NAME_BEDROCK_ENTRY_SIZE;
		totalsize += sounddefenitionssize;
		final int sha256splitoffset = totalsize;
		int manifestjavaoffset = totalsize;
		int manifestjavasize = ZIP_LOCAL_HEADER_ENTRY_SIZE + MANIFEST_JAVA.length;
		if(addmanifest) {
			totalsize += manifestjavasize;
		}
		int manifestbedrockoffset = totalsize;
		int manifestbedrocksize = ZIP_LOCAL_HEADER_ENTRY_SIZE + MANIFEST_BEDROCK.length;
		totalsize += manifestbedrocksize;
		//END LOCALZIP COUNT
		final int cdoffset = totalsize;
		int globalzipoffset = cdoffset;
		int globalzipsize = 0;
		byte[] comment = "AMUSIC RESOURCEPACK".getBytes(StandardCharsets.US_ASCII);
		int size = comment.length;
		if(size > 0xFFFF) {
			size = 0xFFFF;
		}
		final int commentsize = size;
		totalsize += commentsize;
		//BEGIN MERGE PACK ZIP CD ENTRY COUNT
		
		//END MERGE PACK ZIP CD ENTRY COUNT
		//BEGIN ZIP CD ENTRY COUNT
		globalzipsize += ReadSound.ZIP_GLOBAL_HEADERS_SIZE * count;
		globalzipsize += ZIP_GLOBAL_HEADER_ENTRY_SIZE << 2;
		globalzipsize += SILENCE_JAVA_PATH.length;
		globalzipsize += SILENCE_BEDROCK_PATH.length;
		globalzipsize += SOUNDS_JAVA_PATH.length;
		globalzipsize += SOUNDS_BEDROCK_PATH.length;
		if(addmanifest) {
			globalzipsize += ZIP_GLOBAL_HEADER_ENTRY_SIZE;
			globalzipsize += MANIFEST_JAVA_PATH.length;
		}
		globalzipsize += ZIP_GLOBAL_HEADER_ENTRY_SIZE;
		globalzipsize += MANIFEST_BEDROCK_PATH.length;
		totalsize += globalzipsize;
		//END ZIP CD ENTRY COUNT
		//BEGIN ZIP EOCD COUNT
		totalsize += EOCD_SIZE;
		//END ZIP EOCD COUNT
		if(totalsize > this.maxresourcepacksize) {
			return null;
		}
		final ByteBuffer resourcepack = ByteBuffer.allocateDirect(totalsize);
		int opos;
		ByteBuffer soundjson = ReadSound.slice(resourcepack, soundjsonoffset, soundjsonsize);
		opos = soundjsonoffset;
		soundjsonoffset += ZIP_LOCAL_HEADER_ENTRY_SIZE;
		soundjson.position(soundjsonoffset);
		soundjson.put(SOUNDS_JAVA_START);
		soundjson.position(opos);
		soundjsonoffset += SOUNDS_JAVA_START.length;
		
		ByteBuffer sounddefenitions = ReadSound.slice(resourcepack, sounddefenitionsoffset, sounddefenitionssize);
		opos = sounddefenitionsoffset;
		sounddefenitionsoffset += ZIP_LOCAL_HEADER_ENTRY_SIZE;
		sounddefenitions.position(sounddefenitionsoffset);
		sounddefenitions.put(SOUNDS_BEDROCK_START);
		sounddefenitions.position(opos);
		sounddefenitionsoffset += SOUNDS_BEDROCK_START.length;
		i = count;
		ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(i, false);
		while(--i > -1) {
			final Path filepath = filepaths[i];
			final byte[] soundname = soundnames[i];
			size = ReadSound.ZIP_LOCAL_HEADER_SIZE;
			ByteBuffer localzip = ReadSound.slice(resourcepack, localzipoffset, size);
			localzipoffset += size;
			size = filesizes[i];
			ByteBuffer datazip = ReadSound.slice(resourcepack, localzipoffset, size);
			localzipoffset += size;
			size = ReadSound.ZIP_GLOBAL_HEADERS_SIZE;
			ByteBuffer globalzip = ReadSound.slice(resourcepack, globalzipoffset, size);
			globalzipoffset += size;
			size = ReadSound.SOUNDS_JAVA_ENTRY_SIZE + (soundname == null ? 0 : ReadSound.SOUNDS_NAME_JAVA_ENTRY_SIZE + soundname.length);
			ByteBuffer soundjsonentry = ReadSound.slice(resourcepack, soundjsonoffset, size);
			soundjsonoffset += size;
			size = ReadSound.SOUNDS_BEDROCK_ENTRY_SIZE + (soundname == null ? 0 : ReadSound.SOUNDS_NAME_BEDROCK_ENTRY_SIZE + soundname.length);
			ByteBuffer sounddefenitionsentry = ReadSound.slice(resourcepack, sounddefenitionsoffset, size);
			sounddefenitionsoffset += size;
			ReadSound readsound = new ReadSound(this.fsp, filepath, localzip, datazip, globalzip, soundjsonentry, sounddefenitionsentry, soundnames[i]);
			queue.add(readsound);
		}
		int resultthreadcount = count;
		resultthreadcount *= this.threadcoefficient;
		if(resultthreadcount > this.threadcountlimit) resultthreadcount = this.threadcountlimit;
		if(resultthreadcount < 1) resultthreadcount = 1;
		ThreadPoolExecutor executor = new ThreadPoolExecutor(resultthreadcount, resultthreadcount, 0, TimeUnit.MILLISECONDS, queue);
		executor.shutdown();
		executor.prestartAllCoreThreads();
		CRC32 crc32 = new CRC32();
		int crc;
		ByteBuffer cdglobalentry;
		ByteBuffer silence = ReadSound.slice(resourcepack, silenceoffset, silencesize);
		size = (ZIP_GLOBAL_HEADER_ENTRY_SIZE << 1) + SILENCE_JAVA_PATH.length + SILENCE_BEDROCK_PATH.length;
		cdglobalentry = ReadSound.slice(resourcepack, globalzipoffset, size);
		globalzipoffset += size;
		opos = silence.position();
		
		silenceoffset += ZIP_LOCAL_HEADER_ENTRY_SIZE;
		silence.position(silenceoffset);
		silence.put(SILENCE);
		silenceoffset += SILENCE.length;
		silence.position(opos + ZIP_LOCAL_HEADER_ENTRY_SIZE);
		size = silence.remaining();
		crc32.update(silence);
		
		crc = (int) crc32.getValue();
		
		cdglobalentry.order(ByteOrder.LITTLE_ENDIAN);
		cdglobalentry.putInt(ZIP_GLOBAL_HEADER); //HEADER
		cdglobalentry.putShort(ZIP_VERSION); //VERSION MADE BY
		cdglobalentry.putShort(ZIP_VERSION); //VERSION EXTRACT
		cdglobalentry.putLong(0L); //GENERAL PURPOSE BIT FLAG + COMPRESSION METHOD + TIME + DATE
		cdglobalentry.putInt(crc); //CRC
		cdglobalentry.putInt(size); //COMPRESSED SIZE
		cdglobalentry.putInt(size); //UNCOMPRESSED SIZE
		cdglobalentry.putShort((short) SILENCE_JAVA_PATH.length); //FILENAME LENGTH
		cdglobalentry.putLong(0L); //EXTRA FIELD LENGTH + FILE COMMENT LENGTH + DISK NUMBER + INTERNAL FILE ATTRIBUTES
		cdglobalentry.putInt(0); //EXTERNAL FILE ATTRIBUTES
		cdglobalentry.putInt(opos); //LOCAL HEADER OFFSET
		cdglobalentry.put(SILENCE_JAVA_PATH); //PARH
		cdglobalentry.putInt(ZIP_GLOBAL_HEADER); //HEADER
		cdglobalentry.putShort(ZIP_VERSION); //VERSION MADE BY
		cdglobalentry.putShort(ZIP_VERSION); //VERSION EXTRACT
		cdglobalentry.putLong(0L); //GENERAL PURPOSE BIT FLAG + COMPRESSION METHOD + TIME + DATE
		cdglobalentry.putInt(crc); //CRC
		cdglobalentry.putInt(size); //COMPRESSED SIZE
		cdglobalentry.putInt(size); //UNCOMPRESSED SIZE
		cdglobalentry.putShort((short) SILENCE_BEDROCK_PATH.length); //FILENAME LENGTH
		cdglobalentry.putLong(0L); //EXTRA FIELD LENGTH + FILE COMMENT LENGTH + DISK NUMBER + INTERNAL FILE ATTRIBUTES
		cdglobalentry.putInt(0); //EXTERNAL FILE ATTRIBUTES
		cdglobalentry.putInt(opos); //LOCAL HEADER OFFSET
		cdglobalentry.put(SILENCE_BEDROCK_PATH); //PARH
		crc32.reset();
		try {
			if(!executor.awaitTermination(1, TimeUnit.MINUTES)) {
				executor.shutdownNow();
				throw new IllegalStateException("Read timeout");
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
			e.printStackTrace();
			return null;
		}
		//AFTER THIS ALL FILES READ SHOULD BE FINISHED
		size = ZIP_GLOBAL_HEADER_ENTRY_SIZE + SOUNDS_JAVA_PATH.length;
		cdglobalentry = ReadSound.slice(resourcepack, globalzipoffset, size);
		globalzipoffset += size;
		opos = soundjson.position();
		soundjson.position(soundjsonoffset);
		soundjson.put(SOUNDS_JAVA_END);
		soundjsonoffset += SOUNDS_JAVA_END.length;
		soundjson.position(opos + ZIP_LOCAL_HEADER_ENTRY_SIZE);
		size = soundjson.remaining();
		crc32.update(soundjson);
		
		crc = (int) crc32.getValue();
		soundjson.position(opos);
		soundjson.order(ByteOrder.LITTLE_ENDIAN);
		soundjson.putInt(ZIP_LOCAL_HEADER); //HEADER
		soundjson.putShort(ZIP_VERSION); //EXTRACT VERSION
		soundjson.putLong(0L); //GENERAL PURPOSE BIT FLAG + COMPRESSION METHOD + TIME + DATE
		soundjson.putInt(crc); //CRC
		soundjson.putInt(size); //COMPRESSED SIZE
		soundjson.putInt(size); //UNCOMPRESSED SIZE
		soundjson.putInt(0); //FILENAME LENGTH + EXTRA
		
		cdglobalentry.order(ByteOrder.LITTLE_ENDIAN);
		cdglobalentry.putInt(ZIP_GLOBAL_HEADER); //HEADER
		cdglobalentry.putShort(ZIP_VERSION); //VERSION MADE BY
		cdglobalentry.putShort(ZIP_VERSION); //VERSION EXTRACT
		cdglobalentry.putLong(0L); //GENERAL PURPOSE BIT FLAG + COMPRESSION METHOD + TIME + DATE
		cdglobalentry.putInt(crc); //CRC
		cdglobalentry.putInt(size); //COMPRESSED SIZE
		cdglobalentry.putInt(size); //UNCOMPRESSED SIZE
		cdglobalentry.putShort((short) SOUNDS_JAVA_PATH.length); //FILENAME LENGTH
		cdglobalentry.putLong(0L); //EXTRA FIELD LENGTH + FILE COMMENT LENGTH + DISK NUMBER + INTERNAL FILE ATTRIBUTES
		cdglobalentry.putInt(0); //EXTERNAL FILE ATTRIBUTES
		cdglobalentry.putInt(opos); //LOCAL HEADER OFFSET
		cdglobalentry.put(SOUNDS_JAVA_PATH); //PARH
		
		crc32.reset();
		size = ZIP_GLOBAL_HEADER_ENTRY_SIZE + SOUNDS_BEDROCK_PATH.length;
		cdglobalentry = ReadSound.slice(resourcepack, globalzipoffset, size);
		globalzipoffset += size;
		opos = sounddefenitions.position();
		sounddefenitions.position(sounddefenitionsoffset);
		sounddefenitions.put(SOUNDS_BEDROCK_END);
		sounddefenitionsoffset += SOUNDS_BEDROCK_END.length;
		sounddefenitions.position(opos + ZIP_LOCAL_HEADER_ENTRY_SIZE);
		size = sounddefenitions.remaining();
		crc32.update(sounddefenitions);
		
		crc = (int) crc32.getValue();
		sounddefenitions.position(opos);
		sounddefenitions.order(ByteOrder.LITTLE_ENDIAN);
		sounddefenitions.putInt(ZIP_LOCAL_HEADER); //HEADER
		sounddefenitions.putShort(ZIP_VERSION); //EXTRACT VERSION
		sounddefenitions.putLong(0L); //GENERAL PURPOSE BIT FLAG + COMPRESSION METHOD + TIME + DATE
		sounddefenitions.putInt(crc); //CRC
		sounddefenitions.putInt(size); //COMPRESSED SIZE
		sounddefenitions.putInt(size); //UNCOMPRESSED SIZE
		sounddefenitions.putInt(0); //FILENAME LENGTH + EXTRA
		
		cdglobalentry.order(ByteOrder.LITTLE_ENDIAN);
		cdglobalentry.putInt(ZIP_GLOBAL_HEADER); //HEADER
		cdglobalentry.putShort(ZIP_VERSION); //VERSION MADE BY
		cdglobalentry.putShort(ZIP_VERSION); //VERSION EXTRACT
		cdglobalentry.putLong(0L); //GENERAL PURPOSE BIT FLAG + COMPRESSION METHOD + TIME + DATE
		cdglobalentry.putInt(crc); //CRC
		cdglobalentry.putInt(size); //COMPRESSED SIZE
		cdglobalentry.putInt(size); //UNCOMPRESSED SIZE
		cdglobalentry.putShort((short) SOUNDS_BEDROCK_PATH.length); //FILENAME LENGTH
		cdglobalentry.putLong(0L); //EXTRA FIELD LENGTH + FILE COMMENT LENGTH + DISK NUMBER + INTERNAL FILE ATTRIBUTES
		cdglobalentry.putInt(0); //EXTERNAL FILE ATTRIBUTES
		cdglobalentry.putInt(opos); //LOCAL HEADER OFFSET
		cdglobalentry.put(SOUNDS_BEDROCK_PATH); //PARH
		
		crc32.reset();
		
		
		sha256hash.update(ReadSound.slice(resourcepack, 0, sha256splitoffset));
		
		if(addmanifest) {
			ByteBuffer manifestjava = ReadSound.slice(resourcepack, manifestjavaoffset, manifestjavasize);
			size = ZIP_GLOBAL_HEADER_ENTRY_SIZE + MANIFEST_JAVA_PATH.length;
			cdglobalentry = ReadSound.slice(resourcepack, globalzipoffset, size);
			globalzipoffset += size;
			opos = manifestjava.position();
			manifestjavaoffset += ZIP_LOCAL_HEADER_ENTRY_SIZE;
			manifestjava.position(manifestjavaoffset);
			manifestjava.put(MANIFEST_JAVA);
			manifestjavaoffset += MANIFEST_JAVA.length;
			manifestjava.position(opos + ZIP_LOCAL_HEADER_ENTRY_SIZE);
			size = manifestjava.remaining();
			crc32.update(manifestjava);
			
			crc = (int) crc32.getValue();
			manifestjava.position(opos);
			manifestjava.order(ByteOrder.LITTLE_ENDIAN);
			manifestjava.putInt(ZIP_LOCAL_HEADER); //HEADER
			manifestjava.putShort(ZIP_VERSION); //EXTRACT VERSION
			manifestjava.putLong(0L); //GENERAL PURPOSE BIT FLAG + COMPRESSION METHOD + TIME + DATE
			manifestjava.putInt(crc); //CRC
			manifestjava.putInt(size); //COMPRESSED SIZE
			manifestjava.putInt(size); //UNCOMPRESSED SIZE
			manifestjava.putInt(0); //FILENAME LENGTH + EXTRA
			
			cdglobalentry.order(ByteOrder.LITTLE_ENDIAN);
			cdglobalentry.putInt(ZIP_GLOBAL_HEADER); //HEADER
			cdglobalentry.putShort(ZIP_VERSION); //VERSION MADE BY
			cdglobalentry.putShort(ZIP_VERSION); //VERSION EXTRACT
			cdglobalentry.putLong(0L); //GENERAL PURPOSE BIT FLAG + COMPRESSION METHOD + TIME + DATE
			cdglobalentry.putInt(crc); //CRC
			cdglobalentry.putInt(size); //COMPRESSED SIZE
			cdglobalentry.putInt(size); //UNCOMPRESSED SIZE
			cdglobalentry.putShort((short) MANIFEST_JAVA_PATH.length); //FILENAME LENGTH
			cdglobalentry.putLong(0L); //EXTRA FIELD LENGTH + FILE COMMENT LENGTH + DISK NUMBER + INTERNAL FILE ATTRIBUTES
			cdglobalentry.putInt(0); //EXTERNAL FILE ATTRIBUTES
			cdglobalentry.putInt(opos); //LOCAL HEADER OFFSET
			cdglobalentry.put(MANIFEST_JAVA_PATH); //PARH
			
			crc32.reset();
		}
		
		{
			UUID bhea, bres;
			{
				MessageDigest sha256hashclone;
				try {
					sha256hashclone = (MessageDigest) sha256hash.clone();
				} catch (CloneNotSupportedException e) {
					throw new IllegalStateException(e);
				}
				byte[] buf = sha256hashclone.digest();
				
				i = 0x20;
				bhea = new UUID((buf[--i] & 0xFFL) | (buf[--i] & 0xFFL) << 8 | (buf[--i] & 0xFFL) << 16 | (buf[--i] & 0xFFL) << 24 | (buf[--i] & 0xFFL) << 32 | (buf[--i] & 0xFFL) << 40 | (buf[--i] & 0xFFL) << 48 | (buf[--i] & 0xFFL) << 56, (buf[--i] & 0xFFL) | (buf[--i] & 0xFFL) << 8 | (buf[--i] & 0xFFL) << 16 | (buf[--i] & 0xFFL) << 24 | (buf[--i] & 0xFFL) << 32 | (buf[--i] & 0xFFL) << 40 | (buf[--i] & 0xFFL) << 48 | (buf[--i] & 0xFFL) << 56);
				bres = new UUID((buf[--i] & 0xFFL) | (buf[--i] & 0xFFL) << 8 | (buf[--i] & 0xFFL) << 16 | (buf[--i] & 0xFFL) << 24 | (buf[--i] & 0xFFL) << 32 | (buf[--i] & 0xFFL) << 40 | (buf[--i] & 0xFFL) << 48 | (buf[--i] & 0xFFL) << 56, (buf[--i] & 0xFFL) | (buf[--i] & 0xFFL) << 8 | (buf[--i] & 0xFFL) << 16 | (buf[--i] & 0xFFL) << 24 | (buf[--i] & 0xFFL) << 32 | (buf[--i] & 0xFFL) << 40 | (buf[--i] & 0xFFL) << 48 | (buf[--i] & 0xFFL) << 56);
			}
			ByteBuffer manifestbedrock = ReadSound.slice(resourcepack, manifestbedrockoffset, manifestbedrocksize);
			size = ZIP_GLOBAL_HEADER_ENTRY_SIZE + MANIFEST_BEDROCK_PATH.length;
			cdglobalentry = ReadSound.slice(resourcepack, globalzipoffset, size);
			globalzipoffset += size;
			opos = manifestbedrock.position();
			manifestbedrockoffset += ZIP_LOCAL_HEADER_ENTRY_SIZE;
			manifestbedrock.position(manifestbedrockoffset);
			manifestbedrock.put(MANIFEST_BEDROCK);
			manifestbedrock.position(manifestbedrockoffset + MANIFEST_BEDROCK_IDOFF_1);
			manifestbedrock.put(bhea.toString().getBytes(StandardCharsets.US_ASCII));
			manifestbedrock.position(manifestbedrockoffset + MANIFEST_BEDROCK_IDOFF_2);
			manifestbedrock.put(bres.toString().getBytes(StandardCharsets.US_ASCII));
			manifestbedrockoffset += MANIFEST_BEDROCK.length;
			manifestbedrock.position(opos + ZIP_LOCAL_HEADER_ENTRY_SIZE);
			size = manifestbedrock.remaining();
			crc32.update(manifestbedrock);
			
			crc = (int) crc32.getValue();
			manifestbedrock.position(opos);
			manifestbedrock.order(ByteOrder.LITTLE_ENDIAN);
			manifestbedrock.putInt(ZIP_LOCAL_HEADER); //HEADER
			manifestbedrock.putShort(ZIP_VERSION); //EXTRACT VERSION
			manifestbedrock.putLong(0L); //GENERAL PURPOSE BIT FLAG + COMPRESSION METHOD + TIME + DATE
			manifestbedrock.putInt(crc); //CRC
			manifestbedrock.putInt(size); //COMPRESSED SIZE
			manifestbedrock.putInt(size); //UNCOMPRESSED SIZE
			manifestbedrock.putInt(0); //FILENAME LENGTH + EXTRA
			
			cdglobalentry.order(ByteOrder.LITTLE_ENDIAN);
			cdglobalentry.putInt(ZIP_GLOBAL_HEADER); //HEADER
			cdglobalentry.putShort(ZIP_VERSION); //VERSION MADE BY
			cdglobalentry.putShort(ZIP_VERSION); //VERSION EXTRACT
			cdglobalentry.putLong(0L); //GENERAL PURPOSE BIT FLAG + COMPRESSION METHOD + TIME + DATE
			cdglobalentry.putInt(crc); //CRC
			cdglobalentry.putInt(size); //COMPRESSED SIZE
			cdglobalentry.putInt(size); //UNCOMPRESSED SIZE
			cdglobalentry.putShort((short) MANIFEST_BEDROCK_PATH.length); //FILENAME LENGTH
			cdglobalentry.putLong(0L); //EXTRA FIELD LENGTH + FILE COMMENT LENGTH + DISK NUMBER + INTERNAL FILE ATTRIBUTES
			cdglobalentry.putInt(0); //EXTERNAL FILE ATTRIBUTES
			cdglobalentry.putInt(opos); //LOCAL HEADER OFFSET
			cdglobalentry.put(MANIFEST_BEDROCK_PATH); //PARH
			
			crc32.reset();
		}
		
		ByteBuffer eocd = ReadSound.slice(resourcepack, globalzipoffset, EOCD_SIZE + commentsize);
		eocd.order(ByteOrder.LITTLE_ENDIAN);
		eocd.putInt(EOCD_HEADER); //HEADER
		eocd.putShort((short) 0);
		eocd.putShort((short) 0);
		size = (count << 1) + 5;
		if(addmanifest) {
			++size;
		}
		eocd.putShort((short) size);
		eocd.putShort((short) size);
		eocd.putInt((int) globalzipsize); //CD SIZE
		eocd.putInt((int) cdoffset); //CD OFFSET
		eocd.putShort((short) commentsize);
    	eocd.put(comment, 0, commentsize); //COMMENT
    	
    	sha1hash.update(resourcepack.duplicate());
    	sha256hash.update(ReadSound.slice(resourcepack, sha256splitoffset, totalsize - sha256splitoffset));
		return resourcepack;
	}
	
	public boolean exists(String entrykey) {
		DirectoryStream<Path> ds = null;
		Path musicdir = this.musicdir.resolve(filterName(entrykey));
		if(musicdir == null) {
			return false;
		}
		try {
			ds = fsp.newDirectoryStream(musicdir, this.directoryfilter);
			final Iterator<Path> it = ds.iterator();
			while(it.hasNext()) {
				final Path oggfile = it.next();
				try {
					BasicFileAttributes attributes = fsp.readAttributes(oggfile, BasicFileAttributes.class);
					final long size = attributes.size();
					if(size > maxsoundsize) {
						continue;
					}
					ds.close();
					return true;
				} catch (IOException e) {
					continue;
				}
			}
			ds.close();
		} catch(IOException e1) {
			if(ds != null) {
				try {
					ds.close();
				} catch(IOException e2) {
				}
			}
		}
		return false;
	}

	public String[] listResourcepacks() {
		DirectoryStream<Path> ds = null;
		ArrayList<Path> playlistsc = new ArrayList<Path>();
		try {
			ds = fsp.newDirectoryStream(this.musicdir, this.directoryfilter);
			final Iterator<Path> it = ds.iterator();
			while(it.hasNext()) {
				final Path musicdir = it.next();
				playlistsc.add(musicdir);
			}
			ds.close();
		} catch(IOException e1) {
			if(ds != null) {
				try {
					ds.close();
				} catch(IOException e2) {
				}
			}
		}
		ds = null;

		ArrayList<String> playlists = new ArrayList<String>();
		final Iterator<Path> itc = playlistsc.iterator();
		while(itc.hasNext()) {
			final Path musicdir = itc.next();
			try {
				ds = fsp.newDirectoryStream(musicdir, this.regularfilefilter);
				final Iterator<Path> it = ds.iterator();
				boolean add = false;
				while(it.hasNext()) {
					final Path oggfile = it.next();
					try {
						BasicFileAttributes attributes = fsp.readAttributes(oggfile, BasicFileAttributes.class);
						final long size = attributes.size();
						if(size > maxsoundsize) {
							continue;
						}
						add = true;
						break;
					} catch (IOException e) {
						continue;
					}
				}
				ds.close();
				if(add) {
					playlists.add(musicdir.getFileName().toString());
				}
			} catch(IOException e1) {
				if(ds != null) {
					try {
						ds.close();
					} catch(IOException e2) {
					}
				}
			}
		}
		return playlists.toArray(new String[playlists.size()]);
	}
	
	public String[] getSounds(String entrykey) {
		DirectoryStream<Path> ds = null;
		Path musicdir = this.musicdir.resolve(filterName(entrykey));
		if(musicdir == null) {
			return null;
		}
		ArrayList<String> tracks = new ArrayList<String>();
		try {
			ds = fsp.newDirectoryStream(musicdir, this.regularfilefilter);
			final Iterator<Path> it = ds.iterator();
			while(it.hasNext()) {
				final Path oggfile = it.next();
				try {
					BasicFileAttributes attributes = fsp.readAttributes(oggfile, BasicFileAttributes.class);
					final long size = attributes.size();
					if(size > maxsoundsize) {
						continue;
					}
					String trackname = musicdir.getFileName().toString();
					int index = trackname.lastIndexOf('.');
					if(index != -1) {
						trackname = trackname.substring(0, index);
					}
					tracks.add(trackname);
				} catch (IOException e) {
					continue;
				}
			}
			ds.close();
		} catch(IOException e1) {
			if(ds != null) {
				try {
					ds.close();
				} catch(IOException e2) {
				}
			}
		}
		return tracks.size() == 0 ? null : tracks.toArray(new String[tracks.size()]);
	}
	
	private static final class RegularFileFilter implements DirectoryStream.Filter<Path> {
		private final FileSystemProvider fsp;
		private RegularFileFilter(FileSystemProvider fsp) {
			this.fsp = fsp;
		}
		@Override
		public boolean accept(Path path) throws IOException {
			return this.fsp.readAttributes(path, BasicFileAttributes.class).isRegularFile();
		}
	}
	
	private static final class DirectoryFilter implements DirectoryStream.Filter<Path> {
		private final FileSystemProvider fsp;
		private DirectoryFilter(FileSystemProvider fsp) {
			this.fsp = fsp;
		}
		@Override
		public boolean accept(Path path) throws IOException {
			return this.fsp.readAttributes(path, BasicFileAttributes.class).isDirectory();
		}
	}

}
