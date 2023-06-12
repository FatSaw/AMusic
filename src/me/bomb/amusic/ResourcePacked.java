package me.bomb.amusic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.bomb.amusic.Data.Options;

final class ResourcePacked extends Thread {
	private final String name;
	private final UUID target;
	private final Data data;
	private final File musicdir, tempdir, resourcefile;
	private final PackInfo packinfo;
	private static final HashMap<UUID, PackInfo> downloadedplaylistinfo = new HashMap<UUID, PackInfo>();
	private byte[] sha1 = null;
	private boolean ok = false;

	private ResourcePacked(UUID target, Data data, String name, boolean update) throws NoSuchElementException {
		this.name = name;
		this.target = target;
		this.data = data;
		musicdir = new File(ConfigOptions.musicpath.toString(), name);
		tempdir = new File(ConfigOptions.temppath.toString(), name);
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
				} else if (options.check(resourcefile)) {
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
		packinfo = new PackInfo(asongnames == null ? new ArrayList<String>() : asongnames, asonglengths == null ? new ArrayList<Short>() : asonglengths);
	}

	protected static boolean load(Player player, Data data, String name, boolean update) {
		UUID uuid = player.getUniqueId();
		remove(uuid);
		PositionTracker.remove(uuid);
		ResourcePacked resourcepacked = new ResourcePacked(uuid, data, name, update);
		if (!resourcepacked.isAlive()) {
			resourcepacked.start();
			return true;
		}
		return false;
	}

	protected static PackInfo getPackInfo(UUID playeruuid) {
		return playeruuid == null ? null : downloadedplaylistinfo.get(playeruuid);
	}

	protected static void remove(UUID playeruuid) {
		ResourcePacked.downloadedplaylistinfo.remove(playeruuid);
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
					packinfo.songs.add(songname);
				}
			}
			delete(tempdir);
			tempdir.mkdirs();
			// read base pack
			boolean asyncconvertation = musicfiles.size() > 1 && ConfigOptions.encodetracksasynchronly;
			byte musicfilessize = (byte) musicfiles.size();
			List<Converter> convertators = ConfigOptions.useconverter ? new ArrayList<Converter>(musicfilessize) : null;
			for (byte i = 0; musicfilessize > i; ++i) {
				File musicfile = musicfiles.get(i);
				if (ConfigOptions.useconverter) {
					File outfile = new File(tempdir, "music".concat(Byte.toString(i)).concat(".ogg"));
					convertators.add(new Converter(asyncconvertation, musicfile, outfile));
				}
			}
			if (ConfigOptions.useconverter) {
				if (asyncconvertation) {
					boolean convertationrunning = true;
					byte checkcount = 0;
					while (convertationrunning) {
						if (checkcount == -1) {
							return; // drop task if not finished for 4 minutes
						}
						try {
							sleep(1000);
						} catch (InterruptedException e) {
						}
						boolean finished = true;
						for (byte i = 0; i < convertators.size(); ++i) {
							finished &= convertators.get(i).status.get();
						}
						convertationrunning = !finished;
						++checkcount;
					}
				}
				musicfiles.clear();
				for (byte i = 0; i < convertators.size(); ++i) {
					File outfile = convertators.get(i).output;
					musicfiles.add(outfile);
				}
			}
			// read files
			HashMap<String, byte[]> topack = new HashMap<String, byte[]>();
			topack.put("pack.mcmeta", "{\n\t\"pack\": {\n\t\t\"pack_format\": 3,\n\t\t\"description\": \"§4§lＡＭｕｓｉｃ\"\n\t}\n}".getBytes());
			StringBuffer sounds = new StringBuffer("{");
			for (byte i = 0; i < musicfiles.size(); ++i) {
				sounds.append("\t\"amusic.music");
				sounds.append(i);
				sounds.append("\": {\n\t\t\"category\": \"master\",\n\t\t\"sounds\": [\n\t\t\t{\n\t\t\t\t\"name\":\"amusic/music");
				sounds.append(i);
				sounds.append("\",\n\t\t\t\t\"stream\": true\n\t\t\t}\n\t\t]\n");
				sounds.append(musicfilessize - 1 == i ? "\t}\n" : "\t},\n");
				File outfile = musicfiles.get(i);
				try {
					long musicfilelength;
					if ((musicfilelength = (int) outfile.length()) > ConfigOptions.maxmusicfilesize) {
						continue;
					}
					byte[] resource = new byte[(int) musicfilelength];
					FileInputStream in = new FileInputStream(outfile);
					resource = Arrays.copyOf(resource, in.read(resource));
					in.close();
					packinfo.lengths.add(calculateDuration(resource));
					topack.put("assets/minecraft/sounds/amusic/music".concat(Byte.toString(i)).concat(".ogg"), resource);
				} catch (IOException e) {
				}
			}
			sounds.append("}");
			topack.put("assets/minecraft/sounds.json", sounds.toString().getBytes());
			delete(tempdir);
			// packing to archive
			CachedResource.resetCache(resourcefile.toPath());
			try {
				ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(resourcefile));
				zipOutputStream.setMethod(8);
				zipOutputStream.setLevel(5);
				for (String entry : topack.keySet()) {
					ZipEntry zipEntry = new ZipEntry(entry);
					byte[] resource = topack.get(entry);
					try {
						zipOutputStream.putNextEntry(zipEntry);
						zipOutputStream.write(resource);
						zipOutputStream.closeEntry();
					} catch (IOException e) {
					}
				}
				zipOutputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.sha1 = data.setPlaylist(name, packinfo.songs, packinfo.lengths, resourcefile);

			data.save();
			data.load();
		}
		this.ok = true;

		Player player = Bukkit.getPlayer(target);
		if (player != null) {
			StringBuilder sb = new StringBuilder("http://");
			sb.append(ConfigOptions.host);
			sb.append(":");
			sb.append(ConfigOptions.port);
			sb.append("/");
			sb.append(CachedResource.add(player.getUniqueId(), this.resourcefile));
			sb.append(".zip");
			player.setResourcePack(sb.toString(), this.sha1);
			downloadedplaylistinfo.put(target, packinfo);
		}
	}
}
