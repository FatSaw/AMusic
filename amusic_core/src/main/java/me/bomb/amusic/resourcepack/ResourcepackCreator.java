package me.bomb.amusic.resourcepack;

import java.io.IOException;
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
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

import static java.io.File.separatorChar;

public final class ResourcepackCreator {
	
	private final static int ZIP_LOCAL_HEADER = 0x04034b50, ZIP_GLOBAL_HEADER = 0x02014b50, EOCD_HEADER = 0x06054b50;
	private final static short ZIP_VERSION = 0x0014;
	
	private static final byte[] SOUNDS_JAVA_START, SOUNDS_BEDROCK_START, SOUNDS_JAVA_END, SOUNDS_BEDROCK_END, SOUNDS_JAVA_PATH, SOUNDS_BEDROCK_PATH;
	
	private static final int EOCD_SIZE, ZIP_LOCAL_HEADER_ENTRY_SIZE, ZIP_GLOBAL_HEADER_ENTRY_SIZE;
	
	static {
		EOCD_SIZE = 22;
		SOUNDS_JAVA_START = "{\n".getBytes(StandardCharsets.US_ASCII);
		SOUNDS_BEDROCK_START = "{\n\t\"format_version\": \"1.14.0\",\n\t\"sound_definitions\": {\n".getBytes(StandardCharsets.US_ASCII);
		SOUNDS_JAVA_END = "\t\"amusic.internal.silence\": {\n\t\t\"category\": \"master\",\n\t\t\"sounds\": [\n\t\t\t{\n\t\t\t\t\"name\": \"minecraft:amusic/silence\",\n\t\t\t\t\"stream\": true\n\t\t\t}\n\t\t]\n\t}\n}".getBytes(StandardCharsets.US_ASCII);
		SOUNDS_BEDROCK_END = "\t\t\"amusic.internal.silence\": {\n\t\t\t\"category\": \"voice\",\n\t\t\t\"sounds\": [\n\t\t\t\t{\n\t\t\t\t\t\"name\": \"sounds/amusic/silence\",\n\t\t\t\t\t\"stream\": true\n\t\t\t\t}\n\t\t\t]\n\t\t}\n\t}\n}".getBytes(StandardCharsets.US_ASCII);
		ZIP_LOCAL_HEADER_ENTRY_SIZE = 30;
		ZIP_GLOBAL_HEADER_ENTRY_SIZE = 46;
		SOUNDS_JAVA_PATH = "assets/minecraft/sounds.json".getBytes(StandardCharsets.US_ASCII);
		SOUNDS_BEDROCK_PATH = "sounds/sound_definitions.json".getBytes(StandardCharsets.US_ASCII);
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
		//END LOCALZIP COUNT
		final int cdoffset = totalsize;
		int globalzipoffset = cdoffset;
		int globalzipsize = 0;
		byte[] comment = "".getBytes(StandardCharsets.US_ASCII);
		//BEGIN MERGE PACK ZIP CD ENTRY COUNT
		
		//END MERGE PACK ZIP CD ENTRY COUNT
		//BEGIN ZIP CD ENTRY COUNT
		globalzipsize += ReadSound.ZIP_GLOBAL_HEADERS_SIZE * count;
		globalzipsize += ZIP_GLOBAL_HEADER_ENTRY_SIZE << 1;
		globalzipsize += SOUNDS_JAVA_PATH.length;
		globalzipsize += SOUNDS_BEDROCK_PATH.length;
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
		int size;
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
		int crc;
		ByteBuffer cdglobalentry;
		
		size = ZIP_GLOBAL_HEADER_ENTRY_SIZE + SOUNDS_JAVA_PATH.length;
		cdglobalentry = ReadSound.slice(resourcepack, globalzipoffset, size);
		globalzipoffset += size;
		opos = soundjson.position();
		soundjson.position(soundjsonoffset);
		soundjson.put(SOUNDS_JAVA_END);
		soundjsonoffset += SOUNDS_JAVA_END.length;
		CRC32 crc32 = new CRC32();
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
		sounddefenitions.putInt((int) crc32.getValue()); //CRC
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
		
		ByteBuffer eocd = ReadSound.slice(resourcepack, globalzipoffset, EOCD_SIZE);
		eocd.order(ByteOrder.LITTLE_ENDIAN);

        // 1. End of central dir signature (4 bytes)
		eocd.putInt(EOCD_HEADER);

        // 2. Number of this disk (2 bytes - unsigned short)
		eocd.putShort((short) 0);

        // 3. Disk where central directory starts (2 bytes - unsigned short)
		eocd.putShort((short) 0);
		size = (count << 1) + 2;
        // 4. Number of central directory records on this disk (2 bytes - unsigned short)
		eocd.putShort((short) size);

        // 5. Total number of central directory records (2 bytes - unsigned short)
		eocd.putShort((short) size);

        // 6. Size of central directory in bytes (4 bytes - unsigned int)
		eocd.putInt((int) globalzipsize);

        // 7. Offset of start of central directory (4 bytes - unsigned int)
		eocd.putInt((int) cdoffset);

        // 8. Comment length (2 bytes - unsigned short)
		size = comment.length;
		if(size > 0xFFFF) {
			size = 0xFFFF;
		}
		eocd.putShort((short) size);
    	eocd.put(comment, 0, size); //COMMENT
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
