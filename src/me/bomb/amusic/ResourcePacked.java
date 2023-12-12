package me.bomb.amusic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.bomb.amusic.Data.Options;

final class ResourcePacked extends Thread {
	private final String name;
	private final UUID target;
	private final Data data;
	private final File musicdir, tempdir, resourcefile, sourcearchive;
	private final List<String> soundnames;
	private final List<Short> soundlengths;
	protected static PositionTracker positiontracker;
	private byte[] sha1 = null;
	private boolean ok = false;

	private ResourcePacked(UUID target, Data data, String name, boolean update) throws NoSuchElementException {
		this.name = name;
		this.target = target;
		this.data = data;
		musicdir = new File(ConfigOptions.musicpath.toString(), name);
		tempdir = new File(ConfigOptions.temppath.toString(), name);
		File srcarchive = new File(ConfigOptions.musicpath.toString(), name.concat(".zip"));
		sourcearchive = srcarchive.isFile() ? srcarchive : null;
		List<String> asongnames = null;
		List<Short> asonglengths = null;
		File aresourcefile = null;
		if (data.containsPlaylist(name)) {
			Options options = data.getPlaylist(name);
			File resourcefile = new File(ConfigOptions.packedpath.toString(), options.name);
			if (resourcefile != null && resourcefile.exists()) {
				if (update) {
					delete(resourcefile);
					data.removePlaylist(name);
				} else if (CachedResource.isCached(resourcefile.toPath()) || options.check(resourcefile)) {
					aresourcefile = resourcefile;
					asongnames = options.sounds;
					asonglengths = options.length;
					this.sha1 = options.sha1;
					ok = true;
				}
			}
		}
		if (!ok) {
			if(!musicdir.exists()) {
				throw new NoSuchElementException();
			}
			for (short zip = 0; zip != Short.MIN_VALUE && (aresourcefile = new File(ConfigOptions.packedpath.toString(), "music".concat(Short.toString(zip)).concat(".zip"))).exists(); ++zip) {
			}
		}
		this.resourcefile = aresourcefile;
		soundnames = asongnames == null ? new ArrayList<String>() : asongnames;
		soundlengths = asonglengths == null ? new ArrayList<Short>() : asonglengths;
	}
	private ResourcePacked(Data data, String name) throws NoSuchElementException {
		this.name = name;
		this.target = null;
		this.data = data;
		musicdir = new File(ConfigOptions.musicpath.toString(), name);
		tempdir = new File(ConfigOptions.temppath.toString(), name);
		File srcarchive = new File(ConfigOptions.musicpath.toString(), name.concat(".zip"));
		sourcearchive = srcarchive.isFile() ? srcarchive : null;
		if (!data.containsPlaylist(name)) {
			throw new NoSuchElementException();
		}
		Options options = data.getPlaylist(name);
		File resourcefile = new File(ConfigOptions.packedpath.toString(), options.name);
		if (resourcefile != null && resourcefile.exists()) {
			delete(resourcefile);
			data.removePlaylist(name);
		}
		File aresourcefile = null;

		if(!musicdir.exists()) {
			throw new NoSuchElementException();
		}
		for (short zip = 0; zip != Short.MIN_VALUE && (aresourcefile = new File(ConfigOptions.packedpath.toString(), "music".concat(Short.toString(zip)).concat(".zip"))).exists(); ++zip) {
		}
		this.resourcefile = aresourcefile;
		soundnames = new ArrayList<String>();
		soundlengths = new ArrayList<Short>();
	}

	protected static boolean load(Player player, Data data, String name, boolean update) {
		boolean processpack = ConfigOptions.processpack;
		if(player==null&&processpack) {
			ResourcePacked resourcepacked = new ResourcePacked(data, name);
			
			if (resourcepacked!=null && !resourcepacked.isAlive()) {
				resourcepacked.start();
				return true;
			}
			return false;
		}
		update&=processpack;
		UUID uuid = player.getUniqueId();
		ResourcePacked resourcepacked = new ResourcePacked(uuid, data, name, update);
		if (resourcepacked!=null && !resourcepacked.isAlive() && (processpack || resourcepacked.ok)) {
			resourcepacked.start();
			positiontracker.remove(uuid);
			return true;
		}
		return false;
	}

	private static void delete(File file) {
		try {
			if (file.isDirectory()) {
				if (file.list().length == 0) {
					file.delete();
					return;
				}
				final String[] files = file.list();
				String[] array;
				for (int length = (array = files).length, i = 0; i < length; ++i) {
					String temp = array[i];
					File fileDelete = new File(file, temp);
					delete(fileDelete);
				}
				if (file.list().length == 0) {
					file.delete();
				}
			} else {
				file.delete();
			}
		} catch (Exception e) {
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

	@Override
	public void run() {
		if (!this.ok) {
			List<File> musicfiles = new ArrayList<File>();
			for (File musicfile : musicdir.listFiles()) {
				if (ConfigOptions.useconverter || musicfile.getName().endsWith(".ogg")) {
					musicfiles.add(musicfile);
					String songname = musicfile.getName();
					if (songname.contains(".")) {
						songname = songname.substring(0, songname.lastIndexOf("."));
					}
					soundnames.add(songname);
				}
			}
			delete(tempdir);
			tempdir.mkdirs();
			// read base pack
			boolean asyncconvertation = musicfiles.size() > 1 && ConfigOptions.encodetracksasynchronly;
			byte musicfilessize = (byte) musicfiles.size();
			if (ConfigOptions.useconverter) {
				List<Converter> convertators = new ArrayList<Converter>(musicfilessize);
				for (byte i = 0; musicfilessize > i; ++i) {
					File musicfile = musicfiles.get(i);
					File outfile = new File(tempdir, "music".concat(Byte.toString(i)).concat(".ogg"));
					convertators.add(new Converter(asyncconvertation, musicfile, outfile));
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
						for (byte i = 0; i < convertators.size(); ++i) {
							finished &= convertators.get(i).status.get();
						}
						convertationrunning = !finished;
						if (++checkcount == 0) {
							return; // drop task if not finished for 4 minutes
						}
					}
				}
				musicfiles.clear();
				for (byte i = 0; i < musicfilessize; ++i) {
					File outfile = convertators.get(i).output;
					musicfiles.add(outfile);
				}
			}
			
			// read files
			byte[][] topack = new byte[musicfilessize][];
			StringBuffer sounds = new StringBuffer("\n");
			for (byte i = 0; i < musicfilessize; ++i) {
				sounds.append("\t\"amusic.music");
				sounds.append(i);
				sounds.append("\": {\n\t\t\"category\": \"master\",\n\t\t\"sounds\": [\n\t\t\t{\n\t\t\t\t\"name\":\"amusic/music");
				sounds.append(i);
				sounds.append("\",\n\t\t\t\t\"stream\": true\n\t\t\t}\n\t\t]\n");
				sounds.append(i==0 ? "\t}\n" : "\t},\n");
				File outfile = musicfiles.get(i);
				try {
					int musicfilelength;
					if ((musicfilelength = (int) outfile.length()) > ConfigOptions.maxmusicfilesize) {
						continue;
					}
					byte[] resource = new byte[musicfilelength];
					FileInputStream in = new FileInputStream(outfile);
					resource = Arrays.copyOf(resource, in.read(resource));
					in.close();
					soundlengths.add(calculateDuration(resource));
					topack[i] = resource;
				} catch (IOException e) {
				}
			}
			String soundslist = sounds.toString();
			delete(tempdir);
			// packing to archive
			CachedResource.resetCache(resourcefile.toPath());
			
			ZipOutputStream zos;
			try {
				zos = new ZipOutputStream(new FileOutputStream(resourcefile, false), Charset.defaultCharset());
			} catch (IOException e) {
				return;
			}
			zos.setMethod(8);
			zos.setLevel(5);
			boolean ioe = false;
			try {
				boolean packmcmetafound = false, soundsjsonappended = false;
				if(sourcearchive!=null) {
					ZipInputStream zis;
					zis = new ZipInputStream(new FileInputStream(sourcearchive), Charset.defaultCharset());
					ZipEntry entry;
					int len;
					byte[] buffer = new byte[1024];
					while((entry = zis.getNextEntry()) != null) {
						String entryname = entry.getName();
						if(!packmcmetafound&&entryname.equals("pack.mcmeta")) {
							packmcmetafound = true;
						} else if(!soundsjsonappended && entryname.equals("assets/minecraft/sounds.json")) {
							
							StringBuilder sb = new StringBuilder();
							while ((len = zis.read(buffer)) != -1) {
								if(len<1024) buffer = Arrays.copyOf(buffer, len);
								sb.append(new String(buffer));
							}
							int open = sb.indexOf("{"),close = sb.lastIndexOf("}");
							if(open==-1||close==-1) {
								continue;
							}
							
							while(close>open&&sb.charAt(--close) == '}');
							if(close==-1) {
								continue;
							}
							sb.insert(close, ',');
							sb.insert(++close, soundslist);
							zos.putNextEntry(new ZipEntry("assets/minecraft/sounds.json"));
							zos.write(sb.toString().getBytes());
							zos.closeEntry();
							soundsjsonappended = true;
							continue;
						}
						//String comment = entry.getComment();
						//FileTime creationtime = entry.getCreationTime(), lastaccesstime = entry.getLastAccessTime(), lastmodifiedtime = entry.getLastModifiedTime();
						entry = new ZipEntry(entryname);
						//if(comment!=null) entry.setComment(comment);
						//if(creationtime!=null) entry.setCreationTime(creationtime);
						//if(lastaccesstime!=null) entry.setLastAccessTime(lastaccesstime);
						//if(lastmodifiedtime!=null) entry.setLastModifiedTime(lastmodifiedtime);
						zos.putNextEntry(entry);
		                while ((len = zis.read(buffer)) != -1) {
		                	zos.write(buffer, 0, len);
		                }
		                zos.closeEntry();
					}
					zis.close();
				}
				for(byte i = musicfilessize; --i>-1;) {
					zos.putNextEntry(new ZipEntry("assets/minecraft/sounds/amusic/music".concat(Integer.toString(i)).concat(".ogg")));
		            zos.write(topack[i]);
		            zos.closeEntry();
					
				}
				if(!soundsjsonappended) {
					zos.putNextEntry(new ZipEntry("assets/minecraft/sounds.json"));
					zos.write("{".getBytes());
					zos.write(soundslist.getBytes());
					zos.write("}".getBytes());
					zos.closeEntry();
				}
				if(!packmcmetafound) {
					zos.putNextEntry(new ZipEntry("pack.mcmeta"));
					zos.write("{\n\t\"pack\": {\n\t\t\"pack_format\": 1,\n\t\t\"description\": \"§4§lＡＭｕｓｉｃ\"\n\t}\n}".getBytes());
					zos.closeEntry();
				}
			} catch (IOException e) {
				ioe = true;
			}
			
			try {
				zos.close();
			} catch (IOException e1) {
				return;
			}
			if(ioe) return;
			
			this.sha1 = data.setPlaylist(name, soundnames, soundlengths, resourcefile);
			
			data.save();
			data.load();
		}
		this.ok = true;

		Player player = Bukkit.getPlayer(target);
		int soundssize = soundnames.size();
		if (player == null || soundssize != soundlengths.size()) {
			return;
		}
		StringBuilder sb = new StringBuilder("http://");
		sb.append(ConfigOptions.host);
		sb.append(":");
		sb.append(ConfigOptions.port);
		sb.append("/");
		sb.append(CachedResource.add(target, this.resourcefile));
		sb.append(".zip");
		if(!ConfigOptions.checkpackstatus) {
			CachedResource.setAccepted(target);
		}
		if(ConfigOptions.legacysender) {
			LegacyPackSender.sendResourcePack(player, sb.toString(), this.sha1);
		} else {
			try {
				player.setResourcePack(sb.toString(), this.sha1);
			} catch (NoSuchMethodError e) {
				player.setResourcePack(sb.toString());
			}
		}
		ArrayList<SoundInfo> soundinfos = new ArrayList<SoundInfo>(soundssize);
		for(int i=0;i<soundssize;++i) {
			soundinfos.add(new SoundInfo(soundnames.get(i), soundlengths.get(i)));
		}
		if(!ConfigOptions.packapplystatus) {
			positiontracker.setPlaylistInfo(target, name, soundinfos);
			return;
		}
		byte i = 0;
		while(++i!=0) {
			try {
				sleep(250);
			} catch (InterruptedException e) {
			}
			if(!player.isOnline()) {
				return;
			}
			if(PackApplyListener.applied(target)) {
				positiontracker.setPlaylistInfo(target, name, soundinfos);
				return;
			}
		}
	}
}
