package me.bomb.amusic;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import me.bomb.amusic.resourceserver.ResourceManager;

public final class ResourcePacker extends Thread {
	
	public final List<String> soundnames = new ArrayList<>();
	public final List<Short> soundlengths = new ArrayList<>();
	public byte[] sha1 = null;
	
	private final boolean useconverter, encodetracksasynchronly;
	private final int bitrate, samplingrate, maxzipsize, maxsoundsize;
	private final byte channels;
	private final File ffmpegbinary, musicdir, tempdir, resourcefile, sourcearchive;
	private final ResourceManager resourcemanager;
	private static final MessageDigest sha1hash; 
	private static final FilenameFilter oggfile;
	private final Runnable runafter;
	
	static {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
		}
		sha1hash = md;
		oggfile = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".ogg");
			}
		};
	}
	
	public ResourcePacker(boolean useconverter, int bitrate, byte channels, int samplingrate, boolean encodetracksasynchronly, int maxzipsize, int maxsoundsize, File ffmpegbinary, File musicdir, File tempdir, File resourcefile, File sourcearchive, ResourceManager resourcemanager, Runnable runafter) {
		this.useconverter = useconverter;
		this.bitrate = bitrate;
		this.channels = channels;
		this.samplingrate = samplingrate;
		this.encodetracksasynchronly = encodetracksasynchronly;
		this.maxzipsize = maxzipsize;
		this.maxsoundsize = maxsoundsize;
		this.ffmpegbinary = ffmpegbinary;
		this.musicdir = musicdir;
		this.tempdir = tempdir;
		this.resourcefile = resourcefile;
		this.sourcearchive = sourcearchive;
		this.resourcemanager = resourcemanager;
		this.runafter = runafter;
	}
	
	public void run() {
		List<File> musicfiles = new ArrayList<File>();
		for (File musicfile : musicdir.listFiles()) {
			if (useconverter || musicfile.getName().endsWith(".ogg")) {
				musicfiles.add(musicfile);
				String songname = musicfile.getName();
				if (songname.contains(".")) {
					songname = songname.substring(0, songname.lastIndexOf("."));
				}
				soundnames.add(songname);
			}
		}
		// read base pack
		int musicfilessize = musicfiles.size();
		if(musicfilessize > 0x0000FFFF) {
			musicfilessize = 0x0000FFFF;
		}
		boolean asyncconvertation = musicfilessize > 1 && encodetracksasynchronly;
		if (useconverter) {
			delete(tempdir);
			tempdir.mkdirs();
			List<Converter> convertators = new ArrayList<Converter>(musicfilessize);
			for (int i = 0; musicfilessize > i; ++i) {
				File musicfile = musicfiles.get(i);
				File outfile = new File(tempdir, "music".concat(Integer.toString(i)).concat(".ogg"));
				convertators.add(new Converter(ffmpegbinary, asyncconvertation, bitrate, channels, samplingrate, musicfile, outfile));
			}
			if (asyncconvertation) {
				boolean convertationrunning = true;
				byte checkcount = 0;
				while (convertationrunning) {
					try {
						sleep(1000);
					} catch (InterruptedException e) {
					}
					boolean finished = true;
					byte i = (byte) convertators.size();
					while(--i > -1) {
						finished &= convertators.get(i).finished();
					}
					convertationrunning = !finished;
					if (++checkcount == 0) {
						return; // drop task if not finished for 4 minutes
					}
				}
			}
			musicfiles.clear();
			for (int i = 0; i < musicfilessize; ++i) {
				File outfile = convertators.get(i).output;
				musicfiles.add(outfile);
			}
		}
		
		// read files
		byte[][] topack = new byte[musicfilessize][];
		StringBuffer sounds = new StringBuffer("\n");
		int lastmusicfileindex = musicfilessize;
		--lastmusicfileindex;
		for (short i = 0; i < musicfilessize; ++i) {
			sounds.append("\t\"amusic.music");
			sounds.append(i);
			sounds.append("\": {\n\t\t\"category\": \"master\",\n\t\t\"sounds\": [\n\t\t\t{\n\t\t\t\t\"name\":\"amusic/music");
			sounds.append(i);
			sounds.append("\",\n\t\t\t\t\"stream\": true\n\t\t\t}\n\t\t]\n");
			sounds.append(i==lastmusicfileindex ? "\t}\n" : "\t},\n");
			File outfile = musicfiles.get(i);
			try {
				long filesize = outfile.length();
				if (filesize > maxsoundsize) {
					continue;
				}
				byte[] resource = new byte[(int) filesize];
				FileInputStream in = new FileInputStream(outfile);
				int size = in.read(resource);
				if(size < filesize) {
					resource = Arrays.copyOf(resource, size);
				}
				in.close();
				soundlengths.add(calculateDuration(resource));
				topack[i] = resource;
			} catch (IOException e) {
			}
		}
		String soundslist = sounds.toString();
		if (useconverter) {
			delete(tempdir);
		}
		// packing to archive
		resourcemanager.resetCache(resourcefile.toPath());
		if(musicfiles.isEmpty()) {
			if(resourcefile.isFile()) {
				resourcefile.delete();
			}
			if(musicdir.isDirectory() && musicdir.list().length == 0) {
				musicdir.delete();
			}
		} else {
			ByteArrayOutputStream baos;
			ZipOutputStream zos;
			baos = new ByteArrayOutputStream(maxzipsize);
			zos = new ZipOutputStream(baos, Charset.defaultCharset());
			zos.setMethod(8);
			zos.setLevel(5);
			
			try {
				boolean packmcmetafound = false, soundsjsonappended = false;
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
				for(int i = musicfilessize; --i>-1;) {
					zos.putNextEntry(new ZipEntry("assets/minecraft/sounds/amusic/music".concat(Integer.toString(i)).concat(".ogg")));
		            zos.write(topack[i]);
		            zos.closeEntry();
					
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
				return;
			} finally {
				try {
					zos.close();
				} catch (IOException e) {
				}
			}
			byte[] buf = baos.toByteArray();
			FileOutputStream fos;
			try {
				fos = new FileOutputStream(resourcefile, false);
				fos.write(buf);
				fos.close();
			} catch (IOException e) {
				return;
			}
			synchronized(sha1hash) {
				this.sha1 = sha1hash.digest(buf);
			}
			resourcemanager.putResource(resourcefile.toPath(), buf);
		}
		
		if(runafter == null) {
			return;
		}
		runafter.run();
	}
	
	private static void delete(File tempdirectory) {
		if (tempdirectory.isDirectory()) {
			for(File tempfile : tempdirectory.listFiles(oggfile)) {
				if(tempfile.isFile()) {
					tempfile.delete();
				}
			}
			if(tempdirectory.list().length == 0) {
				tempdirectory.delete();
			}
		}
		
	}
	
	private static short calculateDuration(byte[] t) {
		int rate = -1, length = -1, size = t.length;
		for (int i = size - 15; i >= 0 && length < 0; i--) {
			if (t[i] == (byte) 'O' && t[i + 1] == (byte) 'g' && t[i + 2] == (byte) 'g' && t[i + 3] == (byte) 'S') {
				byte[] byteArray = new byte[] { t[i + 6], t[i + 7], t[i + 8], t[i + 9], t[i + 10], t[i + 11], t[i + 12], t[i + 13] };
				ByteBuffer bb = ByteBuffer.wrap(byteArray);
				bb.order(ByteOrder.LITTLE_ENDIAN);
				length = bb.getInt(0);
			}
		}
		for (int i = 0; i < size - 14 && rate < 0; i++) {
			if (t[i] == (byte) 'v' && t[i + 1] == (byte) 'o' && t[i + 2] == (byte) 'r' && t[i + 3] == (byte) 'b' && t[i + 4] == (byte) 'i' && t[i + 5] == (byte) 's') {
				byte[] byteArray = new byte[] { t[i + 11], t[i + 12], t[i + 13], t[i + 14] };
				ByteBuffer bb = ByteBuffer.wrap(byteArray);
				bb.order(ByteOrder.LITTLE_ENDIAN);
				rate = bb.getInt(0);
			}
		}
		int res = length / rate;
		return res > Short.MAX_VALUE ? Short.MAX_VALUE : (short) res;
	}

}
