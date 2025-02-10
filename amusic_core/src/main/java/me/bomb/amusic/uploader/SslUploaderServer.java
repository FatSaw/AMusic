package me.bomb.amusic.uploader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public final class SslUploaderServer extends Thread implements Uploader {

	private final static byte[] noaccess = "HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
	private final SSLServerSocketFactory serverfactory;
	private final UploadManager uploadmanager;
	private final ConcurrentHashMap<Object, InetAddress> onlineips;
	private final InetAddress ip;
	private final int port, backlog;
	private boolean run = false;
	private SSLServerSocket server;

	public SslUploaderServer(UploadManager uploadmanager, ConcurrentHashMap<Object, InetAddress> onlineips, InetAddress ip, int port, int backlog, File keyfile, String certpassword) {
		char[] keypassword =  certpassword.toCharArray();
		SSLServerSocketFactory serverfactory = null;
		try {
			KeyStore keystore = KeyStore.getInstance("PKCS12");
			keystore.load(new FileInputStream(keyfile), keypassword);
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(keystore);
			TrustManager[] trustmanagers = trustManagerFactory.getTrustManagers();
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keystore, keypassword);
			KeyManager[] keymanagers = keyManagerFactory.getKeyManagers();
			SSLContext tlscontext = SSLContext.getInstance("TLSv1.2");
			tlscontext.init(keymanagers, trustmanagers, SecureRandom.getInstanceStrong());
			serverfactory = tlscontext.getServerSocketFactory();
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | UnrecoverableKeyException | KeyManagementException e) {
			e.printStackTrace();
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
				//server.setNeedClientAuth(true); //
			} catch (IOException | SecurityException | IllegalArgumentException | NullPointerException e) {
				e.printStackTrace();
				return;
			}
			while (!server.isClosed()) {
				try {
					
					SSLSocket connected = (SSLSocket) server.accept();
					if (this.onlineips == null) {
						new SSLPageSender(uploadmanager, connected).start();
					} else {
						if (onlineips.values().contains(connected.getInetAddress())) {
							new SSLPageSender(uploadmanager, connected).start();
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

}
