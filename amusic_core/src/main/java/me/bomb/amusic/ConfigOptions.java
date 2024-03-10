package me.bomb.amusic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public final class ConfigOptions {
	protected static Class<? extends ConfigOptions> oclass;
	protected static File plugindir;
	protected static byte version;
	public final String host;
	public final int port, maxpacksize, maxmusicfilesize, bitrate, samplingrate;
	public final byte channels;
	public final boolean processpack, servercache, clientcache, strictdownloaderlist, useconverter, encodetracksasynchronly, hasplaceholderapi, legacystopper, legacysender;
	public final File musicdir, packeddir, tempdir;
	public final byte[] tokensalt;
	public ConfigOptions(File plugindir, byte version) {
		ConfigOptions.plugindir = plugindir;
		ConfigOptions.version = version;
		if(!plugindir.exists()) {
			plugindir.mkdirs();
		}
		musicdir = new File(plugindir, "Music");
		packeddir = new File(plugindir, "Packed");
		tempdir = new File(plugindir, "Temp");
		if(!musicdir.exists()) {
			musicdir.mkdir();
		}
		if(!packeddir.exists()) {
			packeddir.mkdir();
		}
		if(!tempdir.exists()) {
			tempdir.mkdir();
		}
		File configfile = new File(plugindir, "config.yml");
		byte[] bytes = null;
		oclass = getClass();
		if (!configfile.exists()) {
			InputStream is = ConfigOptions.class.getClassLoader().getResourceAsStream("config.yml");
			try {
				bytes = new byte[512];
				bytes = Arrays.copyOf(bytes, is.read(bytes));
			} catch (IOException e) {
			}
			try {
				is.close();
			} catch (IOException e) {
			}
			try {
				FileOutputStream fos = new FileOutputStream(configfile);
				fos.write(bytes);
				fos.close();
			} catch (IOException e) {
			}
		} else {
			try {
				InputStream is = new FileInputStream(configfile);
				bytes = new byte[is.available()];
				is.read(bytes);
				is.close();
			} catch (IOException e) {
			}
		}
		SimpleConfiguration sc = new SimpleConfiguration(bytes);
		bytes = null;
		host = sc.getStringOrDefault("host", "127.0.0.1");
		port = sc.getIntOrDefault("port", 25530);
		processpack = sc.getBooleanOrDefault("processpack", true);
		servercache = sc.getBooleanOrDefault("cache.server", true);
		clientcache = sc.getBooleanOrDefault("cache.client", true);
		strictdownloaderlist = sc.getBooleanOrDefault("strictdownloaderlist", true);
		hasplaceholderapi = sc.getBooleanOrDefault("useplaceholderapi", false);
		
		byte[] salt = sc.getBytesBase64OrDefault("tokensalt", new byte[0]);
		
		if(salt == null || salt.length < 2) {
			tokensalt = null;
		} else {
			tokensalt = salt;
		}
		useconverter = sc.getBooleanOrDefault("encoder.use", false);
		bitrate = sc.getIntOrDefault("encoder.bitrate", 65000);
		channels = (byte) sc.getIntOrDefault("encoder.channels", 2);
		samplingrate = sc.getIntOrDefault("encoder.samplingrate", 44100);
		encodetracksasynchronly = sc.getBooleanOrDefault("encoder.async", true);
		
		maxpacksize = version < 15 ? 52428800 : version < 18 ? 104857600 : 262144000;
		maxmusicfilesize = maxpacksize;
		legacystopper = version < 9;
		legacysender = version < 11;
		
	}
}
