package me.bomb.amusic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import me.bomb.amusic.util.SimpleConfiguration;

public final class ConfigOptions {
	
	public final String host;
	public final InetAddress ip;
	public final int port, backlog, maxpacksize, maxmusicfilesize, bitrate, samplingrate;
	public final byte channels;
	public final boolean processpack, servercache, clientcache, strictdownloaderlist, useconverter, encodetracksasynchronly, waitacception;
	public final File ffmpegbinary, musicdir, packeddir;
	protected final byte[] tokensalt;
	
	/**
	 * Custom configuration storage.
	 */
	public ConfigOptions(String host, InetAddress ip, int port, int backlog, int maxpacksize, int maxmusicfilesize, int bitrate, int samplingrate, byte channels, boolean processpack, boolean servercache, boolean clientcache, boolean strictdownloaderlist, boolean useconverter, boolean encodetracksasynchronly, File ffmpegbinary, File musicdir, File packeddir, File tempdir, byte[] tokensalt, boolean waitacception) {
		this.host = host;
		this.ip = ip;
		this.port = port;
		this.backlog = backlog;
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
		this.ffmpegbinary = ffmpegbinary;
		this.musicdir = musicdir;
		this.packeddir = packeddir;
		this.tokensalt = tokensalt;
		this.waitacception = waitacception;
	}
	
	/**
	 * Config file configuration storage.
	 */
	public ConfigOptions(File configfile, int maxpacksize, File musicdir, File packeddir, boolean waitacception) {
		byte[] bytes = null;
		if (!configfile.exists()) {
			InputStream is = ConfigOptions.class.getClassLoader().getResourceAsStream("config.yml");
			try {
				bytes = new byte[0x0200];
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
				long filesize = configfile.length();
				if(filesize > 0x00010000) {
					filesize = 0x00010000;
				}
				bytes = new byte[(int) filesize];
				int size = is.read(bytes);
				if(size < filesize) {
					bytes = Arrays.copyOf(bytes, size);
				}
				is.close();
			} catch (IOException e) {
			}
		}
		SimpleConfiguration sc = new SimpleConfiguration(bytes);
		bytes = null;
		
		host = sc.getStringOrDefault("server\0host", "127.0.0.1:25530");
		InetAddress ip = null;
		String ipstr = sc.getStringOrDefault("server\0ip", null);
		if(ipstr!=null) {
			try {
				ip = InetAddress.getByName(ipstr);
			} catch (UnknownHostException e) {
			}
		}
		
		this.ip = ip;
		port = sc.getIntOrDefault("server\0port", 25530);
		backlog = sc.getIntOrDefault("server\0backlog", 50);
		processpack = sc.getBooleanOrDefault("resource\0processpack", true);
		servercache = sc.getBooleanOrDefault("resource\0cache\0server", true);
		clientcache = sc.getBooleanOrDefault("resource\0cache\0client", true);
		strictdownloaderlist = sc.getBooleanOrDefault("server\0strictdownloaderlist", true);
		
		byte[] salt = sc.getBytesBase64OrDefault("server\0tokensalt", new byte[0]);
		
		if(salt == null || salt.length < 2) {
			tokensalt = null;
		} else {
			tokensalt = salt;
		}
		useconverter = sc.getBooleanOrDefault("resource\0encoder\0use", false);
		String ffmpegbinartypath = sc.getStringOrDefault("resource\0encoder\0ffmpegbinary", null);
		ffmpegbinary = ffmpegbinartypath == null ? null : new File(ffmpegbinartypath);
		bitrate = sc.getIntOrDefault("resource\0encoder\0bitrate", 65000);
		channels = (byte) sc.getIntOrDefault("resource\0encoder\0channels", 2);
		samplingrate = sc.getIntOrDefault("resource\0encoder\0samplingrate", 44100);
		encodetracksasynchronly = sc.getBooleanOrDefault("resource\0encoder\0async", true);
		waitacception = sc.getBooleanOrDefault("server\0waitacception", waitacception);
		this.maxpacksize = maxpacksize;
		this.maxmusicfilesize = maxpacksize;
		this.musicdir = musicdir;
		this.packeddir = packeddir;
		this.waitacception = waitacception;
	}
}
