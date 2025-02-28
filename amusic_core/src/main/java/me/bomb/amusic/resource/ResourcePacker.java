package me.bomb.amusic.resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import me.bomb.amusic.packedinfo.SoundInfo;
import me.bomb.amusic.source.SoundSource;
import me.bomb.amusic.source.SourceEntry;
import me.bomb.amusic.util.ByteArraysOutputStream;

import static java.lang.System.currentTimeMillis;

public final class ResourcePacker implements Runnable {
	
	private final static byte[] silencesound;
	
	public SoundInfo[] sounds;
	public byte[] sha1, resourcepack = null;

	private final SoundSource source;
	private final String id;
	public final Path resourcefile;
	private final Path sourcearchive;
	private int time = -1;
	
	static {
		byte[] buf = new byte[2983];
		InputStream is = ResourcePacker.class.getClassLoader().getResourceAsStream("silence.ogg");
		try {
			is.read(buf);
			is.close();
		} catch (IOException e1) {
			if(is != null) {
				try {
					is.close();
				} catch (IOException e2) {
				}
			}
		}
		silencesound = buf;
	}
	
	public ResourcePacker(SoundSource source, String id, Path resourcefile, Path sourcearchive) {
		this.source = source;
		this.id = id;
		this.resourcefile = resourcefile;
		this.sourcearchive = sourcearchive;
	}
	
	public void run() {
		final long timestart = currentTimeMillis();
		SourceEntry sourceentry = source.get(this.id);
		if(sourceentry == null) {
			return;
		}
		String[] soundnames = sourceentry.names;
		int musiccount = soundnames.length;
		if(musiccount < 1) {
			return;
		}
		if(musiccount > 65536) {
			musiccount = 65536;
		}
		final MessageDigest sha1hash;
		try {
			sha1hash = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			return;
		}
		String soundslist;
		{
			StringBuffer sounds = new StringBuffer("\n");
			int i = musiccount;
			while(--i > -1) {
				sounds.append("\t\"amusic.music");
				sounds.append(i);
				sounds.append("\": {\n\t\t\"category\": \"voice\",\n\t\t\"sounds\": [\n\t\t\t{\n\t\t\t\t\"attenuation_distance\": 2147483647,\n\t\t\t\t\"name\": \"amusic/music");
				sounds.append(i);
				sounds.append("\",\n\t\t\t\t\"stream\": true\n\t\t\t}\n\t\t]\n\t},\n");
			}
			sounds.append("\t\"amusic.silence\": {\n\t\t\"category\": \"master\",\n\t\t\"sounds\": [\n\t\t\t{\n\t\t\t\t\"name\": \"amusic/silence\",\n\t\t\t\t\"stream\": true\n\t\t\t}\n\t\t]\n\t}\n");
			soundslist = sounds.toString();
		}
		
		{
			ByteArraysOutputStream baos;
			ZipOutputStream zos;
			//262144000 >> 9
			baos = new ByteArraysOutputStream(512000);
			//baos = new ByteArrayOutputStream(262144000);
			zos = new ZipOutputStream(baos, Charset.defaultCharset());
			zos.setMethod(8);
			zos.setLevel(5);
			boolean packmcmetafound = false, soundsjsonappended = false;
			if(sourcearchive!=null) {
				ZipInputStream zis = null;
				try {
					zis = new ZipInputStream(sourcearchive.getFileSystem().provider().newInputStream(sourcearchive), Charset.defaultCharset());
					ZipEntry entry;
					int len;
					byte[] buf = new byte[0x2000];
					while((entry = zis.getNextEntry()) != null) {
						String entryname = entry.getName();
						if(!packmcmetafound&&entryname.equals("pack.mcmeta")) {
							packmcmetafound = true;
						} else if(!soundsjsonappended && entryname.equals("assets/minecraft/sounds.json")) {
							StringBuilder sb = new StringBuilder();
							while ((len = zis.read(buf)) != -1) {
								if(len<0x2000) buf = Arrays.copyOf(buf, len);
				            	sb.append(new String(buf, StandardCharsets.US_ASCII));
				            }
							int open = sb.indexOf("{"),close = sb.lastIndexOf("}");
							if(open==-1||close==-1) {
								continue;
							}
							sb.insert(close, ',');
							sb.insert(++close, soundslist);
							zos.putNextEntry(new ZipEntry("assets/minecraft/sounds.json"));
							zos.write(sb.toString().getBytes(StandardCharsets.US_ASCII));
							zos.closeEntry();
							soundsjsonappended = true;
							continue;
						}
						entry = new ZipEntry(entryname);
						zos.putNextEntry(entry);
			            while ((len = zis.read(buf)) != -1) {
			            	zos.write(buf, 0, len);
			            }
		                zos.closeEntry();
					}
					zis.close();
				} catch (IOException e1) {
					if(zis != null) {
						try {
							zis.close();
						} catch (IOException e2) {
						}
					}
				}
			}
			try {
				if(!soundsjsonappended) {
					zos.putNextEntry(new ZipEntry("assets/minecraft/sounds.json"));
					zos.write("{".getBytes());
					zos.write(soundslist.getBytes(StandardCharsets.US_ASCII));
					zos.write("}".getBytes());
					zos.closeEntry();
				}
				if(!packmcmetafound) {
					zos.putNextEntry(new ZipEntry("pack.mcmeta"));
					zos.write("{\n\t\"pack\": {\n\t\t\"pack_format\": 1,\n\t\t\"description\": \"AMusic resourcepack\"\n\t}\n}".getBytes());
					zos.closeEntry();
				}
			} catch (IOException e) {
			}
			try {
				zos.putNextEntry(new ZipEntry("assets/minecraft/sounds/amusic/silence.ogg"));
				zos.write(silencesound);
				zos.closeEntry();
			} catch (IOException e) {
			}
			byte sleepcount = 0;
			boolean processing = true;
			byte[][] topack = sourceentry.data;
			boolean[] success = sourceentry.success, processed = new boolean[musiccount];
			while(processing && --sleepcount != 0) {
				processing = false;
				int i = musiccount;
				while(--i > -1) {
					if(processed[i]) {
						continue;
					}
					if(!sourceentry.finished(i)) {
						processing = true;
						continue;
					}
					try {
						zos.putNextEntry(new ZipEntry("assets/minecraft/sounds/amusic/music".concat(Integer.toString(i)).concat(".ogg")));
			            zos.write(success[i] ? topack[i] : silencesound);
			            zos.closeEntry();
					} catch (IOException e) {
					}
					topack[i] = null;
					sleepcount = 0; //reset inactivity timer
					processed[i] = true;
				}
				if(processing) {
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
					}
				}
			}
			try {
				zos.close();
			} catch (IOException e) {
			}
			this.resourcepack = baos.toByteArray();
		}
		this.sha1 = sha1hash.digest(this.resourcepack);
		short[] soundlengths = sourceentry.lengths;
		int i = musiccount;
		this.sounds = new SoundInfo[i];
		while(--i > -1) {
			this.sounds[i] = new SoundInfo(soundnames[i], soundlengths[i]);
		}

		long timeelapsed = currentTimeMillis() - timestart;
		if(timeelapsed > 0x7FFFFFFF) {
			timeelapsed = 0x7FFFFFFF;
		}
		this.time = (int) timeelapsed;
	}
	
	public int getElapsedTime() {
		return this.time;
	}

}
