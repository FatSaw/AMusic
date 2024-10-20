package me.bomb.amusic;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.source.SoundSource;
import me.bomb.amusic.source.SourceEntry;

public final class ResourcePacker extends Thread {
	
	public SoundInfo[] sounds;
	public byte[] sha1 = null;

	private final SoundSource source;
	private final int maxzipsize;
	private final String entryname;
	private final File resourcefile, sourcearchive;
	private final ResourceManager resourcemanager;
	private final MessageDigest sha1hash; 
	private final Runnable runafter;
	
	
	public ResourcePacker(SoundSource source, int maxzipsize, String entryname, File resourcefile, File sourcearchive, ResourceManager resourcemanager, Runnable runafter) {
		this.source = source;
		this.maxzipsize = maxzipsize;
		this.entryname = entryname;
		this.resourcefile = resourcefile;
		this.sourcearchive = sourcearchive;
		this.resourcemanager = resourcemanager;
		this.runafter = runafter;
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
		}
		sha1hash = md;
	}
	
	public void run() {
		SourceEntry sourceentry = source.get(entryname);
		String[] soundnames = sourceentry.names;
		int musicfilessize = soundnames.length;
		if(musicfilessize > 0x0000FFFF) {
			musicfilessize = 0x0000FFFF;
		}
		String soundslist;
		{
			StringBuffer sounds = new StringBuffer("\n");
			int lastmusicfileindex = musicfilessize;
			--lastmusicfileindex;
			for (int i = 0; i < musicfilessize; ++i) {
				sounds.append("\t\"amusic.music");
				sounds.append(i);
				sounds.append("\": {\n\t\t\"category\": \"master\",\n\t\t\"sounds\": [\n\t\t\t{\n\t\t\t\t\"name\":\"amusic/music");
				sounds.append(i);
				sounds.append("\",\n\t\t\t\t\"stream\": true\n\t\t\t}\n\t\t]\n");
				sounds.append(i==lastmusicfileindex ? "\t}\n" : "\t},\n");
			}
			soundslist = sounds.toString();
		}
		
		ByteArrayOutputStream baos;
		ZipOutputStream zos;
		baos = new ByteArrayOutputStream(maxzipsize);
		zos = new ZipOutputStream(baos, Charset.defaultCharset());
		zos.setMethod(8);
		zos.setLevel(5);
		boolean packmcmetafound = false, soundsjsonappended = false;
		
		try {
			if(sourcearchive!=null) {
				ZipInputStream zis;
				zis = new ZipInputStream(new FileInputStream(sourcearchive), Charset.defaultCharset());
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
			}
			if(!soundsjsonappended) {
				zos.putNextEntry(new ZipEntry("assets/minecraft/sounds.json"));
				zos.write("{".getBytes());
				zos.write(soundslist.getBytes(StandardCharsets.US_ASCII));
				zos.write("}".getBytes());
				zos.closeEntry();
			}
			if(!packmcmetafound) {
				zos.putNextEntry(new ZipEntry("pack.mcmeta"));
				zos.write("{\n\t\"pack\": {\n\t\t\"pack_format\": 1,\n\t\t\"description\": \"§4§lＡＭｕｓｉｃ\"\n\t}\n}".getBytes());
				zos.closeEntry();
			}
		} catch (IOException e) {
		}
		
		byte sleepcount = 0;
		while(!sourceentry.finished() && --sleepcount != 0) {
			try {
				sleep(1000);
			} catch (InterruptedException e) {
			}
		}
		
		byte[][] topack = sourceentry.data;
		
		try {
			for(int i = musicfilessize; --i>-1;) {
				zos.putNextEntry(new ZipEntry("assets/minecraft/sounds/amusic/music".concat(Integer.toString(i)).concat(".ogg")));
	            zos.write(topack[i]);
	            zos.closeEntry();
			}
		} catch (IOException e) {
			return;
		} finally {
			try {
				zos.close();
			} catch (IOException e) {
			}
		}
		
		byte[] buf = baos.toByteArray();
		
		resourcemanager.resetCache(resourcefile.toPath());
		
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(resourcefile, false);
			fos.write(buf);
			fos.close();
		} catch (IOException e) {
			return;
		}
		
		this.sha1 = sha1hash.digest(buf);
		resourcemanager.putResource(resourcefile.toPath(), buf);
		
		int soundssize = soundnames.length;
		short[] soundlengths = sourceentry.lengths;
		
		this.sounds = new SoundInfo[soundssize];
		for(int i=0;i<soundssize;++i) {
			this.sounds[i] = new SoundInfo(soundnames[i], soundlengths[i]);
		}
		
		if(runafter == null) {
			return;
		}
		runafter.run();
	}

}
