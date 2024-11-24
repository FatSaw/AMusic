package me.bomb.amusic.uploader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Decoder;
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
		
		private static final byte[] notfound, clidentifier, headerend, headersplit;
		private static final byte[][] web;
		private static final byte[][] identifier;

		private final Socket connected;

		static {
			notfound = "HTTP/1.1 404 Not Found\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
			clidentifier = "Content-Length: ".getBytes(StandardCharsets.US_ASCII);
			headerend = new byte[] {'\r','\n','\r','\n'};
			headersplit = new byte[] {'\r','\n'};
			final byte[] responseparthtml0 = "HTTP/1.1 200 OK\r\nServer: HTTP server\r\nContent-Type: text/html\r\nContent-Length: "
					.getBytes(StandardCharsets.US_ASCII),
					responsepartjs0 = "HTTP/1.1 200 OK\r\nServer: HTTP server\r\nContent-Type: text/javascript\r\nContent-Length: "
							.getBytes(StandardCharsets.US_ASCII),
					responsepartwasm0 = "HTTP/1.1 200 OK\r\nServer: HTTP server\r\nContent-Type: application/wasm\r\nContent-Length: "
							.getBytes(StandardCharsets.US_ASCII),
					responseclose = "\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
			
			final ClassLoader classloader = UploaderServer.class.getClassLoader();
			web = new byte[5][];
			identifier = new byte[6][];
			loadStaticContent(classloader, (byte)0, 3006, "index.html", "", responseparthtml0, responseclose);
			loadStaticContent(classloader, (byte)1, 7272, "index.js", "index.js", responsepartjs0, responseclose);
			loadStaticContent(classloader, (byte)2, 2954, "814.ffmpeg.js", "814.ffmpeg.js", responsepartjs0, responseclose);
			loadStaticContent(classloader, (byte)3, 87056, "ffmpeg-core.js", "ffmpeg-core.js", responsepartjs0, responseclose);
			loadStaticContent(classloader, (byte)4, 2284922, "ffmpeg-core.wasm", "ffmpeg-core.wasm", responsepartwasm0, responseclose);
			identifier[5] = "POST".getBytes(StandardCharsets.US_ASCII);
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
			byte[] buf = new byte[2048];
			try {
				final InputStream cis = connected.getInputStream();
				int readcount = cis.read(buf);
				byte e = 6;
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
				if(e == 5) {
					connected.shutdownOutput();
					int i = 6;
					while(i < readcount) {
						if(buf[i] == ' ') break;
						++i;
					}
					i-=6;
					byte[] bytes = new byte[i];
					System.arraycopy(buf, 6, bytes, 0, i);
					String name = parseName(bytes);
					int split = indexOf(buf, headerend, i, buf.length);
					if(split != -1) {
						int pi = i, cl = -1;
						while((i = indexOf(buf, headersplit, i, split)) != -1) {
							int length = i - pi;
							if(length > 16 && length < 26) { //27
								byte k = 16;
								int l = pi + k;
								while(--k > -1) {
									if(clidentifier[k] != buf[--l]) {
										break;
									}
								}
								if(k == -1) {
									length -= 16;
									bytes = new byte[length];
									System.arraycopy(buf, pi + 16, bytes, 0, length);
									String cls = new String(bytes, StandardCharsets.US_ASCII);
									try {
										cl = Integer.parseInt(cls);
									} catch (NumberFormatException ex) {
									}
								}
							}
							i += headersplit.length;
							pi = i;
						}
						if(cl != -1) {
							split += 4;
							bytes = new byte[cl];
							readcount-=split;
							System.arraycopy(buf, split, bytes, 0, readcount);
							
							int pos = readcount;
							while((readcount = cis.read(buf)) != -1) {
								System.arraycopy(buf, 0, bytes, pos, readcount);
								pos+=readcount;
							}
							
							if((i = name.lastIndexOf('.')) != -1) {
								name = name.substring(0, i);
							}
							final File debugfile = new File("./plugins/AMusic/" + name + ".ogg");
							final FileOutputStream fos = new FileOutputStream(debugfile, false);
							try {
								fos.write(bytes, 0, bytes.length);
							} catch (IOException ex) {
							} finally {
								try {
									fos.close();
								} catch (IOException ex) {
								}
							}
						}
					}
				} else {
					connected.shutdownInput();
					connected.getOutputStream().write(e == -1 ? notfound : web[e]);
					connected.shutdownOutput();
				}
			} catch (IOException e) {
			} finally {
				try {
					this.connected.close();
				} catch (IOException e) {
				}
			}

		}
		
		public static int indexOf(byte[] outerArray, byte[] smallerArray, int from, int to) {
		    for(int i = from; i < to - smallerArray.length+1; ++i) {
		        boolean found = true;
		        int j = smallerArray.length;
		        while(--j > -1) {
		        	if (outerArray[i+j] != smallerArray[j]) {
			               found = false;
			               break;
			           }
		        }
		        if (found) return i;
		     }
		   return -1;  
		}  
		
		private static String parseName(byte[] nameb) {
			try {
				Decoder base64decoder = Base64.getDecoder();
				nameb = base64decoder.decode(nameb);
				int finalcount = 0;
				int i = nameb.length;
				while(--i > -1) {
					byte c = nameb[i];
					if(c == '/' || c == '\\' || c == ':' || c == '<' || c == '>' || c == '*' || c == '?' || c == '|' || c == '\"' || c == '\0' || (c > 0 && c < 32)) {
						nameb[i] = '\0';
					} else {
						++finalcount;
					}
				}
				byte[] filtered = new byte[finalcount];
				int j = 0;
				while(++i < nameb.length && j < finalcount) {
					byte c = nameb[i];
					if(c != '\0') {
						filtered[j] = c;
						++j;
					}
				}
				return new String(filtered, StandardCharsets.UTF_8);
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
		
	}

}
