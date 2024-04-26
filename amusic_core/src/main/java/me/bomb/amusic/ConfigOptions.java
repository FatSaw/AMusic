package me.bomb.amusic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public final class ConfigOptions {
	
	public final String host;
	public final int port, maxpacksize, maxmusicfilesize, bitrate, samplingrate;
	public final byte channels;
	public final boolean processpack, servercache, clientcache, strictdownloaderlist, useconverter, encodetracksasynchronly, hasplaceholderapi;
	public final File musicdir, packeddir, tempdir;
	public final byte[] tokensalt;
	
	/**
	 * Custom configuration storage.
	 */
	public ConfigOptions(String host, int port, int maxpacksize, int maxmusicfilesize, int bitrate, int samplingrate, byte channels, boolean processpack, boolean servercache, boolean clientcache, boolean strictdownloaderlist, boolean useconverter, boolean encodetracksasynchronly, boolean hasplaceholderapi, File musicdir, File packeddir, File tempdir, byte[] tokensalt) {
		this.host = host;
		this.port = port;
		this.maxpacksize = maxpacksize;
		this.maxmusicfilesize = maxmusicfilesize;
		this.bitrate = bitrate;
		this.samplingrate = samplingrate;
		this.channels = channels;
		this.processpack = processpack;
		this.servercache = servercache;
		this.clientcache = clientcache;
		this.strictdownloaderlist = strictdownloaderlist;
		this.useconverter = useconverter;
		this.encodetracksasynchronly = encodetracksasynchronly;
		this.hasplaceholderapi = hasplaceholderapi;
		this.musicdir = musicdir;
		this.packeddir = packeddir;
		this.tempdir = tempdir;
		this.tokensalt = tokensalt;
	}
	
	/**
	 * Config file configuration storage.
	 */
	public ConfigOptions(File configfile, int maxpacksize, File musicdir, File packeddir, File tempdir) {
		byte[] bytes = null;
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
		this.maxpacksize = maxpacksize;
		this.maxmusicfilesize = maxpacksize;
		this.musicdir = musicdir;
		this.packeddir = packeddir;
		this.tempdir = tempdir;
	}
}
