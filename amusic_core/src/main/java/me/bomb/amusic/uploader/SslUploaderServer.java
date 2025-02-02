package me.bomb.amusic.uploader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public final class SslUploaderServer extends Thread implements Uploader {

	private final static byte[] noaccess = "HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
	private final SSLServerSocketFactory serverfactory;
	private final UploadManager uploadmanager;
	private final ConcurrentHashMap<Object, InetAddress> onlineips;
	private final InetAddress ip;
	private final int port, backlog;
	private boolean run = false;
	private SSLServerSocket server;

	public SslUploaderServer(UploadManager uploadmanager, ConcurrentHashMap<Object, InetAddress> onlineips, InetAddress ip, int port, int backlog, File certfile, String certpassword) {
		TrustManager[] trustcerts = new TrustManager[] {new UploderTrustManager()};
		KeyManager[] keymanagers = null;
		char[] keypassword =  certpassword.toCharArray();
		SSLServerSocketFactory serverfactory = null;
	    SSLContext sslcontext = null, tlscontext = null;
		try {
			KeyStore keystore = KeyStore.getInstance("PKCS12");
			keystore.load(new FileInputStream(certfile), keypassword);
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keystore, keypassword);
			keymanagers = keyManagerFactory.getKeyManagers();
			sslcontext = SSLContext.getInstance("SSL");
			sslcontext.init(null, trustcerts, new java.security.SecureRandom());
			tlscontext = SSLContext.getInstance("TLS");
			tlscontext.init(keymanagers, null, null);
			serverfactory = sslcontext.getServerSocketFactory();
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | UnrecoverableKeyException | KeyManagementException e) {
		}
		this.serverfactory = serverfactory;
		this.uploadmanager = uploadmanager;
		this.onlineips = onlineips;
		this.ip = ip;
		this.port = port;
		this.backlog = backlog;
	}

	@Override
	public void start() {
		run = true;
		super.start();
		this.uploadmanager.start();
	}

	public void run() {
		while (run) {
			try {
				server = (SSLServerSocket)serverfactory.createServerSocket(port, backlog, ip);
				server.setEnabledProtocols(new String[] {"TLSv1", "TLSv1.1", "TLSv1.2", "SSLv3"});
				server.setEnabledCipherSuites(serverfactory.getSupportedCipherSuites());
			} catch (IOException | SecurityException | IllegalArgumentException | NullPointerException e) {
				e.printStackTrace();
				return;
			}
			while (!server.isClosed()) {
				try {
					if (this.onlineips == null) {
						new PageSender(uploadmanager, server.accept()).start();
					} else {
						Socket connected = server.accept();
						if (onlineips.values().contains(connected.getInetAddress())) {
							new PageSender(uploadmanager, connected).start();
						} else {
							connected.getOutputStream().write(noaccess);
							connected.close();
						}
					}
				} catch (IOException e) {
				}
			}
			try {
				sleep(1000L);
			} catch (InterruptedException e) {
			}
		}
	}

	public void end() {
		run = false;
		if (server == null)
			return;
		try {
			server.close();
		} catch (IOException e) {
		}
		this.uploadmanager.end();
	}
	
	protected final static class UploderTrustManager implements X509TrustManager {
		
		protected UploderTrustManager() {
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}
		
	}

}
