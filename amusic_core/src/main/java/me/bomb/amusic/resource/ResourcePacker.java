package me.bomb.amusic.resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import me.bomb.amusic.packedinfo.SoundInfo;
import me.bomb.amusic.source.OggVorbisPageInfo;
import me.bomb.amusic.source.PackSource;
import me.bomb.amusic.source.SoundSource;
import me.bomb.amusic.source.SourceEntry;
import me.bomb.amusic.util.ChunkedOutputStream;
import me.bomb.amusic.util.HexUtils;

import static java.lang.System.currentTimeMillis;

public final class ResourcePacker implements Runnable {
	
	private final static Pattern invalidname;
	private final static byte[] silencesound;
	
	public SoundInfo[] sounds;
	public byte[] sha1, resourcepack = null;

	private final SoundSource soundsource;
	private final String id;
	private final PackSource packsource;
	private final boolean reproducible;
	
	static {
		invalidname = Pattern.compile("[^a-z0-9/._-]");
		byte[] buf = new byte[2983];
		InputStream is = ResourcePacker.class.getClassLoader().getResourceAsStream("silence.ogg");
		try {
			is.read(buf, 0, buf.length);
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
	
	public ResourcePacker(SoundSource source, String id, PackSource packsource, boolean reproducible) {
		this.soundsource = source;
		this.id = id;
		this.packsource = packsource;
		this.reproducible = reproducible;
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
		
		UUID[] soundhashs = sourceentry.soundhashs;
		
		{
			ChunkedOutputStream cos;
			ZipOutputStream zos;
			cos = new ChunkedOutputStream();
			zos = new ZipOutputStream(cos, StandardCharsets.UTF_8);
			zos.setMethod(8);
			zos.setLevel(5);
			boolean packmcmetafound = false;
			byte[] srcbuf = null;
			if(packsource != null && (srcbuf = packsource.get(id)) != null) {
				ZipInputStream zis = null;
				try {
					zis = new ZipInputStream(new ByteArrayInputStream(srcbuf), StandardCharsets.UTF_8);
					ZipEntry entry;
					int len;
					byte[] buf = new byte[0x2000];
					while((entry = zis.getNextEntry()) != null) {
						String entryname = entry.getName();
						if(!packmcmetafound&&entryname.equals("pack.mcmeta")) {
							packmcmetafound = true;
						}
						entry = new ZipEntry(entryname);
						entry.setTime(0L);
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
				if(!packmcmetafound) {
					ZipEntry entry = new ZipEntry("pack.mcmeta");
					entry.setTime(0L);
					zos.putNextEntry(entry);
					zos.write("{\n\t\"pack\": {\n\t\t\"pack_format\": 1,\n\t\t\"description\": \"AMusic resourcepack\"\n\t}\n}".getBytes());
					zos.closeEntry();
				}
			} catch (IOException e) {
			}
			try {
				ZipEntry entry = new ZipEntry("assets/amusic/sounds/silence.ogg");
				entry.setTime(0L);
				zos.putNextEntry(entry);
				zos.write(silencesound);
				zos.closeEntry();
			} catch (IOException e) {
			}
			byte[] channels = new byte[musiccount];
			if(this.reproducible) {
				byte sleepcount = 0;
				boolean processing = true;
				boolean[] processed = new boolean[musiccount];
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
						sleepcount = -1; //reset inactivity timer
						processed[i] = true;
					}
					if(processing) {
						try {
							Thread.sleep(250);
						} catch (InterruptedException e) {
						}
					}
				}
				if(sleepcount != 0) {
					int i = musiccount;
					while(--i > -1) {
						if(!processed[i]) {
							break;
						}
					}
					if(i == -1) {
						i = musiccount;
						byte[][][] ntopack = sourceentry.data;
						while(--i > -1) {
							final byte split = splits[i];
							String entryid = "assets/amusic/sounds/".concat(soundhashs[i].toString()).concat(HexUtils.shortToHex((short)i));
							byte[][] sounddata = ntopack[i];
							byte channel = 0;
							try {
								byte[] sound = sounddata[0];
								OggVorbisPageInfo vorbisinfo = new OggVorbisPageInfo(sound);
								channel = sound[vorbisinfo.vorbisInfoStart + 11];
							} catch (IllegalArgumentException e) {
							}
							channels[i] = channel;
							short partid = (short) (split & 0xFF);
							byte splitbitid = 8;
							while(--splitbitid > -1) {
								boolean splitpresent = ((split >> splitbitid) & 0x01) == 0x01;
								if(splitpresent) {
									byte partscount = (byte) (1 << splitbitid);
									while(--partscount > -1) {
										byte[] part = sounddata[--partid];
										try {
											ZipEntry entry = new ZipEntry(entryid.concat(HexUtils.byteToHex((byte)partid)).concat(".ogg"));
											entry.setTime(0L);
											zos.putNextEntry(entry);
								            zos.write(part);
								            zos.closeEntry();
										} catch (IOException e) {
										}
									}
								}
							}
							ntopack[i] = null;
						}
					}
				}
			} else {
				byte sleepcount = 0;
				boolean processing = true;
				byte[][][] ntopack = sourceentry.data;
				boolean[] processed = new boolean[musiccount];
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
						String entryid = "assets/amusic/sounds/".concat(soundhashs[i].toString()).concat(HexUtils.shortToHex((short)i));
						
						byte[][] sounddata = ntopack[i];
						byte channel = 0;
						try {
							byte[] sound = sounddata[0];
							OggVorbisPageInfo vorbisinfo = new OggVorbisPageInfo(sound);
							channel = sound[vorbisinfo.vorbisInfoStart + 11];
						} catch (IllegalArgumentException e) {
						}
						channels[i] = channel;
						short partid = (short) (split & 0xFF);
						byte splitbitid = 8;
						while(--splitbitid > -1) {
							boolean splitpresent = ((split >> splitbitid) & 0x01) == 0x01;
							if(splitpresent) {
								byte partscount = (byte) (1 << splitbitid);
								while(--partscount > -1) {
									byte[] part = sounddata[--partid];
									try {
										ZipEntry entry = new ZipEntry(entryid.concat(HexUtils.byteToHex((byte)partid)).concat(".ogg"));
										entry.setTime(0L);
										zos.putNextEntry(entry);
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
			}
			
			String soundslist;
			{
				StringBuffer sounds = new StringBuffer("{\n");
				int i = musiccount;
				while(--i > -1) {
					final byte split = splits[i];
					String soundid = new StringBuilder().append(soundhashs[i].toString()).append(HexUtils.shortToHex((short) i)).toString();
					String soundname = soundnames[i];
					if((split & 0x01) == 0x01 && !invalidname.matcher(soundname).find()) {
						String soundidp = soundid.concat("00");
						byte channel = channels[i];
						if(channel == 1) {
							byte att = 100;
							while(--att > 0) {
								String atts = Integer.toString(att);
								sounds.append("\t\"");
								sounds.append(atts);
								sounds.append(".");
								sounds.append(soundname);
								sounds.append("\": {\n\t\t\"category\": \"master\",\n\t\t\"sounds\": [\n\t\t\t{\n\t\t\t\t\"attenuation_distance\": ");
								sounds.append(atts);
								sounds.append(",\n\t\t\t\t\"name\": \"amusic:");
								sounds.append(soundidp);
								sounds.append("\",\n\t\t\t\t\"stream\": true\n\t\t\t}\n\t\t]\n\t},\n");
							}
						}
						sounds.append("\t\"_.");
						sounds.append(soundname);
						sounds.append("\": {\n\t\t\"category\": \"master\",\n\t\t\"sounds\": [\n\t\t\t{\n\t\t\t\t\"attenuation_distance\": 2147483647,\n\t\t\t\t\"name\": \"amusic:");
						sounds.append(soundidp);
						sounds.append("\",\n\t\t\t\t\"stream\": true\n\t\t\t}\n\t\t]\n\t},\n");
					}
					short partid = (short) (split & 0xFF);
					byte splitbitid = 8;
					while(--splitbitid > -1) {
						boolean splitpresent = ((split >>> splitbitid) & 0x01) == 0x01;
						if(splitpresent) {
							byte partscount = (byte) (1 << splitbitid);
							while(--partscount > -1) {
								String soundidp = soundid.concat(HexUtils.byteToHex((byte) --partid));
								sounds.append("\t\"internal.");
								sounds.append(soundidp);
								sounds.append("\": {\n\t\t\"category\": \"master\",\n\t\t\"sounds\": [\n\t\t\t{\n\t\t\t\t\"attenuation_distance\": 2147483647,\n\t\t\t\t\"name\": \"amusic:");
								sounds.append(soundidp);
								sounds.append("\",\n\t\t\t\t\"stream\": true\n\t\t\t}\n\t\t]\n\t},\n");
							}
						}
					}
				}
				sounds.append("\t\"internal.silence\": {\n\t\t\"category\": \"master\",\n\t\t\"sounds\": [\n\t\t\t{\n\t\t\t\t\"name\": \"amusic:silence\",\n\t\t\t\t\"stream\": true\n\t\t\t}\n\t\t]\n\t}\n}");
				soundslist = sounds.toString();
			}
			
			
			try {
				ZipEntry entry = new ZipEntry("assets/amusic/sounds.json");
				entry.setTime(0L);
				zos.putNextEntry(entry);
				zos.write(soundslist.getBytes(StandardCharsets.US_ASCII));
				zos.closeEntry();
			} catch (IOException e) {
			}
			
			try {
				zos.close();
			} catch (IOException e) {
			}
			this.resourcepack = cos.toByteArray();
		}
		this.sha1 = sha1hash.digest(this.resourcepack);
		short[] soundlengths = sourceentry.lengths;
		int i = musiccount;
		this.sounds = new SoundInfo[i];
		while(--i > -1) {
			this.sounds[i] = new SoundInfo(soundnames[i], sourceentry.soundhashs[i], soundlengths[i], splits[i]);
		}

		long timeelapsed = currentTimeMillis() - timestart;
		if(timeelapsed > 0x7FFFFFFF) {
			timeelapsed = 0x7FFFFFFF;
		}
	}

}
