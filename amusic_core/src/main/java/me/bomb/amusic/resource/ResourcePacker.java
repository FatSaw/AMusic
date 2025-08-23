package me.bomb.amusic.resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import me.bomb.amusic.packedinfo.SoundInfo;
import me.bomb.amusic.source.PackSource;
import me.bomb.amusic.source.SoundSource;
import me.bomb.amusic.source.SourceEntry;
import me.bomb.amusic.util.ByteArraysOutputStream;
import me.bomb.amusic.util.HexUtils;

import static java.lang.System.currentTimeMillis;

public final class ResourcePacker implements Runnable {
	
	private final static byte[] silencesound;
	
	public SoundInfo[] sounds;
	public byte[] sha1, resourcepack = null;

	private final SoundSource soundsource;
	private final String id;
	private final PackSource packsource;
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
	
	public ResourcePacker(SoundSource source, String id, PackSource packsource) {
		this.soundsource = source;
		this.id = id;
		this.packsource = packsource;
	}
	
	public void run() {
		final long timestart = currentTimeMillis();
		SourceEntry sourceentry = soundsource.get(this.id);
		if(sourceentry == null) {
			return;
		}
		String[] soundnames = sourceentry.names;
		byte[] splits = sourceentry.splits;
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
				final byte split = splits[i];
				short partid = (short) (split & 0xFF);
				byte splitbitid = 8;
				while(--splitbitid > -1) {
					boolean splitpresent = ((split >>> splitbitid) & 0x01) == 0x01;
					if(splitpresent) {
						byte partscount = (byte) (1 << splitbitid);
						while(--partscount > -1) {
							String musicid = new StringBuilder("music").append(HexUtils.shortToHex((short) i)).append(HexUtils.byteToHex((byte) --partid)).toString();
							sounds.append("\t\"amusic.");
							sounds.append(musicid);
							sounds.append("\": {\n\t\t\"category\": \"voice\",\n\t\t\"sounds\": [\n\t\t\t{\n\t\t\t\t\"attenuation_distance\": 2147483647,\n\t\t\t\t\"name\": \"amusic/");
							sounds.append(musicid);
							sounds.append("\",\n\t\t\t\t\"stream\": true\n\t\t\t}\n\t\t]\n\t},\n");
						}
					}
				}
			}
			sounds.append("\t\"amusic.silence\": {\n\t\t\"category\": \"master\",\n\t\t\"sounds\": [\n\t\t\t{\n\t\t\t\t\"name\": \"amusic/silence\",\n\t\t\t\t\"stream\": true\n\t\t\t}\n\t\t]\n\t}\n");
			soundslist = sounds.toString();
		}
		
		{
			ByteArraysOutputStream baos;
			ZipOutputStream zos;
			int estimatedwritecount = 256 + 194 + (musiccount << 5) + (musiccount << 7);
			baos = new ByteArraysOutputStream(estimatedwritecount);
			zos = new ZipOutputStream(baos, Charset.defaultCharset());
			zos.setMethod(8);
			zos.setLevel(5);
			boolean packmcmetafound = false, soundsjsonappended = false;
			byte[] srcbuf = null;
			if(packsource != null && (srcbuf = packsource.get(id)) != null) {
				ZipInputStream zis = null;
				try {
					zis = new ZipInputStream(new ByteArrayInputStream(srcbuf), Charset.defaultCharset());
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
							int cap = (int) (close >> 9);
							if(cap < 1) {
								cap = 1;
							}
							baos.increaseCapacity(cap);
							zos.putNextEntry(new ZipEntry("assets/minecraft/sounds.json"));
							zos.write(sb.toString().getBytes(StandardCharsets.US_ASCII));
							zos.closeEntry();
							soundsjsonappended = true;
							continue;
						}
						int cap = (int) (entry.getCompressedSize() >> 9);
						if(cap < 1) {
							cap = 1;
						}
						baos.increaseCapacity(cap);
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
			byte[][][] ntopack = sourceentry.data;
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
					final byte split = splits[i];
					String entryid = "assets/minecraft/sounds/amusic/music".concat(HexUtils.shortToHex((short)i));
					
					byte[][] sounddata = ntopack[i];
					short partid = (short) (split & 0xFF);
					byte splitbitid = 8;
					while(--splitbitid > -1) {
						boolean splitpresent = ((split >> splitbitid) & 0x01) == 0x01;
						if(splitpresent) {
							byte partscount = (byte) (1 << splitbitid);
							while(--partscount > -1) {
								byte[] part = sounddata[--partid];
								int cap = part.length >> 9;
								if(cap < 1) {
									cap = 1;
								}
								try {
									baos.increaseCapacity(cap);
									zos.putNextEntry(new ZipEntry(entryid.concat(HexUtils.byteToHex((byte)partid)).concat(".ogg")));
						            zos.write(part);
						            zos.closeEntry();
								} catch (IOException e) {
								}
							}
						}
					}
					ntopack[i] = null;
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
			this.sounds[i] = new SoundInfo(soundnames[i], soundlengths[i], splits[i]);
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
