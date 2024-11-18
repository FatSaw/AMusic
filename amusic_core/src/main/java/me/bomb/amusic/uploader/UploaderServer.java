package me.bomb.amusic.uploader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

public final class UploaderServer extends Thread {
	private final ConcurrentHashMap<Object, InetAddress> onlineips;
	private final InetAddress ip;
	private final int port, backlog;
	private boolean run = false;
	private ServerSocket server;

	public UploaderServer(ConcurrentHashMap<Object, InetAddress> onlineips, InetAddress ip, int port, int backlog) {
		this.onlineips = onlineips;
		this.ip = ip;
		this.port = port;
		this.backlog = backlog;
	}

	@Override
	public void start() {
		run = true;
		super.start();
	}

	public void run() {
		while (run) {
			try {
				server = new ServerSocket();
				server.bind(new InetSocketAddress(ip, port), backlog);
			} catch (IOException | SecurityException | IllegalArgumentException e) {
				e.printStackTrace();
				return;
			}
			while (!server.isClosed()) {
				try {
					new PageSender(server.accept()).start();
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
		try {
			server.close();
		} catch (IOException e) {
		}
	}

	public static final class PageSender extends Thread {
		private static final byte[] responsenotfound;
		private static final byte[][] web;
		private static final byte[][] identifier;

		private final Socket connected;

		static {
			{
				responsenotfound = "HTTP/1.1 404 Not Found\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
				final byte[] responseparthtml0 = "HTTP/1.1 200 OK\r\nServer: HTTP server\r\nContent-Type: text/html\r\nContent-Length: "
						.getBytes(StandardCharsets.US_ASCII),
						responsepartjs0 = "HTTP/1.1 200 OK\r\nServer: HTTP server\r\nContent-Type: text/javascript\r\nContent-Length: "
								.getBytes(StandardCharsets.US_ASCII),
						responsepartwasm0 = "HTTP/1.1 200 OK\r\nServer: HTTP server\r\nContent-Type: application/wasm\r\nContent-Length: "
								.getBytes(StandardCharsets.US_ASCII),
						responseclose = "\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
				
				final ClassLoader classloader = UploaderServer.class.getClassLoader();
				web = new byte[5][];
				identifier = new byte[5][];
				loadStaticContent(classloader, (byte)0, 2397, "index.html", "", responseparthtml0, responseclose);
				loadStaticContent(classloader, (byte)1, 7272, "index.js", "index.js", responsepartjs0, responseclose);
				loadStaticContent(classloader, (byte)2, 2954, "814.ffmpeg.js", "814.ffmpeg.js", responsepartjs0, responseclose);
				loadStaticContent(classloader, (byte)3, 87056, "ffmpeg-core.js", "ffmpeg-core.js", responsepartjs0, responseclose);
				loadStaticContent(classloader, (byte)4, 2284922, "ffmpeg-core.wasm", "ffmpeg-core.wasm", responsepartwasm0, responseclose);
				
			}
		}
		
		private static void loadStaticContent(final ClassLoader classloader, final byte id, final int contentsize, final String contentid, final String contentpublicid, byte[] header, byte[] headerclose) {
			InputStream in = classloader.getResourceAsStream(contentid);
			try {
				byte[] contentsizebytes = Integer.toString(contentsize).getBytes(StandardCharsets.US_ASCII);
				int pos = header.length;
				byte[] buf = new byte[contentsize + contentsizebytes.length + pos + headerclose.length], bufb = new byte[8192];
				System.arraycopy(header, 0, buf, 0, pos);
				System.arraycopy(contentsizebytes, 0, buf, --pos, contentsizebytes.length);
				pos+=contentsizebytes.length;
				System.arraycopy(headerclose, 0, buf, pos, headerclose.length);
				pos+=headerclose.length;
				int i;
				while ((i = in.read(bufb)) != -1) {
					System.arraycopy(bufb, 0, buf, pos, i);
					pos += i;
				}
				web[id] = buf;
				identifier[id] = "GET /".concat(contentpublicid).getBytes(StandardCharsets.US_ASCII);
			} catch (IOException e) {
			} finally {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}

		protected PageSender(Socket connected) {
			this.connected = connected;
		}

		public void run() {
			byte[] buf = new byte[512];
			try {
				InputStream cis = connected.getInputStream();
				cis.read(buf);
				connected.shutdownInput();
				byte e = 5;
				while (--e > -1) {
					int i = identifier[e].length;
					
					boolean b = false;
					if (buf.length <= i || buf[i] != ' ') {
						continue;
					}
					while (--i > -1) {
						if (b = buf[i] != identifier[e][i]) {
							break;
						}
					}
					if (b) {
						continue;
					}
					break;
				}
				OutputStream cos = connected.getOutputStream();
				if (e == -1) {
					cos.write(responsenotfound);
				} else {
					cos.write(web[e], 0, web[e].length);
				}
				cos.flush();
			} catch (IOException e) {
			} finally {
				try {
					this.connected.close();
				} catch (IOException e) {
				}
			}

		}
	}

}
