package me.bomb.amusic.uploader;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.UUID;

import static me.bomb.amusic.util.NameFilter.filterName;

public final class PageSender extends Thread {
	
	private static final byte[] notfound, updated, nolength, novalidtoken, notoken, headertoolarge, datatoolarge, clidentifier, uidentifier, headerend, headersplit;
	private static final byte[][] web;
	private static final byte[][] identifier;

	private final UploadManager uploadmanager;
	private final Socket connected;

	static {
		notfound = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
		updated = "HTTP/1.1 204 No Content\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
		nolength = "HTTP/1.1 411 Length Required\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
		novalidtoken = "HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
		notoken = "HTTP/1.1 410 Gone\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
		headertoolarge = "HTTP/1.1 431 Request Header Fields Too Large\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
		datatoolarge = "HTTP/1.1 413 Payload Too Large\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
		clidentifier = "Content-Length: ".getBytes(StandardCharsets.US_ASCII);
		uidentifier = "UUID: ".getBytes(StandardCharsets.US_ASCII);
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
		identifier = new byte[7][];
		loadStaticContent(classloader, (byte)0, 2787, "index.html", "", responseparthtml0, responseclose);
		loadStaticContent(classloader, (byte)1, 7272, "index.js", "index.js", responsepartjs0, responseclose);
		loadStaticContent(classloader, (byte)2, 2954, "814.ffmpeg.js", "814.ffmpeg.js", responsepartjs0, responseclose);
		loadStaticContent(classloader, (byte)3, 87056, "ffmpeg-core.js", "ffmpeg-core.js", responsepartjs0, responseclose);
		loadStaticContent(classloader, (byte)4, 2284922, "ffmpeg-core.wasm", "ffmpeg-core.wasm", responsepartwasm0, responseclose);
		identifier[5] = "PUT".getBytes(StandardCharsets.US_ASCII);
		identifier[6] = "DELETE".getBytes(StandardCharsets.US_ASCII);
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

	protected PageSender(UploadManager uploadmanager, Socket connected) {
		this.uploadmanager = uploadmanager;
		this.connected = connected;
	}

	public void run() {
		byte[] buf = new byte[2048];
		
		try {
			final InputStream cis = connected.getInputStream();
			int readcount = cis.read(buf);
			byte e = 7;
			while (--e > -1) {
				int i = identifier[e].length;
				boolean b = false;
				if (buf.length <= i || (e != 0 && buf[i] != ' ')) {
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
			if(e == 0) {
				final int si = 36 + identifier[e].length;
				if(buf.length > si && buf[si] == ' ') {
					try {
						UUID.fromString(new String(buf, identifier[e].length, 36, StandardCharsets.US_ASCII));
					} catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
						--e;
					}
				} else {
					--e;
				}
			}
			if(e == 6) {
				//DELETE REQUEST
				connected.shutdownInput();
				int i = 8;
				while(i < readcount) {
					if(buf[i] == ' ') break;
					++i;
				}
				int split = indexOf(buf, headerend, i, buf.length);
				if(split != -1) {
					byte[] bytes = new byte[i-8];
					System.arraycopy(buf, 8, bytes, 0, bytes.length);
					String name = parseName(bytes);
					if(name == null) {
						name = "";
					}
					name = name.replace(' ', '_');
					int pi = i;
					UUID token = null;
					while((i = indexOf(buf, headersplit, i, split)) != -1) {
						int length = i - pi;
						if(length == 42) {
							byte k = 6;
							int l = pi + k;
							while(--k > -1) {
								if(uidentifier[k] != buf[--l]) {
									break;
								}
							}
							if(k == -1) {
								length -= 6;
								try {
									token = UUID.fromString(new String(buf, pi + 6, length, StandardCharsets.US_ASCII));
								} catch (IndexOutOfBoundsException | IllegalArgumentException ex) {
								}
							}
						}
						i += headersplit.length;
						pi = i;
					}
					if(token != null) {
						final UploadSession session = uploadmanager.getSession(token);
						if(session != null) {
							if((i = name.lastIndexOf('.')) != -1) {
								name = name.substring(0, i);
							}
							session.remove(name);
							connected.getOutputStream().write(updated);
							connected.shutdownOutput();
						} else {
							connected.getOutputStream().write(notoken);
							connected.shutdownOutput();
						}
					} else {
						connected.getOutputStream().write(novalidtoken);
						connected.shutdownOutput();
					}
				} else {
					connected.getOutputStream().write(headertoolarge);
					connected.shutdownOutput();
				}
			} else if(e == 5) {
				//PUT REQUEST
				int i = 5;
				while(i < readcount) {
					if(buf[i] == ' ') break;
					++i;
				}
				int split = indexOf(buf, headerend, i, buf.length);
				if(split != -1) {
					byte[] bytes = new byte[i-5];
					System.arraycopy(buf, 5, bytes, 0, bytes.length);
					String name = parseName(bytes);
					if(name == null) {
						name = "";
					}
					name = name.replace(' ', '_');
					int pi = i, cl = -1;
					UUID token = null;
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
								try {
									cl = Integer.parseInt(new String(buf, pi + 16, length, StandardCharsets.US_ASCII));
								} catch (IndexOutOfBoundsException | NumberFormatException ex) {
								}
							}
						}
						if(length == 42) {
							byte k = 6;
							int l = pi + k;
							while(--k > -1) {
								if(uidentifier[k] != buf[--l]) {
									break;
								}
							}
							if(k == -1) {
								length -= 6;
								try {
									token = UUID.fromString(new String(buf, pi + 6, length, StandardCharsets.US_ASCII));
								} catch (IndexOutOfBoundsException | IllegalArgumentException ex) {
								}
							}
						}
						i += headersplit.length;
						pi = i;
					}
					if(token != null) {
						if(cl != -1) {
							final UploadSession session = uploadmanager.getSession(token);
							if(session != null) {
								final boolean canput = session.canPut(cl);
								if(canput) {
									connected.getOutputStream().write(updated);
									connected.shutdownOutput();
									split += 4;
									bytes = new byte[cl];
									if(cl != 0) {
										readcount-=split;
										System.arraycopy(buf, split, bytes, 0, readcount);
										int pos = readcount;
										while((readcount = cis.read(buf)) != -1) {
											System.arraycopy(buf, 0, bytes, pos, readcount);
											pos+=readcount;
										}
									}
									connected.shutdownInput();
									if((i = name.lastIndexOf('.')) != -1) {
										name = name.substring(0, i);
									}
									session.put(name, bytes); //PUT
								} else {
									connected.shutdownInput();
									connected.getOutputStream().write(datatoolarge);
									connected.shutdownOutput();
								}
							} else {
								connected.shutdownInput();
								connected.getOutputStream().write(notoken);
								connected.shutdownOutput();
							}
						} else {
							connected.shutdownInput();
							connected.getOutputStream().write(nolength);
							connected.shutdownOutput();
						}
					} else {
						connected.shutdownInput();
						connected.getOutputStream().write(novalidtoken);
						connected.shutdownOutput();
					}
				} else {
					connected.shutdownInput();
					connected.getOutputStream().write(headertoolarge);
					connected.shutdownOutput();
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
		to -= smallerArray.length;
		++to;
	    while(from < to) {
	        boolean found = true;
	        int j = smallerArray.length;
	        while(--j > -1) {
	        	if (outerArray[from+j] != smallerArray[j]) {
	        		found = false;
	        		break;
	        	}
	        }
	        if (found) return from;
	        ++from;
	     }
	   return -1;  
	}
	
	private static String parseName(byte[] nameb) {
		try {
			Decoder base64decoder = Base64.getDecoder();
			nameb = base64decoder.decode(nameb);
			return filterName(new String(nameb, StandardCharsets.UTF_8));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
	
}
