package me.bomb.amusic.resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import me.bomb.amusic.packedinfo.SoundInfo;
import me.bomb.amusic.source.OggVorbisPageInfo;
import me.bomb.amusic.source.PackSource;
import me.bomb.amusic.source.SoundSource;
import me.bomb.amusic.source.SourceEntry;
import me.bomb.amusic.util.ChunkedOutputStream;
import me.bomb.amusic.util.HexUtils;
import me.bomb.amusic.util.ZipOutput;

import static java.lang.System.currentTimeMillis;

public final class ResourcePacker implements Runnable {
	
	private final static byte[] silencesound;
	
	public SoundInfo[] sounds;
	public byte[] sha1, sha256, resourcepack = null;
	public UUID bhea, bres;

	private final SoundSource soundsource;
	private final String id;
	private final PackSource packsource;
	private final boolean reproducible;
	
	static {
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
		final MessageDigest sha1hash, sha256hash;
		try {
			sha1hash = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			return;
		}
		try {
			sha256hash = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			return;
		}
		
		UUID[] soundhashs = sourceentry.soundhashs;
		
		{
			ChunkedOutputStream cos;
			ZipOutput zo;
			cos = new ChunkedOutputStream();
			zo = new ZipOutput(cos);
			boolean packmcmetafound = false;
			byte[] srcbuf = null;
			if(packsource != null && (srcbuf = packsource.get(id)) != null) {
				ZipInputStream zis = null;
				try {
					zis = new ZipInputStream(new ByteArrayInputStream(srcbuf), StandardCharsets.UTF_8);
					ZipEntry entry;
					while((entry = zis.getNextEntry()) != null) {
						String entryname = entry.getName();
						if(!packmcmetafound&&entryname.equals("pack.mcmeta")) {
							packmcmetafound = true;
						}
						byte[] buf = new byte[(int) entry.getSize()];
						zis.read(buf, 0, buf.length);
						zo.putEntry(buf, entryname);
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
				zo.putSound(silencesound, "assets/amusic/sounds/silence.ogg", "sounds/amusic/silence.ogg");
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
							String soundid =  soundhashs[i].toString().concat(HexUtils.shortToHex((short)i));
							String javaentryid = "assets/amusic/sounds/";
							String bedrockentryid = "sounds/amusic/";
							
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
											String spartid = soundid.concat(HexUtils.byteToHex((byte)partid)).concat(".ogg");
											zo.putSound(part, javaentryid.concat(spartid), bedrockentryid.concat(spartid));
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
						String soundid =  soundhashs[i].toString().concat(HexUtils.shortToHex((short)i));
						String javaentryid = "assets/amusic/sounds/";
						String bedrockentryid = "sounds/amusic/";
						
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
							            String spartid = soundid.concat(HexUtils.byteToHex((byte)partid)).concat(".ogg");
										zo.putSound(part, javaentryid.concat(spartid), bedrockentryid.concat(spartid));
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
			
			String soundslistjava;
			String soundslistbedrock;
			{
				StringBuffer soundsjava = new StringBuffer("{\n");
				StringBuffer soundsbedrock = new StringBuffer("{\n\t\"format_version\": \"1.14.0\",\n\t\"sound_definitions\": {\n");
				int i = musiccount;
				while(--i > -1) {
					final byte split = splits[i];
					String soundid = new StringBuilder().append(soundhashs[i].toString()).append(HexUtils.shortToHex((short) i)).toString();
					String soundname = soundnames[i];
					byte[] soundnameb = soundname.getBytes(StandardCharsets.US_ASCII);
					int j = soundnameb.length;
					while(--j > -1) {
						byte ch = soundnameb[j];
						if(ch < 0x2D || ch > 0x7A || (ch < 0x5F && ch > 0x39) || ch == 0x60) break;
					}
					if((split & 0x01) == 0x01 && j == -1) {
						String soundidp = soundid.concat("00");
						byte channel = channels[i];
						if(channel == 1) {
							byte att = 100;
							while(--att > 0) {
								String atts = Integer.toString(att);
								soundsjava.append("\t\"");
								soundsjava.append(atts);
								soundsjava.append(".");
								soundsjava.append(soundname);
								soundsjava.append("\": {\n\t\t\"category\": \"master\",\n\t\t\"sounds\": [\n\t\t\t{\n\t\t\t\t\"attenuation_distance\": ");
								soundsjava.append(atts);
								soundsjava.append(",\n\t\t\t\t\"name\": \"amusic:");
								soundsjava.append(soundidp);
								soundsjava.append("\",\n\t\t\t\t\"stream\": true\n\t\t\t}\n\t\t]\n\t},\n");
							}
						}
						soundsjava.append("\t\"_.");
						soundsjava.append(soundname);
						soundsjava.append("\": {\n\t\t\"category\": \"master\",\n\t\t\"sounds\": [\n\t\t\t{\n\t\t\t\t\"attenuation_distance\": 2147483647,\n\t\t\t\t\"name\": \"amusic:");
						soundsjava.append(soundidp);
						soundsjava.append("\",\n\t\t\t\t\"stream\": true\n\t\t\t}\n\t\t]\n\t},\n");
					}
					short partid = (short) (split & 0xFF);
					byte splitbitid = 8;
					while(--splitbitid > -1) {
						boolean splitpresent = ((split >>> splitbitid) & 0x01) == 0x01;
						if(splitpresent) {
							byte partscount = (byte) (1 << splitbitid);
							while(--partscount > -1) {
								String soundidp = soundid.concat(HexUtils.byteToHex((byte) --partid));
								soundsjava.append("\t\"internal.");
								soundsjava.append(soundidp);
								soundsjava.append("\": {\n\t\t\"category\": \"master\",\n\t\t\"sounds\": [\n\t\t\t{\n\t\t\t\t\"attenuation_distance\": 2147483647,\n\t\t\t\t\"name\": \"amusic:");
								soundsjava.append(soundidp);
								soundsjava.append("\",\n\t\t\t\t\"stream\": true\n\t\t\t}\n\t\t]\n\t},\n");
								soundsbedrock.append("\t\t\"amusic.internal.");
								soundsbedrock.append(soundidp);
								soundsbedrock.append("\": {\n\t\t\t\"category\": \"neutral\",\n\t\t\t\"sounds\": [\n\t\t\t\t\"amusic/");
								soundsbedrock.append(soundidp);
								soundsbedrock.append("\"\n\t\t\t]\n\t\t},\n");
							}
						}
					}
				}
				soundsjava.append("\t\"internal.silence\": {\n\t\t\"category\": \"master\",\n\t\t\"sounds\": [\n\t\t\t{\n\t\t\t\t\"name\": \"amusic:silence\",\n\t\t\t\t\"stream\": true\n\t\t\t}\n\t\t]\n\t}\n}");
				soundsbedrock.append("\t\t\"amusic.internal.silence\": {\n\t\t\t\"category\": \"neutral\",\n\t\t\t\"sounds\": [\n\t\t\t\t\"amusic/silence\"\n\t\t\t]\n\t\t}\n\t}\n}");
				soundslistjava = soundsjava.toString();
				soundslistbedrock = soundsbedrock.toString();
			}
			
			try {
				zo.putEntry(soundslistjava.getBytes(StandardCharsets.US_ASCII), "assets/amusic/sounds.json");
			} catch (IOException e) {
			}
			
			try {
				zo.putEntry(soundslistbedrock.getBytes(StandardCharsets.US_ASCII), "sounds/sound_definitions.json");
			} catch (IOException e) {
			}
			
			try {
				if(!packmcmetafound) {
					zo.putEntry("{\n\t\"pack\": {\n\t\t\"pack_format\": 1,\n\t\t\"description\": \"AMusic resourcepack\"\n\t}\n}".getBytes(StandardCharsets.US_ASCII), "pack.mcmeta");
				}
			} catch (IOException e) {
			}
			bhea = UUID.fromString("bhea/0".concat(this.id)); //TODO: SET THIS BASED ON ZIP CONTENT HASH
			bres = UUID.fromString("bres/0".concat(this.id)); //TODO: SET THIS BASED ON ZIP CONTENT HASH
			try {
				StringBuilder sb = new StringBuilder();
				sb.append("{\n\t\"format_version\": 2,\n\t\"header\": {\n\t\t\"name\": \"AMusic resourcepack\",\n\t\t\"description\": \"DESCRIPTION\",\n\t\t\"uuid\": \"");
				sb.append(bhea);
				sb.append("\",\n\t\t\"version\": [1, 0, 0],\n\t\t\"min_engine_version\": [1, 14, 0]\n\t},\n\t\"modules\": [\n\t\t{\n\t\t\t\"type\": \"resources\",\n\t\t\t\"uuid\": \"");
				sb.append(bres);
				sb.append("\",\n\t\t\t\"version\": [1, 0, 0]\n\t\t}\n\t]\n}");
				zo.putEntry(sb.toString().getBytes(StandardCharsets.US_ASCII), "manifest.json");
			} catch (IOException e) {
			}
			
			try {
				zo.writeCentralDirectory();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				cos.close();
			} catch (IOException e) {
			}
			this.resourcepack = cos.toByteArray();
		}
		this.sha1 = sha1hash.digest(this.resourcepack);
		this.sha256 = sha256hash.digest(this.resourcepack);
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
