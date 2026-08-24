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
import java.util.concurrent.Executor;

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
	
	public final Executor executor, serverexecutor, sendpackexecutorsender;
	
	public final boolean use, usecmd, connectuse, connecttls;
	
	public final String sendpackhost, joinplaylist;
	public final InetAddress sendpackifip, connectifip, connectremoteip;
	public final int sendpackport, connectport;
	public final int sendpackbacklog, connectbacklog;
	public final int sendpacktimeout;

	public final int waitacceptioncount;
	public final int waitacceptionschedulerthreads;
	public final int waitacceptionwait;
	
	public final boolean sendpackstrictaccess;
	
	public final boolean processpack, ramcache, diskstore;
	public final int packsizelimit;
	public final short packthreadlimitcount;
	public final float packthreadcoefficient;
	
	public final byte[] tokensalt;
	public final ServerSocketFactory sendpackserverfactory, connectserverfactory;
	public final SocketFactory connectsocketfactory;
	
	public Configuration(Executor executor, Executor serverexecutor, Executor sendpackexecutorchecker, Executor sendpackexecutorsender, boolean usecmd, boolean sendpackuse, boolean connectuse, boolean connecthttps, String sendpackhost, String joinplaylist, InetAddress sendpackifip, InetAddress connectifip, InetAddress connectremoteip, int sendpackport, int connectport, int sendpackbacklog, int connectbacklog, int sendpacktimeout, boolean sendpackstrictaccess, boolean processpack, boolean ramcache, boolean diskstore, int waitacceptioncount, int waitacceptionschedulerthreads, int waitacceptionwait, int packsizelimit, short packthreadlimitcount, float packthreadcoefficient, byte[] tokensalt, ServerSocketFactory sendpackserverfactory, ServerSocketFactory connectserverfactory, SocketFactory connectsocketfactory) {
		this.errors = new String();
		this.use = true;
		this.executor = executor;
		this.serverexecutor = serverexecutor;
		this.sendpackexecutorsender = sendpackexecutorsender;
		this.usecmd = usecmd;
		this.connectuse = connectuse;
		this.connecttls = connecthttps;
		this.sendpackhost = sendpackhost;
		this.joinplaylist = joinplaylist;
		this.sendpackifip = sendpackifip;
		this.connectifip = connectifip;
		this.connectremoteip = connectremoteip;
		this.sendpackport = sendpackport;
		this.connectport = connectport;
		this.sendpackbacklog = sendpackbacklog;
		this.connectbacklog = connectbacklog;
		this.sendpacktimeout = sendpacktimeout;
		this.sendpackstrictaccess = sendpackstrictaccess;
		this.processpack = processpack;
		this.ramcache = ramcache;
		this.diskstore = diskstore;
		this.waitacceptioncount = waitacceptioncount;
		this.waitacceptionschedulerthreads = waitacceptionschedulerthreads;
		this.waitacceptionwait = waitacceptionwait;
		this.packsizelimit = packsizelimit;
		this.packthreadlimitcount = packthreadlimitcount;
		this.packthreadcoefficient = packthreadcoefficient;
		this.tokensalt = tokensalt;
		this.sendpackserverfactory = sendpackserverfactory;
		this.connectserverfactory = connectserverfactory;
		this.connectsocketfactory = connectsocketfactory;
	}
	
	public Configuration(FileSystem fs, final Path configfile, final Path musicdir, final Path packeddir, final boolean defaultremoteclient) {
		byte[] bytes = null;
		final StringBuilder errors = new StringBuilder();
		InputStream is = null;
		FileSystemProvider fsp = fs.provider();
		int size = 0;
		try {
			BasicFileAttributes attributes = fsp.readAttributes(configfile, BasicFileAttributes.class);
			is = fsp.newInputStream(configfile);
			long filesize = attributes.size();
			if(filesize > 0x00010000) {
				filesize = 0x00010000;
			}
			bytes = new byte[(int)filesize];
			size = is.read(bytes, 0, bytes.length);
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
				size = is.read(bytes, 0, bytes.length);
				is.close();
				OutputStream os = null;
				try {
					os = fsp.newOutputStream(configfile);
					os.write(bytes, 0, size);
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
		if(bytes != null && (sc = new SimpleConfiguration(bytes, size)).getBooleanOrError("amusic\0use", errors)) {
			final String EMPTY = new String();
			this.use = true;
			this.usecmd = sc.getBooleanOrError("amusic\0usecmd", errors);
			this.connectuse = sc.getBooleanOrError("amusic\0server\0connect\0use", errors);
			String executorcfg = sc.getStringOrDefault("amusic\0executor", EMPTY);
			ExecutorConfiguration executorconfig = executorcfg.equals(EMPTY) ? new ExecutorConfiguration(sc, "amusic\0executor") : new ExecutorConfiguration("executor_".concat(executorcfg).concat(".yml"));
			if(executorconfig.errors.length() != 0) {
				appendError("Filed to load executor configuration", errors);
				errors.append(executorconfig.errors);
			}
			this.executor = executorconfig.createExecutor();
			
			String sendpackexecutorcfg = sc.getStringOrDefault("amusic\0server\0sendpack\0executor\0checker", EMPTY);

			sendpackexecutorcfg = sc.getStringOrDefault("amusic\0server\0sendpack\0executor\0sender", EMPTY);
			ExecutorConfiguration sendpackexecutorconfig = sendpackexecutorcfg.equals(EMPTY) ? new ExecutorConfiguration(sc, "amusic\0server\0sendpack\0executor\0sender") : new ExecutorConfiguration("executor_".concat(sendpackexecutorcfg).concat(".yml"));
			if(sendpackexecutorconfig.errors.length() != 0) {
				appendError("Filed to load sendpack executor sender configuration", errors);
				errors.append(sendpackexecutorconfig.errors);
			}
			this.sendpackexecutorsender = sendpackexecutorconfig.createExecutor();
			
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

			

			this.waitacceptioncount = sc.getIntOrError("amusic\0server\0sendpack\0waitacception\0count", errors);
			if(this.waitacceptioncount > 0) {
				this.waitacceptionwait = sc.getIntOrError("amusic\0server\0sendpack\0waitacception\0wait", errors);
				this.waitacceptionschedulerthreads = sc.getIntOrError("amusic\0server\0sendpack\0waitacception\0schedulerthreads", errors);
			} else {
				this.waitacceptionwait = 0;
				this.waitacceptionschedulerthreads = 0;
			}
			this.tokensalt = sc.getBytesBase64OrDefault("amusic\0server\0sendpack\0tokensalt", null);
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
					this.serverexecutor = null;
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
					String serverexecutorcfg = sc.getStringOrDefault("amusic\0server\0connect\0server\0executor", EMPTY);
					ExecutorConfiguration serverexecutorconfig = serverexecutorcfg.equals(EMPTY) ? new ExecutorConfiguration(sc, "amusic\0server\0connect\0server\0executor") : new ExecutorConfiguration("executor_".concat(serverexecutorcfg).concat(".yml"));
					if(serverexecutorconfig.errors.length() != 0) {
						appendError("Filed to load server executor configuration", errors);
						errors.append(serverexecutorconfig.errors);
					}
					this.serverexecutor = serverexecutorconfig.createExecutor();
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
				this.serverexecutor = null;
				this.connectifip = null;
				this.connecttls = false;
				this.connectremoteip = null;
				this.connectport = 0;
				this.connectbacklog = 0;
				this.connectserverfactory = null;
				this.connectsocketfactory = null;
			}
			this.processpack = sc.getBooleanOrError("amusic\0resourcepack\0processpack", errors);
			this.packsizelimit = sc.getIntOrError("amusic\0resourcepack\0sizelimit", errors);
			this.joinplaylist = sc.getStringOrDefault("amusic\0resourcepack\0joinplaylist", null);
			int packthreadlimitcount = sc.getIntOrError("amusic\0resourcepack\0packthread\0limitcount", errors);
			if(packthreadlimitcount > 32767) {
				packthreadlimitcount = 32767;
			}
			this.packthreadlimitcount = (short) packthreadlimitcount;
			this.packthreadcoefficient = sc.getFloatOrError("amusic\0resourcepack\0packthread\0coefficient", errors);
			this.ramcache = sc.getBooleanOrError("amusic\0resourcepack\0cache", errors);
			this.diskstore = sc.getBooleanOrError("amusic\0resourcepack\0diskstore", errors);
		} else {
			this.use = false;
			this.usecmd = false;
			this.executor = null;
			this.serverexecutor = null;
			this.connectuse = false;
			this.sendpackserverfactory = null;
			this.sendpackexecutorsender = null;
			this.sendpackhost = null;
			this.sendpackifip = null;
			this.sendpackport = 0;
			this.sendpackbacklog = 0;
			this.sendpacktimeout = 0;
			this.sendpackstrictaccess = false;
			this.waitacceptioncount = 0;
			this.waitacceptionwait = 0;
			this.waitacceptionschedulerthreads = 0;
			this.tokensalt = null;
			this.connectifip = null;
			this.connecttls = false;
			this.connectremoteip = null;
			this.connectport = 0;
			this.connectbacklog = 0;
			this.connectserverfactory = null;
			this.connectsocketfactory = null;
			this.processpack = false;
			this.packsizelimit = 0;
			this.joinplaylist = null;
			this.packthreadlimitcount = 0;
			this.packthreadcoefficient = 0;
			this.ramcache = false;
			this.diskstore = false;
		}
		this.errors = errors.toString();
	}
	
	private static void appendError(String error, StringBuilder sb) {
		sb.append(error);
		sb.append('\n');
	}

}
