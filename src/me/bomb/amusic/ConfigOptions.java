package me.bomb.amusic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

public class ConfigOptions {
	public static final String host;
	public static final int port;
	public static final boolean useconverter;
    public static final Path ffmpegbinary;
    protected static final EncodingAttributes encodingattributes;
    protected static final boolean encodetracksasynchronly;
    public static final Path packpath;
    public static final Path musicpath;
    public static final Path packedpath;
    public static final Path temppath;
	static {
		JavaPlugin plugin = JavaPlugin.getPlugin(AMusic.class);
		YamlConfiguration aconfig = null; 
		File configfile = new File(plugin.getDataFolder().getPath().concat(File.separator).concat("config.yml"));
		if(!configfile.exists()) {
			try {
				byte[] buf = new byte[256];
				InputStream in = plugin.getResource("config.yml");
				if (in!=null) {
					buf = Arrays.copyOf(buf, in.read(buf));
					in.close();
					OutputStream out = new FileOutputStream(configfile);
					if(out!=null) {
						out.write(buf);
						out.close();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		aconfig = YamlConfiguration.loadConfiguration(configfile);
		host = aconfig.getString("host","127.0.0.1");
		port = aconfig.getInt("port",25530);
		useconverter = aconfig.getConfigurationSection("encoder")!=null; 
	    String os = System.getProperty("os.name").toLowerCase();
		ffmpegbinary = useconverter?Paths.get(aconfig.getString("encoder.ffmpegbinarypath",plugin.getDataFolder().getPath().concat(File.separator).concat("ffmpeg").concat(os.contains("windows")?".exe":os.contains("mac")?"-osx":""))):null;
		AudioAttributes audio = new AudioAttributes();          
		audio.setCodec("libvorbis");                                     
	 	audio.setBitRate(aconfig.getInt("encoder.bitrate",128000));                                            
	 	audio.setChannels(aconfig.getInt("encoder.channels",2));                                                
	 	audio.setSamplingRate(aconfig.getInt("encoder.samplingrate",44100));
		EncodingAttributes attrs = new EncodingAttributes();
	 	attrs.setOutputFormat("ogg");
	 	attrs.setAudioAttributes(audio); 
		encodingattributes = useconverter?attrs:null;
		encodetracksasynchronly = useconverter?aconfig.getBoolean("encoder.async", true):false;
		packpath = Paths.get(plugin.getDataFolder().getPath().concat(File.separator).concat("Pack"));
		musicpath = Paths.get(plugin.getDataFolder().getPath().concat(File.separator).concat("Music"));
	    packedpath = Paths.get(plugin.getDataFolder().getPath().concat(File.separator).concat("Packed"));
	    temppath = Paths.get(plugin.getDataFolder().getPath().concat(File.separator).concat("Temp"));
	    try {
        	if(Files.notExists(musicpath)) Files.createDirectories(musicpath);
		} catch (IOException e1) {
		}
	    try {
	    	if(Files.notExists(packedpath)) Files.createDirectories(packedpath);
		} catch (IOException e1) {
		}
	    try {
	    	if(Files.notExists(temppath)) Files.createDirectories(temppath);
		} catch (IOException e1) {
		}
	}
}