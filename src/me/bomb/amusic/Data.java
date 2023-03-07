package me.bomb.amusic;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

class Data {
	private static final File datafile;
	private Map<String,Options> options = new HashMap<String,Options>();
	static {
		JavaPlugin plugin = JavaPlugin.getPlugin(AMusic.class);
		File adatafile = new File(plugin.getDataFolder().getPath().concat(File.separator).concat("data.yml"));
		datafile = adatafile;
		if(!datafile.exists()) {
			try {
				datafile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	protected void save() {
		YamlConfiguration data = new YamlConfiguration();
		for(String playlistname : options.keySet()) {
			Options option = options.get(playlistname);
			if(option.size>0&&option.name!=null&&option.sounds!=null) {
				data.set("playlists.".concat(playlistname).concat(".size"), option.size);
				data.set("playlists.".concat(playlistname).concat(".name"), option.name);
				data.set("playlists.".concat(playlistname).concat(".sounds"), option.sounds);
				data.set("playlists.".concat(playlistname).concat(".length"), option.length);
				data.set("playlists.".concat(playlistname).concat(".sha1"), option.sha1);
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
		if(playlists==null) {
			return;
		}
		for(String playlistname : playlists.getKeys(false)) {
			if(data.isInt("playlists.".concat(playlistname).concat(".size")) && data.isString("playlists.".concat(playlistname).concat(".name")) && data.isList("playlists.".concat(playlistname).concat(".sounds"))) {
				Options option = new Options(data.getInt("playlists.".concat(playlistname).concat(".size")), data.getString("playlists.".concat(playlistname).concat(".name")),data.getStringList("playlists.".concat(playlistname).concat(".sounds")),data.getIntegerList("playlists.".concat(playlistname).concat(".length")),data.getString("playlists.".concat(playlistname).concat(".sha1")));
				options.put(playlistname, option);
			}
		}
	}
	protected void setPlaylist(String playlistname,int size,String name,List<String> sounds,List<Integer> length,String sha1) {
		options.put(playlistname,new Options(size, name, sounds, length, sha1));
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
		return options==null?null:options.keySet();
	}
	protected class Options {
		protected final int size;
		protected final String name;
		protected final List<String> sounds;
		protected final List<Integer> length; 
		protected final String sha1;
		private Options(int size,String name,List<String> sounds,List<Integer> length,String sha1) {
			this.size = size;
			this.name = name;
			this.sounds = sounds;
			this.length = length;
			this.sha1 = sha1;
		}
	}
}
