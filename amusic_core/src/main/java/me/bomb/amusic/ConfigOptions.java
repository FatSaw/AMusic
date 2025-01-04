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
	
	public final String loaderrors, host, uploaderhost;
	public final InetAddress ip, uploaderip, remotelocalip, remoteip;
	public final int port, uploaderport, remoteport, backlog, uploaderbacklog, remotebacklog, maxpacksize, maxmusicfilesize, bitrate, samplingrate, uploadertimeout, uploaderlimit;
	public final byte channels;
	public final boolean processpack, servercache, clientcache, resourcestrictaccess, uploaderstrictaccess, useconverter, useuploader, useremote, encodetracksasynchronly, waitacception;
	public final File ffmpegbinary, musicdir, packeddir;
	protected final byte[] tokensalt;
	
	/**
	 * Custom configuration storage.
	 */
	public ConfigOptions(String host, InetAddress ip, String uploaderhost, InetAddress uploaderip, InetAddress remotelocalip, InetAddress remoteip, int uploadertimeout, int uploaderlimit, int port, int uploaderport, int remoteport, int backlog, int uploaderbacklog, int remotebacklog, int maxpacksize, int maxmusicfilesize, int bitrate, int samplingrate, byte channels, boolean processpack, boolean servercache, boolean clientcache, boolean strictdownloaderlist, boolean uploaderstrictdownloaderlist, boolean useconverter, boolean useuploader, boolean useremote, boolean encodetracksasynchronly, File ffmpegbinary, File musicdir, File packeddir, byte[] tokensalt, boolean waitacception) {
		this.host = host;
		this.ip = ip;
		this.uploaderhost = uploaderhost;
		this.uploaderip = uploaderip;
		this.remotelocalip = remotelocalip;
		this.remoteip = remoteip;
		this.uploadertimeout = uploadertimeout;
		this.uploaderlimit = uploaderlimit;
		this.port = port;
		this.uploaderport = uploaderport;
		this.remoteport = remoteport;
		this.backlog = backlog;
		this.uploaderbacklog = uploaderbacklog;
		this.remotebacklog = remotebacklog;
		this.maxpacksize = maxpacksize;
		this.maxmusicfilesize = maxmusicfilesize;
		this.bitrate = bitrate;
		this.samplingrate = samplingrate;
		this.channels = channels;
		this.processpack = processpack;
		this.servercache = servercache;
		this.clientcache = clientcache;
		this.resourcestrictaccess = strictdownloaderlist;
		this.uploaderstrictaccess = uploaderstrictdownloaderlist;
		this.useconverter = useconverter;
		this.useuploader = useuploader;
		this.useremote = useremote;
		this.encodetracksasynchronly = encodetracksasynchronly;
		this.ffmpegbinary = ffmpegbinary;
		this.musicdir = musicdir;
		this.packeddir = packeddir;
		this.tokensalt = tokensalt;
		this.waitacception = waitacception;
		this.loaderrors = "";
	}
	
	/**
	 * Config file configuration storage.
	 */
	public ConfigOptions(File configfile, int maxpacksize, File musicdir, File packeddir, boolean waitacception) {
		byte[] bytes = null;
		if (!configfile.exists()) {
			InputStream is = ConfigOptions.class.getClassLoader().getResourceAsStream("config.yml");
			try {
				bytes = new byte[0x0400];
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
		StringBuilder errors = new StringBuilder();
		host = sc.getStringOrError("server\0host", errors);
		InetAddress ip = null;
		String ipstr = sc.getStringOrDefault("server\0ip", null);
		if(ipstr!=null) {
			try {
				ip = InetAddress.getByName(ipstr);
			} catch (UnknownHostException e) {
			}
		}
		
		this.ip = ip;
		port = sc.getIntOrError("server\0port", errors);
		backlog = sc.getIntOrError("server\0backlog", errors);
		
		this.useuploader = sc.getBooleanOrError("uploaderserver\0use", errors);

		uploaderhost = sc.getStringOrError("uploaderserver\0host", errors);
		InetAddress uploaderip = null;
		String uploaderipstr = sc.getStringOrDefault("uploaderserver\0ip", null);
		if(uploaderipstr!=null) {
			try {
				uploaderip = InetAddress.getByName(uploaderipstr);
			} catch (UnknownHostException e) {
			}
		}
		
		this.uploaderip = uploaderip;
		uploaderport = sc.getIntOrError("uploaderserver\0port", errors);
		uploaderbacklog = sc.getIntOrError("uploaderserver\0backlog", errors);
		

		this.useremote = sc.getBooleanOrError("remote\0use", errors);
		InetAddress remotelocalip = null;
		String remotelocalipstr = sc.getStringOrDefault("remote\0localip", null);
		if(remotelocalipstr!=null) {
			try {
				remotelocalip = InetAddress.getByName(remotelocalipstr);
			} catch (UnknownHostException e) {
			}
		}
		this.remotelocalip = remotelocalip;
		InetAddress remoteip = null;
		String remoteipstr = sc.getStringOrDefault("remote\0ip", null);
		if(remoteipstr!=null) {
			try {
				remoteip = InetAddress.getByName(remoteipstr);
			} catch (UnknownHostException e) {
			}
		}
		this.remoteip = remoteip;
		remoteport = sc.getIntOrError("remote\0port", errors);
		remotebacklog = sc.getIntOrError("remote\0backlog", errors);
		
		
		uploadertimeout = sc.getIntOrError("uploaderserver\0timeout", errors);
		uploaderlimit = sc.getIntOrError("uploaderserver\0limit", errors);
		
		processpack = sc.getBooleanOrError("resource\0processpack", errors);
		servercache = sc.getBooleanOrError("resource\0cache\0server", errors);
		clientcache = sc.getBooleanOrError("resource\0cache\0client", errors);
		resourcestrictaccess = sc.getBooleanOrError("server\0strictaccess", errors);
		uploaderstrictaccess = sc.getBooleanOrError("uploaderserver\0strictaccess", errors);
		byte[] salt = sc.getBytesBase64OrError("server\0tokensalt", errors);
		
		if(salt == null || salt.length < 2) {
			tokensalt = null;
		} else {
			tokensalt = salt;
		}
		useconverter = sc.getBooleanOrError("resource\0encoder\0use", errors);
		String ffmpegbinartypath = sc.getStringOrDefault("resource\0encoder\0ffmpegbinary", null);
		ffmpegbinary = ffmpegbinartypath == null ? null : new File(ffmpegbinartypath);
		bitrate = sc.getIntOrError("resource\0encoder\0bitrate", errors);
		channels = (byte) sc.getIntOrError("resource\0encoder\0channels", errors);
		samplingrate = sc.getIntOrError("resource\0encoder\0samplingrate", errors);
		encodetracksasynchronly = sc.getBooleanOrError("resource\0encoder\0async", errors);
		waitacception = sc.getBooleanOrDefault("server\0waitacception", waitacception);
		this.maxpacksize = maxpacksize;
		this.maxmusicfilesize = maxpacksize;
		this.musicdir = musicdir;
		this.packeddir = packeddir;
		this.waitacception = waitacception;
		this.loaderrors = errors.toString();
	}
}
