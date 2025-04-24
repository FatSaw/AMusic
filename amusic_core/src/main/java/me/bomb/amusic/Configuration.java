package me.bomb.amusic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.FileSystem;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import me.bomb.amusic.http.SimpleServerSocketFactory;
import me.bomb.amusic.http.SimpleSocketFactory;
import me.bomb.amusic.util.SimpleConfiguration;

public final class Configuration {
	
	public final String errors;
	public final Path musicdir, packeddir;
	
	public final boolean use, usecmd, uploaduse, connectuse, encoderuse, uploadhttps, connecttls;
	
	public final String uploadhost, sendpackhost;
	public final InetAddress sendpackifip, uploadifip, connectifip, connectremoteip;
	public final int sendpackport, uploadport, connectport;
	public final int sendpackbacklog, uploadbacklog, connectbacklog;
	public final int sendpacktimeout, uploadtimeout;
	
	public final boolean uploadstrictaccess, sendpackstrictaccess;
	
	public final Path encoderbinary;
	
	public final boolean processpack, servercache, clientcache, waitacception;
	public final int uploadlifetime, uploadlimitsize, uploadlimitcount, packsizelimit;
	public final short packthreadlimitcount;
	public final float packthreadcoefficient;
	
	public final byte encoderchannels;
	public final int encoderbitrate, encodersamplingrate;
	
	protected final byte[] tokensalt;
	protected final ServerSocketFactory sendpackserverfactory, uploadserverfactory, connectserverfactory;
	protected final SocketFactory connectsocketfactory;
	
	public Configuration(Path musicdir, Path packeddir, boolean usecmd, boolean uploaduse, boolean sendpackuse, boolean connectuse, boolean encoderuse, boolean uploadhttps, boolean connecthttps, String uploadhost, String sendpackhost, InetAddress sendpackifip, InetAddress uploadifip, InetAddress connectifip, InetAddress connectremoteip, int sendpackport, int uploadport, int connectport, int sendpackbacklog, int uploadbacklog, int connectbacklog, int sendpacktimeout, int uploadtimeout, boolean uploadstrictaccess, boolean sendpackstrictaccess, Path encoderbinary, boolean processpack, boolean servercache, boolean clientcache, boolean waitacception, int uploadlifetime, int uploadlimitsize, int uploadlimitcount, int packsizelimit, short packthreadlimitcount, float packthreadcoefficient, byte encoderchannels, int encoderbitrate, int encodersamplingrate, byte[] tokensalt, ServerSocketFactory sendpackserverfactory, ServerSocketFactory uploadserverfactory, ServerSocketFactory connectserverfactory, SocketFactory connectsocketfactory) {
		this.errors = new String();
		this.use = true;
		this.musicdir = musicdir;
		this.packeddir = packeddir;
		this.usecmd = usecmd;
		this.uploaduse = uploaduse;
		this.connectuse = connectuse;
		this.encoderuse = encoderuse;
		this.uploadhttps = uploadhttps;
		this.connecttls = connecthttps;
		this.uploadhost = uploadhost;
		this.sendpackhost = sendpackhost;
		this.sendpackifip = sendpackifip;
		this.uploadifip = uploadifip;
		this.connectifip = connectifip;
		this.connectremoteip = connectremoteip;
		this.sendpackport = sendpackport;
		this.uploadport = uploadport;
		this.connectport = connectport;
		this.sendpackbacklog = sendpackbacklog;
		this.uploadbacklog = uploadbacklog;
		this.connectbacklog = connectbacklog;
		this.sendpacktimeout = sendpacktimeout;
		this.uploadtimeout = uploadtimeout;
		this.uploadstrictaccess = uploadstrictaccess;
		this.sendpackstrictaccess = sendpackstrictaccess;
		this.encoderbinary = encoderbinary;
		this.processpack = processpack;
		this.servercache = servercache;
		this.clientcache = clientcache;
		this.waitacception = waitacception;
		this.uploadlifetime = uploadlifetime;
		this.uploadlimitsize = uploadlimitsize;
		this.uploadlimitcount = uploadlimitcount;
		this.packsizelimit = packsizelimit;
		this.packthreadlimitcount = packthreadlimitcount;
		this.packthreadcoefficient = packthreadcoefficient;
		this.encoderchannels = encoderchannels;
		this.encoderbitrate = encoderbitrate;
		this.encodersamplingrate = encodersamplingrate;
		this.tokensalt = tokensalt;
		this.sendpackserverfactory = sendpackserverfactory;
		this.uploadserverfactory = uploadserverfactory;
		this.connectserverfactory = connectserverfactory;
		this.connectsocketfactory = connectsocketfactory;
	}
	
	public Configuration(FileSystem fs, final Path configfile, final Path musicdir, final Path packeddir, final boolean defaultwaitacception, final boolean defaultremoteclient) {
		byte[] bytes = null;
		final StringBuilder errors = new StringBuilder();
		InputStream is = null;
		FileSystemProvider fsp = fs.provider();
		try {
			BasicFileAttributes attributes = fsp.readAttributes(configfile, BasicFileAttributes.class);
			is = fsp.newInputStream(configfile);
			long filesize = attributes.size();
			if(filesize > 0x00010000) {
				filesize = 0x00010000;
			}
			bytes = new byte[(int)filesize];
			int size = is.read(bytes);
			if(size < filesize) {
				bytes = Arrays.copyOf(bytes, size);
			}
			is.close();
		} catch (IOException e1) {
			if(is != null) {
				try {
					is.close();
				} catch (IOException e2) {
				}
			}
			try {
				is = Configuration.class.getClassLoader().getResourceAsStream("config.yml");
				bytes = new byte[0x1000];
				bytes = Arrays.copyOf(bytes, is.read(bytes));
				is.close();
				OutputStream os = null;
				try {
					os = fsp.newOutputStream(configfile);
					os.write(bytes);
					os.close();
				} catch (IOException e3) {
					appendError("Filed to write default config", errors);
					if(os != null) {
						try {
							os.close();
						} catch (IOException e4) {
						}
					}
				}
			} catch (IOException e3) {
				appendError("Filed to read default config", errors);
				if(is != null) {
					try {
						is.close();
					} catch (IOException e4) {
					}
				}
			}
		}
		SimpleConfiguration sc;
		if(bytes != null && (sc = new SimpleConfiguration(bytes)).getBooleanOrError("amusic\0use", errors)) {
			this.use = true;
			this.musicdir = musicdir;
			this.packeddir = packeddir;
			this.usecmd = sc.getBooleanOrError("amusic\0usecmd", errors);
			this.uploaduse = sc.getBooleanOrError("amusic\0server\0upload\0use", errors);
			this.connectuse = sc.getBooleanOrError("amusic\0server\0connect\0use", errors);
			this.encoderuse = sc.getBooleanOrError("amusic\0encoder\0use", errors);
			if(defaultremoteclient && connectuse) {
				this.uploadhost = sc.getStringOrError("amusic\0server\0upload\0host", errors);
				this.uploadhttps = false;
				this.uploadserverfactory = null;
				this.uploadifip = null;
				this.uploadport = 0;
				this.uploadbacklog = 0;
				this.uploadtimeout = 0;
				this.uploadstrictaccess = false;
				this.uploadlifetime = 0;
				this.uploadlimitsize = 0;
				this.uploadlimitcount = 0;
			} else if(this.uploaduse) {
				this.uploadhost = sc.getStringOrError("amusic\0server\0upload\0host", errors);
				this.uploadhttps = sc.getBooleanOrError("amusic\0server\0upload\0https\0use", errors);
				if(uploadhttps) {
					KeyStore keystore = null;
					SSLServerSocketFactory sslserverfactory = null;
					final String uploadercertpath = sc.getStringOrError("amusic\0server\0upload\0https\0path", errors);
					Path certfile = null;
					try {
						certfile = fs.getPath(uploadercertpath);
					} catch (InvalidPathException e) {
						appendError("Filed to read upload https certificate file (path invalid)", errors);
					}
					
					final String certpassword;
					if(certfile != null && (certpassword = sc.getStringOrError("amusic\0server\0upload\0https\0password", errors)) != null) {
						is = null;
						try {
							is = fs.provider().newInputStream(certfile);
						} catch (SecurityException e1) {
							if(is != null) {
								try {
									is.close();
								} catch (IOException e2) {
								}
							}
							appendError("Filed to read upload https certificate file (no permission)", errors);
						} catch (IOException e) {
							appendError("Filed to read upload https certificate file (not found)", errors);
						}
						try {
							keystore = KeyStore.getInstance("PKCS12");
						} catch (KeyStoreException e) {
							keystore = null;
							appendError("Filed to initialize upload https certificate (filed to get PKCS12 instance)", errors);
						}
						try {
							keystore.load(is, certpassword.toCharArray());
						} catch (CertificateException | NoSuchAlgorithmException | IOException e) {
							keystore = null;
							appendError("Filed to initialize upload https certificate", errors);
						}
						try {
							TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
							trustManagerFactory.init(keystore);
							TrustManager[] trustmanagers = trustManagerFactory.getTrustManagers();
							KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
							keyManagerFactory.init(keystore, certpassword.toCharArray());
							KeyManager[] keymanagers = keyManagerFactory.getKeyManagers();
							SSLContext tlscontext = SSLContext.getInstance("TLSv1.2");
							tlscontext.init(keymanagers, trustmanagers, SecureRandom.getInstanceStrong());
							sslserverfactory = tlscontext.getServerSocketFactory();
						} catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyManagementException e) {
							sslserverfactory = null;
						}
					}
					this.uploadserverfactory = sslserverfactory;
				} else {
					this.uploadserverfactory = new SimpleServerSocketFactory();
				}
				
				InetAddress uploadifip = null;
				String uploadipstr = sc.getStringOrError("amusic\0server\0upload\0ip", errors);
				if(uploadipstr!=null) {
					try {
						uploadifip = InetAddress.getByName(uploadipstr);
					} catch (UnknownHostException e) {
						appendError("Filed to get uploader local interface ip", errors);
					}
				}
				this.uploadifip = uploadifip;
				this.uploadport = sc.getIntOrError("amusic\0server\0upload\0port", errors);
				this.uploadbacklog = sc.getIntOrError("amusic\0server\0upload\0backlog", errors);
				this.uploadstrictaccess = sc.getBooleanOrError("amusic\0server\0upload\0strictaccess", errors);
				this.uploadlifetime = sc.getIntOrError("amusic\0server\0upload\0lifetime", errors);
				this.uploadtimeout = sc.getIntOrError("amusic\0server\0upload\0timeout", errors);
				this.uploadlimitsize = sc.getIntOrError("amusic\0server\0upload\0limit\0size", errors);
				this.uploadlimitcount = sc.getIntOrError("amusic\0server\0upload\0limit\0count", errors);
			} else {
				this.uploadhost = null;
				this.uploadhttps = false;
				this.uploadserverfactory = null;
				this.uploadifip = null;
				this.uploadport = 0;
				this.uploadbacklog = 0;
				this.uploadtimeout = 0;
				this.uploadstrictaccess = false;
				this.uploadlifetime = 0;
				this.uploadlimitsize = 0;
				this.uploadlimitcount = 0;
			}
			this.sendpackhost = sc.getStringOrError("amusic\0server\0sendpack\0host", errors);
			InetAddress sendpackifip = null;
			String sendpackipstr = sc.getStringOrError("amusic\0server\0sendpack\0ip", errors);
			if(sendpackipstr!=null) {
				try {
					sendpackifip = InetAddress.getByName(sendpackipstr);
				} catch (UnknownHostException e) {
					appendError("Filed to get sendpack local interface ip", errors);
				}
			}
			this.sendpackifip = sendpackifip;
			this.sendpackport = sc.getIntOrError("amusic\0server\0sendpack\0port", errors);
			this.sendpackbacklog = sc.getIntOrError("amusic\0server\0sendpack\0backlog", errors);
			this.sendpacktimeout = sc.getIntOrError("amusic\0server\0sendpack\0timeout", errors);
			this.sendpackstrictaccess = sc.getBooleanOrError("amusic\0server\0sendpack\0strictaccess", errors);
			this.waitacception = sc.getBooleanOrDefault("amusic\0server\0sendpack\0waitacception", defaultwaitacception);
			this.tokensalt = sc.getBytesBase64OrError("amusic\0server\0sendpack\0tokensalt", errors);
			this.sendpackserverfactory = new SimpleServerSocketFactory();
			if(this.connectuse) {
				InetAddress connectifip = null;
				String connectipstr = sc.getStringOrError("amusic\0server\0connect\0ip", errors);
				if(connectipstr!=null) {
					try {
						connectifip = InetAddress.getByName(connectipstr);
					} catch (UnknownHostException e) {
						appendError("Filed to get connect local interface ip", errors);
					}
				}
				this.connectifip = connectifip;

				this.connecttls = sc.getBooleanOrError("amusic\0server\0connect\0tls\0use", errors);
				
				if(defaultremoteclient) {
					InetAddress connectserverip = null;
					String connectserveripstr = sc.getStringOrError("amusic\0server\0connect\0client\0serverip", errors);
					if(connectserveripstr != null && !connectserveripstr.equals("0.0.0.0")) {
						try {
							connectserverip = InetAddress.getByName(connectserveripstr);
						} catch (UnknownHostException e) {
							appendError("Filed to get connect server ip", errors);
						}
					}
					this.connectremoteip = connectserverip;
					this.connectport = sc.getIntOrError("amusic\0server\0connect\0client\0port", errors);
					this.connectbacklog = 0;
					this.connectserverfactory = null;
					if(connecttls) {
						KeyStore keystore = null;
						SSLSocketFactory sslsocketfactory = null;
						final String connectcertpath = sc.getStringOrError("amusic\0server\0connect\0tls\0path", errors);
						Path certfile = null;
						try {
							certfile = fs.getPath(connectcertpath);
						} catch (InvalidPathException e) {
							appendError("Filed to read connect tls certificate file (path invalid)", errors);
						}
						
						final String certpassword;
						if(certfile != null && (certpassword = sc.getStringOrError("amusic\0server\0connect\0tls\0password", errors)) != null) {
							is = null;
							try {
								is = fs.provider().newInputStream(certfile);
							} catch (SecurityException e1) {
								if(is != null) {
									try {
										is.close();
									} catch (IOException e2) {
									}
								}
								appendError("Filed to read connect tls certificate file (no permission)", errors);
							} catch (IOException e) {
								appendError("Filed to read connect tls certificate file (not found)", errors);
							}
							try {
								keystore = KeyStore.getInstance("PKCS12");
							} catch (KeyStoreException e) {
								keystore = null;
								appendError("Filed to initialize connect tls certificate (filed to get PKCS12 instance)", errors);
							}
							try {
								keystore.load(is, certpassword.toCharArray());
							} catch (CertificateException | NoSuchAlgorithmException | IOException e) {
								keystore = null;
								appendError("Filed to initialize connect tls certificate", errors);
							}
							try {
								TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
								trustManagerFactory.init(keystore);
								TrustManager[] trustmanagers = trustManagerFactory.getTrustManagers();
								KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
								keyManagerFactory.init(keystore, certpassword.toCharArray());
								KeyManager[] keymanagers = keyManagerFactory.getKeyManagers();
								SSLContext tlscontext = SSLContext.getInstance("TLSv1.2");
								tlscontext.init(keymanagers, trustmanagers, SecureRandom.getInstanceStrong());
								sslsocketfactory = tlscontext.getSocketFactory();
							} catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyManagementException e) {
								sslsocketfactory = null;
							}
						}
						this.connectsocketfactory = sslsocketfactory;
					} else {
						this.connectsocketfactory = new SimpleSocketFactory();
					}
				} else {
					InetAddress connectclientip = null;
					String connectclientipstr = sc.getStringOrError("amusic\0server\0connect\0server\0clientip", errors);
					if(connectclientipstr != null && !connectclientipstr.equals("0.0.0.0")) {
						try {
							connectclientip = InetAddress.getByName(connectclientipstr);
						} catch (UnknownHostException e) {
							appendError("Filed to get connect client ip", errors);
						}
					}
					this.connectremoteip = connectclientip;
					this.connectport = sc.getIntOrError("amusic\0server\0connect\0server\0port", errors);
					this.connectbacklog = sc.getIntOrError("amusic\0server\0connect\0server\0backlog", errors);
					this.connectsocketfactory = null;
					if(connecttls) {
						KeyStore keystore = null;
						SSLServerSocketFactory sslserverfactory = null;
						final String connectcertpath = sc.getStringOrError("amusic\0server\0connect\0tls\0path", errors);
						Path certfile = null;
						try {
							certfile = fs.getPath(connectcertpath);
						} catch (InvalidPathException e) {
							appendError("Filed to read connect tls certificate file (path invalid)", errors);
						}
						
						final String certpassword;
						if(certfile != null && (certpassword = sc.getStringOrError("amusic\0server\0connect\0tls\0password", errors)) != null) {
							is = null;
							try {
								is = fs.provider().newInputStream(certfile);
							} catch (SecurityException e1) {
								if(is != null) {
									try {
										is.close();
									} catch (IOException e2) {
									}
								}
								appendError("Filed to read connect tls certificate file (no permission)", errors);
							} catch (IOException e) {
								appendError("Filed to read connect tls certificate file (not found)", errors);
							}
							try {
								keystore = KeyStore.getInstance("PKCS12");
							} catch (KeyStoreException e) {
								keystore = null;
								appendError("Filed to initialize connect tls certificate (filed to get PKCS12 instance)", errors);
							}
							try {
								keystore.load(is, certpassword.toCharArray());
							} catch (CertificateException | NoSuchAlgorithmException | IOException e) {
								keystore = null;
								appendError("Filed to initialize connect tls certificate", errors);
							}
							try {
								TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
								trustManagerFactory.init(keystore);
								TrustManager[] trustmanagers = trustManagerFactory.getTrustManagers();
								KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
								keyManagerFactory.init(keystore, certpassword.toCharArray());
								KeyManager[] keymanagers = keyManagerFactory.getKeyManagers();
								SSLContext tlscontext = SSLContext.getInstance("TLSv1.2");
								tlscontext.init(keymanagers, trustmanagers, SecureRandom.getInstanceStrong());
								sslserverfactory = tlscontext.getServerSocketFactory();
							} catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyManagementException e) {
								sslserverfactory = null;
							}
						}
						this.connectserverfactory = sslserverfactory;
					} else {
						this.connectserverfactory = new SimpleServerSocketFactory();
					}
				}
			} else {
				this.connectifip = null;
				this.connecttls = false;
				this.connectremoteip = null;
				this.connectport = 0;
				this.connectbacklog = 0;
				this.connectserverfactory = null;
				this.connectsocketfactory = null;
			}
			if(this.encoderuse) {
				final String ffmpegpath = sc.getStringOrError("amusic\0encoder\0path", errors);
				Path ffmpegfile = null;
				try {
					ffmpegfile = fs.getPath(ffmpegpath);
				} catch (InvalidPathException e) {
					appendError("FFmpeg binary path invalid", errors);
				}
				this.encoderbinary = ffmpegfile;
				this.encoderbitrate = sc.getIntOrError("amusic\0encoder\0bitrate", errors);
				int channels = sc.getIntOrError("amusic\0encoder\0channels", errors);
				this.encoderchannels = (byte) channels; 
				this.encodersamplingrate = sc.getIntOrError("amusic\0encoder\0samplingrate", errors);
			} else {
				this.encoderbinary = null;
				this.encoderbitrate = 0;
				this.encoderchannels = 0;
				this.encodersamplingrate = 0;
			}
			this.processpack = sc.getBooleanOrError("amusic\0resourcepack\0processpack", errors);
			this.packsizelimit = sc.getIntOrError("amusic\0resourcepack\0sizelimit", errors);
			int packthreadlimitcount = sc.getIntOrError("amusic\0resourcepack\0packthread\0limitcount", errors);
			if(packthreadlimitcount > 32767) {
				packthreadlimitcount = 32767;
			}
			this.packthreadlimitcount = (short) packthreadlimitcount;
			this.packthreadcoefficient = sc.getFloatOrError("amusic\0resourcepack\0packthread\0coefficient", errors);
			
			this.servercache = sc.getBooleanOrError("amusic\0resourcepack\0cache\0server", errors);
			this.clientcache = sc.getBooleanOrError("amusic\0resourcepack\0cache\0client", errors);
			
		} else {
			this.use = false;
			this.usecmd = false;
			this.musicdir = null;
			this.packeddir = null;
			this.uploaduse = false;
			this.connectuse = false;
			this.encoderuse = false;
			this.uploadhost = null;
			this.uploadhttps = false;
			this.sendpackserverfactory = null;
			this.uploadserverfactory = null;
			this.uploadifip = null;
			this.uploadport = 0;
			this.uploadbacklog = 0;
			this.uploadtimeout = 0;
			this.uploadstrictaccess = false;
			this.uploadlifetime = 0;
			this.uploadlimitsize = 0;
			this.uploadlimitcount = 0;
			this.sendpackhost = null;
			this.sendpackifip = null;
			this.sendpackport = 0;
			this.sendpackbacklog = 0;
			this.sendpacktimeout = 0;
			this.sendpackstrictaccess = false;
			this.waitacception = false;
			this.tokensalt = null;
			this.connectifip = null;
			this.connecttls = false;
			this.connectremoteip = null;
			this.connectport = 0;
			this.connectbacklog = 0;
			this.connectserverfactory = null;
			this.connectsocketfactory = null;
			this.encoderbinary = null;
			this.encoderbitrate = 0;
			this.encoderchannels = 0;
			this.encodersamplingrate = 0;
			this.processpack = false;
			this.packsizelimit = 0;
			this.packthreadlimitcount = 0;
			this.packthreadcoefficient = 0;
			this.servercache = false;
			this.clientcache = false;
		}
		this.errors = errors.toString();
	}
	
	private static void appendError(String error, StringBuilder sb) {
		sb.append(error);
		sb.append('\n');
	}

}
