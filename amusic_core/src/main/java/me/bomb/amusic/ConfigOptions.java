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
	
	public final String host, uploaderhost;
	public final InetAddress ip, uploaderip;
	public final int port, uploaderport, backlog, uploaderbacklog, maxpacksize, maxmusicfilesize, bitrate, samplingrate, uploadertimeout, uploaderlimit;
	public final byte channels;
	public final boolean processpack, servercache, clientcache, strictdownloaderlist, useconverter, useuploader, encodetracksasynchronly, waitacception;
	public final File ffmpegbinary, musicdir, packeddir;
	protected final byte[] tokensalt;
	
	/**
	 * Custom configuration storage.
	 */
	public ConfigOptions(String host, InetAddress ip, String uploaderhost, InetAddress uploaderip, int uploadertimeout, int uploaderlimit, int port, int uploaderport, int backlog, int uploaderbacklog, int maxpacksize, int maxmusicfilesize, int bitrate, int samplingrate, byte channels, boolean processpack, boolean servercache, boolean clientcache, boolean strictdownloaderlist, boolean useconverter, boolean useuploader, boolean encodetracksasynchronly, File ffmpegbinary, File musicdir, File packeddir, File tempdir, byte[] tokensalt, boolean waitacception) {
		this.host = host;
		this.ip = ip;
		this.uploaderhost = uploaderhost;
		this.uploaderip = uploaderip;
		this.uploadertimeout = uploadertimeout;
		this.uploaderlimit = uploaderlimit;
		this.port = port;
		this.uploaderport = uploaderport;
		this.backlog = backlog;
		this.uploaderbacklog = uploaderbacklog;
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
		this.useuploader = useuploader;
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
		
		host = sc.getStringOrDefault("server\0host", "http://127.0.0.1:25530/");
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
		
		this.useuploader = sc.getBooleanOrDefault("uploaderserver\0use", false);

		uploaderhost = sc.getStringOrDefault("uploaderserver\0host", "http://127.0.0.1:25532/");
		InetAddress uploaderip = null;
		String uploaderipstr = sc.getStringOrDefault("uploaderserver\0ip", null);
		if(uploaderipstr!=null) {
			try {
				uploaderip = InetAddress.getByName(uploaderipstr);
			} catch (UnknownHostException e) {
			}
		}
		
		this.uploaderip = uploaderip;
		uploaderport = sc.getIntOrDefault("uploaderserver\0port", 25532);
		uploaderbacklog = sc.getIntOrDefault("uploaderserver\0backlog", 50);
		uploadertimeout = sc.getIntOrDefault("uploaderserver\0timeout", 600000);
		uploaderlimit = sc.getIntOrDefault("uploaderserver\0limit", 262144000);
		
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
