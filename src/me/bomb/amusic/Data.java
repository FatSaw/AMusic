package me.bomb.amusic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

final class Data {
	private static final File datafile;
	private Map<String, Options> options = new HashMap<String, Options>();
	static {
		JavaPlugin plugin = JavaPlugin.getPlugin(AMusic.class);
		File adatafile = new File(plugin.getDataFolder(), "data.yml");
		datafile = adatafile;
		if (!datafile.exists()) {
			try {
				datafile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	protected void save() {
		YamlConfiguration data = new YamlConfiguration();
		for (String playlistname : options.keySet()) {
			Options option = options.get(playlistname);
			if (option.size > 0 && option.name != null && option.sounds != null) {
				data.set("playlists.".concat(playlistname).concat(".size"), option.size);
				data.set("playlists.".concat(playlistname).concat(".name"), option.name);
				data.set("playlists.".concat(playlistname).concat(".sounds"), option.sounds);
				data.set("playlists.".concat(playlistname).concat(".length"), option.length);
				StringBuilder sb = new StringBuilder();
				for (byte b : option.sha1) {
					int value = b & 0xFF;
					if (value < 16) {
						sb.append("0");
					}
					sb.append(Integer.toHexString(value).toUpperCase());
				}
				data.set("playlists.".concat(playlistname).concat(".sha1"), sb.toString());
			}
		}
		try {
			data.save(datafile);
		} catch (IOException e) {
		}
	}

	protected void load() {
		options.clear();
		YamlConfiguration data = YamlConfiguration.loadConfiguration(datafile);
		ConfigurationSection playlists = data.getConfigurationSection("playlists");
		if (playlists == null) {
			return;
		}
		for (String playlistname : playlists.getKeys(false)) {
			if (data.isInt("playlists.".concat(playlistname).concat(".size"))
					&& data.isString("playlists.".concat(playlistname).concat(".name"))
					&& data.isList("playlists.".concat(playlistname).concat(".sounds"))) {
				try {
					List<Short> lengths = new ArrayList<Short>();
					for (int length : data.getIntegerList("playlists.".concat(playlistname).concat(".length"))) {
						lengths.add((short) length);
					}
					String sha1s = data.getString("playlists.".concat(playlistname).concat(".sha1"));
					if (!sha1s.matches("[0-9a-fA-F]+")) {
						continue;
					}
					byte[] sha1 = new byte[20];
					for (byte i = 0, j = 0; i < 20; j = (byte) (++i << 1)) {
						sha1[i] = (byte) ((Character.digit(sha1s.charAt(j), 16) << 4)
								| Character.digit(sha1s.charAt(j + 1), 16));
					}
					Options option = new Options(data.getInt("playlists.".concat(playlistname).concat(".size")),
							data.getString("playlists.".concat(playlistname).concat(".name")),
							data.getStringList("playlists.".concat(playlistname).concat(".sounds")), lengths, sha1);
					options.put(playlistname, option);
				} catch (IllegalArgumentException e) {
				}
			}
		}
	}

	private static byte[] calcSHA1(File file) {
		try {
			FileInputStream fileInputStream = new FileInputStream(file);
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			DigestInputStream digestInputStream = new DigestInputStream(fileInputStream, digest);
			byte[] bytes = new byte[1024];
			while (digestInputStream.read(bytes) > 0);
			digestInputStream.close();
			return digest.digest();
		} catch (IOException | NoSuchAlgorithmException e) {
		}
		return null;
	}

	protected byte[] setPlaylist(String playlistname, List<String> sounds, List<Short> length, File file) {
		byte[] sha1 = calcSHA1(file);
		options.put(playlistname, new Options((int) file.length(), file.getName(), sounds, length, sha1));
		return sha1;
	}

	protected Options getPlaylist(String playlistname) {
		return options.get(playlistname);
	}

	protected boolean containsPlaylist(String playlistname) {
		return options.containsKey(playlistname);
	}

	protected void removePlaylist(String playlistname) {
		options.remove(playlistname);
	}

	protected Set<String> getPlaylists() {
		return options == null ? null : options.keySet();
	}

	protected class Options {
		protected final int size;
		protected final String name;
		protected final List<String> sounds;
		protected final List<Short> length;
		protected final byte[] sha1;

		private Options(int size, String name, List<String> sounds, List<Short> length, byte[] sha1) throws IllegalArgumentException {
			if (size < 0 || name == null || sounds == null || length == null || sha1 == null || sounds.size() != length.size() || sha1.length != 20) {
				throw new IllegalArgumentException();
			}
			this.size = size;
			this.name = name;
			this.sounds = sounds;
			this.length = length;
			this.sha1 = sha1;
		}

		protected boolean check(File file) {
			if (file.length() != size) {
				return false;
			}
			byte[] filesha1 = calcSHA1(file);
			for (byte i = 0; i < 20; ++i) {
				if (sha1[i] != filesha1[i]) {
					return false;
				}
			}
			return true;
		}
	}
}
