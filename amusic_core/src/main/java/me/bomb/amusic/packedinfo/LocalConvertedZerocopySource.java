package me.bomb.amusic.packedinfo;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

import me.bomb.amusic.util.HexUtils;

public final class LocalConvertedZerocopySource implements SoundSource<SourceEntry> {
	
	private final static byte[] silencesound, silencesoundglobalheader, packmcmeta, packmcmetaglobalheader, manifestjson, manifestjsonglobalheader, soundsjsonstart, sounddefenitionsstart, soundsjsonsilence, sounddefenitionssilence, soundsjsonpath, sounddefenitionspath;
	
	static {
		int size = 2983;
		CRC32 crc32 = new CRC32();
		byte[] buf = new byte[30 + size];
		InputStream is = LocalConvertedZerocopySource.class.getClassLoader().getResourceAsStream("silence.ogg");
		try {
			is.read(buf, 30, size);
			crc32.update(buf, 30, size);
			is.close();
		} catch (IOException e1) {
			if(is != null) {
				try {
					is.close();
				} catch (IOException e2) {
				}
			}
		}
		
		silencesoundglobalheader = new byte[46 + 42 + 46 + 25];
		int v = (int) crc32.getValue();
		crc32.reset();
		byte b;
		int i = -1, j = -1, k = 87;
		
		silencesoundglobalheader[++j] = 0x50;
		silencesoundglobalheader[++k] = 0x50;
		silencesoundglobalheader[++j] = 0x4b;
		silencesoundglobalheader[++k] = 0x4b;
		silencesoundglobalheader[++j] = 0x01;
		silencesoundglobalheader[++k] = 0x01;
		silencesoundglobalheader[++j] = 0x02;
		silencesoundglobalheader[++k] = 0x02;
		silencesoundglobalheader[++j] = 0x14;
		silencesoundglobalheader[++k] = 0x14;
		silencesoundglobalheader[++j] = 0x00;
		silencesoundglobalheader[++k] = 0x00;
		silencesoundglobalheader[++j] = 0x14;
		silencesoundglobalheader[++k] = 0x14;
		j += 9;
		k += 9;
		buf[++i] = 0x50;
		buf[++i] = 0x4b;
		buf[++i] = 0x03;
		buf[++i] = 0x04;
		buf[++i] = 0x14;
		i += 9;
		b = (byte) (v & 0xFF);
		silencesoundglobalheader[++j] = b;
		silencesoundglobalheader[++k] = b;
		buf[++i] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		silencesoundglobalheader[++j] = b;
		silencesoundglobalheader[++k] = b;
		buf[++i] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		silencesoundglobalheader[++j] = b;
		silencesoundglobalheader[++k] = b;
		buf[++i] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		silencesoundglobalheader[++j] = b;
		silencesoundglobalheader[++k] = b;
		buf[++i] = b;
		v = size;
		b = (byte) (v & 0xFF);
		silencesoundglobalheader[++j] = b;
		silencesoundglobalheader[++k] = b;
		buf[++i] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		silencesoundglobalheader[++j] = b;
		silencesoundglobalheader[++k] = b;
		buf[++i] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		silencesoundglobalheader[++j] = b;
		silencesoundglobalheader[++k] = b;
		buf[++i] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		silencesoundglobalheader[++j] = b;
		silencesoundglobalheader[++k] = b;
		buf[++i] = b;
		v = size;
		b = (byte) (v & 0xFF);
		silencesoundglobalheader[++j] = b;
		silencesoundglobalheader[++k] = b;
		buf[++i] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		silencesoundglobalheader[++j] = b;
		silencesoundglobalheader[++k] = b;
		buf[++i] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		silencesoundglobalheader[++j] = b;
		silencesoundglobalheader[++k] = b;
		buf[++i] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		silencesoundglobalheader[++j] = b;
		silencesoundglobalheader[++k] = b;
		buf[++i] = b;
		silencesound = buf;
		silencesoundglobalheader[++j] = 42;
		silencesoundglobalheader[++k] = 25;
		silencesoundglobalheader[++j] = 0;
		silencesoundglobalheader[++k] = 0;
		j += 16;
		k += 16;
		buf = "assets/minecraft/sounds/amusic/silence.ogg".getBytes(StandardCharsets.US_ASCII);
		System.arraycopy(buf, 0, silencesoundglobalheader, ++j, buf.length);
		buf = "sounds/amusic/silence.ogg".getBytes(StandardCharsets.US_ASCII);
		System.arraycopy(buf, 0, silencesoundglobalheader, ++k, buf.length);
		buf = "{\n\t\"pack\": {\n\t\t\"pack_format\": 1,\n\t\t\"description\": \"AMusic resourcepack\"\n\t}\n}".getBytes(StandardCharsets.US_ASCII);
		size = buf.length;
		packmcmeta = new byte[30 + size];
		System.arraycopy(buf, 0, packmcmeta, 30, size);
		buf = null;
		crc32.update(packmcmeta, 30, size);
		packmcmetaglobalheader = new byte[46 + 11];
		v = (int) crc32.getValue();
		crc32.reset();
		i = -1;
		j = -1;
		packmcmetaglobalheader[++j] = 0x50;
		packmcmetaglobalheader[++j] = 0x4b;
		packmcmetaglobalheader[++j] = 0x01;
		packmcmetaglobalheader[++j] = 0x02;
		packmcmetaglobalheader[++j] = 0x14;
		packmcmetaglobalheader[++j] = 0x00;
		packmcmetaglobalheader[++j] = 0x14;
		j += 9;
		
		packmcmeta[++i] = 0x50;
		packmcmeta[++i] = 0x4b;
		packmcmeta[++i] = 0x03;
		packmcmeta[++i] = 0x04;
		packmcmeta[++i] = 0x14;
		i += 9;
		b = (byte) (v & 0xFF);
		packmcmeta[++i] = b;
		packmcmetaglobalheader[++j] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		packmcmeta[++i] = b;
		packmcmetaglobalheader[++j] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		packmcmeta[++i] = b;
		packmcmetaglobalheader[++j] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		packmcmeta[++i] = b;
		packmcmetaglobalheader[++j] = b;
		v = size;
		b = (byte) (v & 0xFF);
		packmcmeta[++i] = b;
		packmcmetaglobalheader[++j] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		packmcmeta[++i] = b;
		packmcmetaglobalheader[++j] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		packmcmeta[++i] = b;
		packmcmetaglobalheader[++j] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		packmcmeta[++i] = b;
		packmcmetaglobalheader[++j] = b;
		v = size;
		b = (byte) (v & 0xFF);
		packmcmeta[++i] = b;
		packmcmetaglobalheader[++j] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		packmcmeta[++i] = b;
		packmcmetaglobalheader[++j] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		packmcmeta[++i] = b;
		packmcmetaglobalheader[++j] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		packmcmeta[++i] = b;
		packmcmetaglobalheader[++j] = b;
		
		packmcmetaglobalheader[++j] = 11;
		packmcmetaglobalheader[++j] = 0;
		buf = "pack.mcmeta".getBytes(StandardCharsets.US_ASCII);
		System.arraycopy(buf, 0, packmcmetaglobalheader, 46, 11);
		buf = "{\n\t\"format_version\": 2,\n\t\"header\": {\n\t\t\"name\": \"AMusic resourcepack\",\n\t\t\"description\": \"DESCRIPTION\",\n\t\t\"uuid\": \"00000000-0000-0000-0000-000000000000\",\n\t\t\"version\": [1, 0, 0],\n\t\t\"min_engine_version\": [1, 14, 0]\n\t},\n\t\"modules\": [\n\t\t{\n\t\t\t\"type\": \"resources\",\n\t\t\t\"uuid\": \"00000000-0000-0000-0000-000000000000\",\n\t\t\t\"version\": [1, 0, 0]\n\t\t}\n\t]\n}".getBytes(StandardCharsets.US_ASCII); //376
		size = buf.length;
		manifestjson = new byte[30 + size];
		System.arraycopy(buf, 0, manifestjson, 30, size);
		buf = null;
		manifestjsonglobalheader = new byte[46 + 13];
		i = -1;
		j = -1;
		manifestjsonglobalheader[++j] = 0x50;
		manifestjsonglobalheader[++j] = 0x4b;
		manifestjsonglobalheader[++j] = 0x01;
		manifestjsonglobalheader[++j] = 0x02;
		manifestjsonglobalheader[++j] = 0x14;
		manifestjsonglobalheader[++j] = 0x00;
		manifestjsonglobalheader[++j] = 0x14;
		j += 13;
		manifestjson[++i] = 0x50;
		manifestjson[++i] = 0x4b;
		manifestjson[++i] = 0x03;
		manifestjson[++i] = 0x04;
		manifestjson[++i] = 0x14;
		i += 13;
		v = size;
		b = (byte) (v & 0xFF);
		manifestjson[++i] = b;
		manifestjsonglobalheader[++j] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		manifestjson[++i] = b;
		manifestjsonglobalheader[++j] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		manifestjson[++i] = b;
		manifestjsonglobalheader[++j] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		manifestjson[++i] = b;
		manifestjsonglobalheader[++j] = b;
		v = size;
		b = (byte) (v & 0xFF);
		manifestjson[++i] = b;
		manifestjsonglobalheader[++j] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		manifestjson[++i] = b;
		manifestjsonglobalheader[++j] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		manifestjson[++i] = b;
		manifestjsonglobalheader[++j] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		manifestjson[++i] = b;
		manifestjsonglobalheader[++j] = b;
		manifestjsonglobalheader[++j] = 13;
		manifestjsonglobalheader[++j] = 0;
		buf = "manifest.json".getBytes(StandardCharsets.US_ASCII);
		System.arraycopy(buf, 0, manifestjsonglobalheader, 46, 13);
		soundsjsonstart = "{\n".getBytes(StandardCharsets.US_ASCII);
		sounddefenitionsstart = "{\n\t\"format_version\": \"1.14.0\",\n\t\"sound_definitions\": {\n".getBytes(StandardCharsets.US_ASCII);
		soundsjsonsilence = "\t\"amusic.internal.silence\": {\n\t\t\"category\": \"master\",\n\t\t\"sounds\": [\n\t\t\t{\n\t\t\t\t\"name\": \"minecraft:amusic/silence\",\n\t\t\t\t\"stream\": true\n\t\t\t}\n\t\t]\n\t}\n}".getBytes(StandardCharsets.US_ASCII);
		sounddefenitionssilence = "\t\t\"amusic.internal.silence\": {\n\t\t\t\"category\": \"voice\",\n\t\t\t\"sounds\": [\n\t\t\t\t{\n\t\t\t\t\t\"name\": \"sounds/amusic/silence\",\n\t\t\t\t\t\"stream\": true\n\t\t\t\t}\n\t\t\t]\n\t\t}\n\t}\n}".getBytes(StandardCharsets.US_ASCII);
		soundsjsonpath = "assets/minecraft/sounds.json".getBytes(StandardCharsets.US_ASCII);
		sounddefenitionspath = "sounds/sound_definitions.json".getBytes(StandardCharsets.US_ASCII);
	}

	private final FileSystemProvider fsp;
	private final RegularFileFilter regularfilefilter;
	private final DirectoryFilter directoryfilter;
	private final Path musicdir;
	private final int maxresourcepacksize, maxsoundsize;
	private final float threadcoefficient;
	private final short threadcountlimit;
	
	public LocalConvertedZerocopySource(Path musicdir, int maxresourcepacksize, int maxsoundsize, float threadcoefficient, short threadcountlimit) {
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
	
	public PackedResourcepack get(String entrykey) {
		DirectoryStream<Path> ds = null;
		Path musicdir = null;
		try {
			ds = fsp.newDirectoryStream(this.musicdir, this.directoryfilter);
			final Iterator<Path> it = ds.iterator();
			while(it.hasNext()) {
				final Path dir = it.next();
				if(dir.getFileName().toString().equals(entrykey)) {
					musicdir = dir;
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			if(ds != null) {
				try {
					ds.close();
				} catch (IOException e1) {
				}
			}
		}
		ds = null;
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
		
		int i;
		final byte[] resourcepack;
		final UUID[] soundhashs;
		final String[] names;
		final byte[] splits;
		final short[] lengths;
		int offset = silencesound.length, totalsize = silencesound.length;
		final Iterator<Path> it;
		HashMap<Path, Integer> filesm;
		try {
			filesm = new HashMap<>();
			ds = fsp.newDirectoryStream(musicdir, regularfilefilter);
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
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			if(ds != null) {
				try {
					ds.close();
				} catch (IOException e1) {
				}
			}
		}
		final int count = filesm.size();
		if (count == 0) {
			return null;
		}
		int resultthreadcount = count;
		resultthreadcount *= this.threadcoefficient;
		if(resultthreadcount > threadcountlimit) resultthreadcount = threadcountlimit;
		if(resultthreadcount < 1) resultthreadcount = 1;
		ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(count, false);
		totalsize += 30 * count; //ZIP LOCAL SOUND HEADERS
		int soundsjsonentryoffset = totalsize;
		totalsize += 255 * count; //18 + 42 + 118 + 42 + 35 = 255 JAVA SOUNDLIST ENTRY
		totalsize += 177; //JAVA SOUNDLIST SILENCE SOUND PATH INCLUDED + LOCAL HEADER 
		int sounddefenitionsentryoffset = totalsize;
		totalsize += 308 * count; //19 + 42 + 146 + 42 + 59 = 308 BEDROCK SOUNDLIST ENTRY
		totalsize += 234; //BEDROCK SOUNDLIST SILENCE SOUND PATH INCLUDED + LOCAL HEADER 

		final int bedrockpackidlength = totalsize;
		totalsize += packmcmeta.length; //PACK MCMETA + LOCAL HEADER 
		totalsize += manifestjson.length; //MANIFEST JSON + LOCAL HEADER 
		final int centraldirectoryoffset = totalsize, centraldirectorylength = silencesoundglobalheader.length + packmcmetaglobalheader.length + manifestjsonglobalheader.length + 149 + 229 * count;
		int globalheaderoffset = totalsize; //all sizes except global header and zip end should be calculated before this
		totalsize += silencesoundglobalheader.length; //ZIP GLOBAL SILENCE SOUND HEADERS WITH PATHS
		totalsize += 74; //SOUNDS JSON GLOBAL HEADER
		totalsize += 75; //SOUND DEFENITIONS GLOBAL HEADER
		totalsize += packmcmetaglobalheader.length; //GLOBAL HEADER PACK MCMETA
		totalsize += manifestjsonglobalheader.length; //GLOBAL HEADER MANIFEST JSON
		totalsize += 229 * count; //ZIP GLOBAL SOUND HEADERS WITH PATHS
		totalsize += 22; //ZIP GLOBAL END HEADER
		if(totalsize > this.maxresourcepacksize) {
			return null;
		}
		resourcepack = new byte[totalsize];
		System.arraycopy(silencesound, 0, resourcepack, 0, silencesound.length);
		System.arraycopy(silencesoundglobalheader, 0, resourcepack, globalheaderoffset, silencesoundglobalheader.length);
		globalheaderoffset += silencesoundglobalheader.length;
		
		i = count;
		soundhashs = new UUID[i];
		names = new String[i];
		splits = new byte[i];
		lengths = new short[i];
		int soundsjsonziplocalentryoffset = soundsjsonentryoffset;
		final int soundsjsonzipglobalentryoffset = soundsjsonentryoffset;
		int sounddefenitionsziplocalentryoffset = sounddefenitionsentryoffset;
		final int sounddefenitionszipglobalentryoffset = sounddefenitionsentryoffset;
		soundsjsonentryoffset += 30;
		sounddefenitionsentryoffset += 30;
		System.arraycopy(soundsjsonstart, 0, resourcepack, soundsjsonentryoffset, soundsjsonstart.length); //2
		System.arraycopy(sounddefenitionsstart, 0, resourcepack, sounddefenitionsentryoffset, sounddefenitionsstart.length); //51
		soundsjsonentryoffset += 2;
		sounddefenitionsentryoffset += 51;
		Iterator<Entry<Path, Integer>> fiterator = filesm.entrySet().iterator();
		while(--i > -1) {
			offset += 30;
			final Entry<Path, Integer> filee = fiterator.next();
			final Path file = filee.getKey();
			String songname = file.getFileName().toString();
			final int j = songname.lastIndexOf(".");
			if (j != -1) {
				songname = songname.substring(0, j);
			}
			names[i] = songname;
			splits[i] = 0x01; //HARDCODE SPLITS FEATURE NOT SUPPORTED FOR THIS IMPLEMENTATION
			int size = filee.getValue();
			LocalConvertedZerocopySource.ReadSoundZerocopy run = new LocalConvertedZerocopySource.ReadSoundZerocopy(this.fsp, file, (short)i, resourcepack, offset, size, globalheaderoffset, soundsjsonentryoffset, sounddefenitionsentryoffset, soundhashs, lengths);
			queue.add(run);
			offset += size;
			globalheaderoffset += 229;
			soundsjsonentryoffset += 255;
			sounddefenitionsentryoffset += 308;
		}
		ThreadPoolExecutor executor = new ThreadPoolExecutor(resultthreadcount, resultthreadcount, 0, TimeUnit.MILLISECONDS, queue);
		executor.shutdown();
		executor.prestartAllCoreThreads();
		System.arraycopy(soundsjsonsilence, 0, resourcepack, soundsjsonentryoffset, soundsjsonsilence.length); //145
		System.arraycopy(sounddefenitionssilence, 0, resourcepack, sounddefenitionsentryoffset, sounddefenitionssilence.length); //153
		soundsjsonentryoffset += 145;
		sounddefenitionsentryoffset += 153;
		offset += 30;
		offset += 30;
		offset += 2;
		offset += 51;
		offset += 255 * count;
		offset += 308 * count;
		offset += 145;
		offset += 153;
		soundsjsonentryoffset = soundsjsonentryoffset - soundsjsonziplocalentryoffset;
		soundsjsonentryoffset -= 30;
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
		CRC32 crc32 = new CRC32();
		crc32.update(resourcepack, soundsjsonziplocalentryoffset + 30, soundsjsonentryoffset);
		int v = (int) crc32.getValue();
		crc32.reset();
		byte b;
		--soundsjsonziplocalentryoffset;
		--globalheaderoffset;
		
		
		resourcepack[++globalheaderoffset] = 0x50;
		resourcepack[++globalheaderoffset] = 0x4b;
		resourcepack[++globalheaderoffset] = 0x01;
		resourcepack[++globalheaderoffset] = 0x02;
		resourcepack[++globalheaderoffset] = 0x14;
		resourcepack[++globalheaderoffset] = 0x00;
		resourcepack[++globalheaderoffset] = 0x14;
		globalheaderoffset += 9;
		
		resourcepack[++soundsjsonziplocalentryoffset] = 0x50;
		resourcepack[++soundsjsonziplocalentryoffset] = 0x4b;
		resourcepack[++soundsjsonziplocalentryoffset] = 0x03;
		resourcepack[++soundsjsonziplocalentryoffset] = 0x04;
		resourcepack[++soundsjsonziplocalentryoffset] = 0x14;
		soundsjsonziplocalentryoffset += 9;
		b = (byte) (v & 0xFF);
		resourcepack[++soundsjsonziplocalentryoffset] = b;
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++soundsjsonziplocalentryoffset] = b;
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++soundsjsonziplocalentryoffset] = b;
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++soundsjsonziplocalentryoffset] = b;
		resourcepack[++globalheaderoffset] = b;
		v = soundsjsonentryoffset;
		b = (byte) (v & 0xFF);
		resourcepack[++soundsjsonziplocalentryoffset] = b;
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++soundsjsonziplocalentryoffset] = b;
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++soundsjsonziplocalentryoffset] = b;
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++soundsjsonziplocalentryoffset] = b;
		resourcepack[++globalheaderoffset] = b;
		v = soundsjsonentryoffset;
		b = (byte) (v & 0xFF);
		resourcepack[++soundsjsonziplocalentryoffset] = b;
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++soundsjsonziplocalentryoffset] = b;
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++soundsjsonziplocalentryoffset] = b;
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++soundsjsonziplocalentryoffset] = b;
		resourcepack[++globalheaderoffset] = b;
		
		resourcepack[++globalheaderoffset] = 28;
		resourcepack[++globalheaderoffset] = 0;
		
		globalheaderoffset += 12;
		v = soundsjsonzipglobalentryoffset;
		b = (byte) (v & 0xFF);
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++globalheaderoffset] = b;
		System.arraycopy(soundsjsonpath, 0, resourcepack, 1+globalheaderoffset, soundsjsonpath.length); //28
		globalheaderoffset += soundsjsonpath.length;
		

		sounddefenitionsentryoffset = sounddefenitionsentryoffset - sounddefenitionsziplocalentryoffset;
		sounddefenitionsentryoffset -= 30;
		crc32.update(resourcepack, sounddefenitionsziplocalentryoffset + 30, sounddefenitionsentryoffset);
		v = (int) crc32.getValue();
		crc32.reset();
		--sounddefenitionsziplocalentryoffset;

		resourcepack[++globalheaderoffset] = 0x50;
		resourcepack[++globalheaderoffset] = 0x4b;
		resourcepack[++globalheaderoffset] = 0x01;
		resourcepack[++globalheaderoffset] = 0x02;
		resourcepack[++globalheaderoffset] = 0x14;
		resourcepack[++globalheaderoffset] = 0x00;
		resourcepack[++globalheaderoffset] = 0x14;
		globalheaderoffset += 9;
		
		resourcepack[++sounddefenitionsziplocalentryoffset] = 0x50;
		resourcepack[++sounddefenitionsziplocalentryoffset] = 0x4b;
		resourcepack[++sounddefenitionsziplocalentryoffset] = 0x03;
		resourcepack[++sounddefenitionsziplocalentryoffset] = 0x04;
		resourcepack[++sounddefenitionsziplocalentryoffset] = 0x14;
		sounddefenitionsziplocalentryoffset += 9;
		
		b = (byte) (v & 0xFF);
		resourcepack[++sounddefenitionsziplocalentryoffset] = b;
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++sounddefenitionsziplocalentryoffset] = b;
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++sounddefenitionsziplocalentryoffset] = b;
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++sounddefenitionsziplocalentryoffset] = b;
		resourcepack[++globalheaderoffset] = b;
		v = sounddefenitionsentryoffset;
		b = (byte) (v & 0xFF);
		resourcepack[++sounddefenitionsziplocalentryoffset] = b;
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++sounddefenitionsziplocalentryoffset] = b;
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++sounddefenitionsziplocalentryoffset] = b;
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++sounddefenitionsziplocalentryoffset] = b;
		resourcepack[++globalheaderoffset] = b;
		v = sounddefenitionsentryoffset;
		b = (byte) (v & 0xFF);
		resourcepack[++sounddefenitionsziplocalentryoffset] = b;
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++sounddefenitionsziplocalentryoffset] = b;
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++sounddefenitionsziplocalentryoffset] = b;
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++sounddefenitionsziplocalentryoffset] = b;
		resourcepack[++globalheaderoffset] = b;
		
		resourcepack[++globalheaderoffset] = 29;
		resourcepack[++globalheaderoffset] = 0;
		globalheaderoffset += 12;

		v = sounddefenitionszipglobalentryoffset;
		b = (byte) (v & 0xFF);
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++globalheaderoffset] = b;
		System.arraycopy(sounddefenitionspath, 0, resourcepack, ++globalheaderoffset, sounddefenitionspath.length); //29
		globalheaderoffset += sounddefenitionspath.length;
		sha256hash.update(resourcepack, 0, bedrockpackidlength);
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
		
		System.arraycopy(packmcmeta, 0, resourcepack, offset, packmcmeta.length);
		v = offset;
		offset += packmcmeta.length;
		System.arraycopy(packmcmetaglobalheader, 0, resourcepack, globalheaderoffset, packmcmetaglobalheader.length);
		globalheaderoffset += 41;
		b = (byte) (v & 0xFF);
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++globalheaderoffset] = b;
		globalheaderoffset += 12;
		
		System.arraycopy(manifestjson, 0, resourcepack, offset, manifestjson.length);
		System.arraycopy(manifestjsonglobalheader, 0, resourcepack, globalheaderoffset, manifestjsonglobalheader.length);
		
		System.arraycopy(bhea.toString().getBytes(StandardCharsets.US_ASCII), 0, resourcepack, offset + 143, 36);
		System.arraycopy(bres.toString().getBytes(StandardCharsets.US_ASCII), 0, resourcepack, offset + 299, 36);
		
		crc32.update(resourcepack, offset + 30, manifestjson.length - 30);
		v = (int) crc32.getValue();
		crc32.reset();
		
		globalheaderoffset += 15;
		b = (byte) (v & 0xFF);
		resourcepack[offset + 13] = b;
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[offset + 14] = b;
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[offset + 15] = b;
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[offset + 16] = b;
		resourcepack[++globalheaderoffset] = b;

		globalheaderoffset += 22;
		
		v = offset;
		b = (byte) (v & 0xFF);
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++globalheaderoffset] = b;
		v >>>= 8;
		b = (byte) (v & 0xFF);
		resourcepack[++globalheaderoffset] = b;
		globalheaderoffset += 13;
		
		offset += manifestjson.length;
		
		
		v = 6 + (count << 1);
		byte[] end = new byte[] {0x50, 0x4b, 0x05, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
		end[8] = (byte) (v & 0xFF);
		end[9] = (byte) ((v >>> 8) & 0xFF);
		end[10] = (byte) (v & 0xFF);
		end[11] = (byte) ((v >>> 8) & 0xFF);
		v = centraldirectorylength;
		end[12] = (byte) (v & 0xFF);
		end[13] = (byte) ((v >>> 8) & 0xFF);
		end[14] = (byte) ((v >>> 16) & 0xFF);
		end[15] = (byte) ((v >>> 24) & 0xFF);
		v = centraldirectoryoffset;
		end[16] = (byte) (v & 0xFF);
		end[17] = (byte) ((v >>> 8) & 0xFF);
		end[18] = (byte) ((v >>> 16) & 0xFF);
		end[19] = (byte) ((v >>> 24) & 0xFF);
		System.arraycopy(end, 0, resourcepack, ++globalheaderoffset, end.length);
		sha1hash.update(resourcepack, 0, resourcepack.length);
		sha256hash.update(resourcepack, bedrockpackidlength, resourcepack.length - bedrockpackidlength);
		return new PackedResourcepack(resourcepack, sha1hash.digest(), sha256hash.digest(), soundhashs, names, splits, lengths, bhea, bres);
	}
	
	public boolean exists(String entrykey) {
		DirectoryStream<Path> ds = null;
		Path musicdir = null;
		try {
			ds = fsp.newDirectoryStream(this.musicdir, this.directoryfilter);
			final Iterator<Path> it = ds.iterator();
			while(it.hasNext()) {
				final Path dir = it.next();
				if(dir.getFileName().toString().equals(entrykey)) {
					musicdir = dir;
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			if(ds != null) {
				try {
					ds.close();
				} catch (IOException e1) {
				}
			}
		}
		ds = null;
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

	public String[] getPlaylists() {
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
		Path musicdir = null;
		try {
			ds = fsp.newDirectoryStream(this.musicdir, this.directoryfilter);
			final Iterator<Path> it = ds.iterator();
			while(it.hasNext()) {
				final Path dir = it.next();
				if(dir.getFileName().toString().equals(entrykey)) {
					musicdir = dir;
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			if(ds != null) {
				try {
					ds.close();
				} catch (IOException e1) {
				}
			}
		}
		ds = null;
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
	
	public static final class PackedResourcepack implements SourceEntry {
		
		public final byte[] resourcepack, sha1, sha256;
		public final UUID[] soundhashs;
		public final String[] names;
		public final byte[] splits;
		public final short[] lengths;
		public final UUID bhea, bres;
		
		private PackedResourcepack(byte[] resourcepack, byte[] sha1, byte[] sha256, UUID[] soundhashs, String[] names, byte[] splits , short[] lengths, UUID bhea, UUID bres) {
			this.resourcepack = resourcepack;
			this.sha1 = sha1;
			this.sha256 = sha256;
			this.soundhashs = soundhashs;
			this.names = names;
			this.splits = splits;
			this.lengths = lengths;
			this.bhea = bhea;
			this.bres = bres;
		}
	}
	
	protected static final class ReadSoundZerocopy implements Runnable {
		
		private static final byte[] soundsjavaentry, soundsbedrockentry, javapath, bedrockpath, ogg;
		
		static {
			soundsjavaentry = "\t\"amusic.internal.00000000-0000-0000-0000-000000000000000000\": {\n\t\t\"category\": \"master\",\n\t\t\"sounds\": [\n\t\t\t{\n\t\t\t\t\"attenuation_distance\": 2147483647,\n\t\t\t\t\"name\": \"minecraft:amusic/00000000-0000-0000-0000-000000000000000000\",\n\t\t\t\t\"stream\": true\n\t\t\t}\n\t\t]\n\t},\n".getBytes(StandardCharsets.US_ASCII);
			soundsbedrockentry = "\t\t\"amusic.internal.00000000-0000-0000-0000-000000000000000000\": {\n\t\t\t\"category\": \"voice\",\n\t\t\t\"min_distance\": 3.4028235e+38,\n\t\t\t\"max_distance\": 3.4028235e+38,\n\t\t\t\"sounds\": [\n\t\t\t\t{\n\t\t\t\t\t\"name\": \"sounds/amusic/00000000-0000-0000-0000-000000000000000000\",\n\t\t\t\t\t\"stream\": true,\n\t\t\t\t\t\"is3D\": false\n\t\t\t\t}\n\t\t\t]\n\t\t},\n".getBytes(StandardCharsets.US_ASCII);
			javapath = "assets/minecraft/sounds/amusic/".getBytes(StandardCharsets.US_ASCII);
			bedrockpath = "sounds/amusic/".getBytes(StandardCharsets.US_ASCII);
			ogg = ".ogg".getBytes(StandardCharsets.US_ASCII);
		}
		
		private final FileSystemProvider fsp;
		private final Path file;
		private final short num;
		private final byte[] resourcepack;
		private final int offset, length, globalentryoffset, soundsjsonentryoffset, sounddefenitionsentryoffset;
		private final UUID[] soundhashs;
		private final short[] lengths;
		private final CRC32 crc32;
		private final MessageDigest md5hash;
		
		private ReadSoundZerocopy(FileSystemProvider fsp, Path file, short num, byte[] resourcepack, int offset, int length, int globalentryoffset, int soundsjsonentryoffset, int sounddefenitionsentryoffset, UUID[] soundhashs, short[] lengths) {
			this.fsp = fsp;
			this.file = file;
			this.num = num;
			this.resourcepack = resourcepack;
			this.offset = offset;
			this.soundhashs = soundhashs;
			this.length = length;
			this.globalentryoffset = --globalentryoffset;
			this.soundsjsonentryoffset = soundsjsonentryoffset;
			this.sounddefenitionsentryoffset = sounddefenitionsentryoffset;
			this.lengths = lengths;
			this.crc32 = new CRC32();
			try {
				this.md5hash = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				throw new IllegalStateException(e);
			}
		}
		//30 LOCAL HEADER
		//46 + 31 + 42 + 4 + 46 + 14 + 42 + 4 = 229 GLOBAL HEADER
		//259 HEADERS
		@Override
		public void run() {
			InputStream is = null;
			try {
				is = this.fsp.newInputStream(this.file);
				{
					int off = this.offset;
					int remaining = this.length;
					while (remaining > 0) {
						int read = is.read(this.resourcepack, off, remaining);
						if (read < 0) throw new EOFException();
						off += read;
						remaining -= read;
					}
				}
			} catch (IOException e) {
				throw new IllegalStateException(e);
			} finally {
				if(is == null) {
					return;
				}
				try {
					is.close();
				} catch (IOException e2) {
				}
			}
			this.lengths[this.num] = (short) calculateDuration(this.resourcepack, this.offset, this.length);
			this.crc32.update(this.resourcepack, this.offset, this.length);
			this.md5hash.update(this.resourcepack, this.offset, this.length);
			byte[] ubuf;
			{
				byte[] buf = this.md5hash.digest();
				byte i = 0x10;
				long lsb = (buf[--i] & 0xFFL) | (buf[--i] & 0xFFL) << 8 | (buf[--i] & 0xFFL) << 16 | (buf[--i] & 0xFFL) << 24 | (buf[--i] & 0xFFL) << 32 | (buf[--i] & 0xFFL) << 40 | (buf[--i] & 0xFFL) << 48 | (buf[--i] & 0xFFL) << 56;
				this.soundhashs[this.num] = new UUID((buf[--i] & 0xFFL) | (buf[--i] & 0xFFL) << 8 | (buf[--i] & 0xFFL) << 16 | (buf[--i] & 0xFFL) << 24 | (buf[--i] & 0xFFL) << 32 | (buf[--i] & 0xFFL) << 40 | (buf[--i] & 0xFFL) << 48 | (buf[--i] & 0xFFL) << 56, lsb);
				buf = HexUtils.fromUUIDBytesToUUIDHexBytes(buf);
				ubuf = new byte[42];
				System.arraycopy(buf, 0, ubuf, 0, 36);
				buf = HexUtils.shortToHex(num).getBytes(StandardCharsets.US_ASCII);
				System.arraycopy(buf, 0, ubuf, 36, 4);
				ubuf[40] = 0x30;
				ubuf[41] = 0x30;
			}
			int v = (int) crc32.getValue();
			byte b;
			int off = this.offset - 31, goff1 = this.globalentryoffset, goff2 = this.globalentryoffset + 46 + 31 + 42 + 4;
			this.resourcepack[++goff1] = 0x50;
			this.resourcepack[++goff2] = 0x50;
			this.resourcepack[++goff1] = 0x4b;
			this.resourcepack[++goff2] = 0x4b;
			this.resourcepack[++goff1] = 0x01;
			this.resourcepack[++goff2] = 0x01;
			this.resourcepack[++goff1] = 0x02;
			this.resourcepack[++goff2] = 0x02;
			this.resourcepack[++goff1] = 0x14;
			this.resourcepack[++goff2] = 0x14;
			this.resourcepack[++goff1] = 0x00;
			this.resourcepack[++goff2] = 0x00;
			this.resourcepack[++goff1] = 0x14;
			this.resourcepack[++goff2] = 0x14;
			goff1 += 9;
			goff2 += 9;
			this.resourcepack[++off] = 0x50;
			this.resourcepack[++off] = 0x4b;
			this.resourcepack[++off] = 0x03;
			this.resourcepack[++off] = 0x04;
			this.resourcepack[++off] = 0x14;
			off += 9;
			
			b = (byte) (v & 0xFF);
			this.resourcepack[++goff1] = b;
			this.resourcepack[++goff2] = b;
			this.resourcepack[++off] = b;
			v >>>= 8;
			b = (byte) (v & 0xFF);
			this.resourcepack[++goff1] = b;
			this.resourcepack[++goff2] = b;
			this.resourcepack[++off] = b;
			v >>>= 8;
			b = (byte) (v & 0xFF);
			this.resourcepack[++goff1] = b;
			this.resourcepack[++goff2] = b;
			this.resourcepack[++off] = b;
			v >>>= 8;
			b = (byte) (v & 0xFF);
			this.resourcepack[++goff1] = b;
			this.resourcepack[++goff2] = b;
			this.resourcepack[++off] = b;
			v = this.length;
			b = (byte) (v & 0xFF);
			this.resourcepack[++goff1] = b;
			this.resourcepack[++goff2] = b;
			this.resourcepack[++off] = b;
			v >>>= 8;
			b = (byte) (v & 0xFF);
			this.resourcepack[++goff1] = b;
			this.resourcepack[++goff2] = b;
			this.resourcepack[++off] = b;
			v >>>= 8;
			b = (byte) (v & 0xFF);
			this.resourcepack[++goff1] = b;
			this.resourcepack[++goff2] = b;
			this.resourcepack[++off] = b;
			v >>>= 8;
			b = (byte) (v & 0xFF);
			this.resourcepack[++goff1] = b;
			this.resourcepack[++goff2] = b;
			this.resourcepack[++off] = b;
			v = this.length;
			b = (byte) (v & 0xFF);
			this.resourcepack[++goff1] = b;
			this.resourcepack[++goff2] = b;
			this.resourcepack[++off] = b;
			v >>>= 8;
			b = (byte) (v & 0xFF);
			this.resourcepack[++goff1] = b;
			this.resourcepack[++goff2] = b;
			this.resourcepack[++off] = b;
			v >>>= 8;
			b = (byte) (v & 0xFF);
			this.resourcepack[++goff1] = b;
			this.resourcepack[++goff2] = b;
			this.resourcepack[++off] = b;
			v >>>= 8;
			b = (byte) (v & 0xFF);
			this.resourcepack[++goff1] = b;
			this.resourcepack[++goff2] = b;
			this.resourcepack[++off] = b;
			
			this.resourcepack[++goff1] = 77;
			this.resourcepack[++goff2] = 60;
			this.resourcepack[++goff1] = 0;
			this.resourcepack[++goff2] = 0;
			goff1 += 12;
			goff2 += 12;

			v = this.offset - 30;
			b = (byte) (v & 0xFF);
			this.resourcepack[++goff1] = b;
			this.resourcepack[++goff2] = b;
			v >>>= 8;
			b = (byte) (v & 0xFF);
			this.resourcepack[++goff1] = b;
			this.resourcepack[++goff2] = b;
			v >>>= 8;
			b = (byte) (v & 0xFF);
			this.resourcepack[++goff1] = b;
			this.resourcepack[++goff2] = b;
			v >>>= 8;
			b = (byte) (v & 0xFF);
			this.resourcepack[++goff1] = b;
			this.resourcepack[++goff2] = b;
			
			System.arraycopy(javapath, 0, this.resourcepack, ++goff1, javapath.length);
			goff1 += 31;
			System.arraycopy(bedrockpath, 0, this.resourcepack, ++goff2, bedrockpath.length);
			goff2 += 14;
			System.arraycopy(soundsjavaentry, 0, this.resourcepack, soundsjsonentryoffset, soundsjavaentry.length);
			System.arraycopy(soundsbedrockentry, 0, this.resourcepack, sounddefenitionsentryoffset, soundsbedrockentry.length);
			System.arraycopy(ubuf, 0, this.resourcepack, goff1, ubuf.length);
			System.arraycopy(ubuf, 0, this.resourcepack, goff2, ubuf.length);
			goff1 += ubuf.length;
			goff2 += ubuf.length;
			System.arraycopy(ogg, 0, this.resourcepack, goff1, ogg.length);
			System.arraycopy(ogg, 0, this.resourcepack, goff2, ogg.length);
			goff1 += 4;
			goff2 += 4;
			System.arraycopy(ubuf, 0, this.resourcepack, soundsjsonentryoffset + 18, ubuf.length);
			System.arraycopy(ubuf, 0, this.resourcepack, sounddefenitionsentryoffset + 19, ubuf.length);
			System.arraycopy(ubuf, 0, this.resourcepack, soundsjsonentryoffset + 178, ubuf.length);
			System.arraycopy(ubuf, 0, this.resourcepack, sounddefenitionsentryoffset + 207, ubuf.length);
		}
		
		protected static int calculateDuration(final byte[] buf, final int offset, final int length) {
			long len = -1L;
			int rate = -1;
			for (int i = offset + length - 15; i >= offset && len < 0L; i--) {
				if (buf[i] == (byte) 'O' && buf[i + 1] == (byte) 'g' && buf[i + 2] == (byte) 'g' && buf[i + 3] == (byte) 'S') {
					byte[] byteArray = new byte[] { buf[i + 6], buf[i + 7], buf[i + 8], buf[i + 9], buf[i + 10], buf[i + 11], buf[i + 12], buf[i + 13] };
					ByteBuffer bb = ByteBuffer.wrap(byteArray);
					bb.order(ByteOrder.LITTLE_ENDIAN);
					len = bb.getLong(0);
				}
			}
			for (int i = offset; i < offset + length - 14 && rate < 0; i++) {
				if (buf[i] == (byte) 'v' && buf[i + 1] == (byte) 'o' && buf[i + 2] == (byte) 'r' && buf[i + 3] == (byte) 'b' && buf[i + 4] == (byte) 'i' && buf[i + 5] == (byte) 's') {
					byte[] byteArray = new byte[] { buf[i + 11], buf[i + 12], buf[i + 13], buf[i + 14] };
					ByteBuffer bb = ByteBuffer.wrap(byteArray);
					bb.order(ByteOrder.LITTLE_ENDIAN);
					rate = bb.getInt(0);
				}
			}
			long res = len / rate;
			if(res < 0) res = 0;
			if(res > 65535) res = 65535;
			return (int) res;
		}
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
