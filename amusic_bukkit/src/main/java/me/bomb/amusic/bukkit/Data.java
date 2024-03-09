package me.bomb.amusic.bukkit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import me.bomb.amusic.DataEntry;

public final class Data extends me.bomb.amusic.Data {
	
	private final File datafile;
	
	protected Data(File datafile) {
		super();
		this.datafile = datafile;
	}
	
	@Override
	protected void save() {
		YamlConfiguration data = new YamlConfiguration();
		for (String playlistname : options.keySet()) {
			DataEntry option = options.get(playlistname);
			if (option.size > 0 && option.name != null && option.sounds != null) {
				playlistname = "playlists.".concat(playlistname);
				data.set(playlistname.concat(".size"), option.size);
				data.set(playlistname.concat(".name"), option.name);
				data.set(playlistname.concat(".sounds"), option.sounds);
				data.set(playlistname.concat(".length"), option.length);
				StringBuilder sb = new StringBuilder();
				for (byte b : option.sha1) {
					int value = b & 0xFF;
					if (value < 16) {
						sb.append("0");
					}
					sb.append(Integer.toHexString(value).toUpperCase());
				}
				data.set(playlistname.concat(".sha1"), sb.toString());
			}
		}
		try {
			data.save(datafile);
		} catch (IOException e) {
		}
	}

	@Override
	protected void load() {
		options.clear();
		YamlConfiguration data = YamlConfiguration.loadConfiguration(datafile);
		ConfigurationSection playlists = data.getConfigurationSection("playlists");
		if (playlists == null) {
			return;
		}
		for (String playlistname : playlists.getKeys(false)) {
			String aplaylistname = "playlists.".concat(playlistname);
			if (data.isInt(aplaylistname.concat(".size"))
					&& data.isString(aplaylistname.concat(".name"))
					&& data.isList(aplaylistname.concat(".sounds"))) {
				try {
					List<Short> lengths = new ArrayList<Short>();
					for (int length : data.getIntegerList(aplaylistname.concat(".length"))) {
						lengths.add((short) length);
					}
					String sha1s = data.getString(aplaylistname.concat(".sha1"));
					if (!sha1s.matches("[0-9a-fA-F]+")) {
						continue;
					}
					byte[] sha1 = new byte[20];
					for (byte i = 0, j = 0; i < 20; j = (byte) (++i << 1)) {
						sha1[i] = (byte) ((Character.digit(sha1s.charAt(j), 16) << 4)
								| Character.digit(sha1s.charAt(j + 1), 16));
					}
					DataEntry option = new DataEntry(data.getInt(aplaylistname.concat(".size")),
							data.getString(aplaylistname.concat(".name")),
							data.getStringList(aplaylistname.concat(".sounds")), lengths, sha1);
					options.put(playlistname, option);
				} catch (IllegalArgumentException e) {
				}
			}
		}
	}
	
}
