package me.bomb.amusic.uploader;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.UUID;

import me.bomb.amusic.http.ServerWorker;

import static me.bomb.amusic.util.NameFilter.filterName;

final class PageSender implements ServerWorker {
	
	private static final byte[] notfound, updated, nolength, novalidtoken, notoken, headertoolarge, novalidtokenget, notokenget, headertoolargeget, datatoolarge, queryheader, clidentifier, uidentifier, expectidentifier, headerend, headersplit;
	private static final byte[][] web;
	private static final byte[][] identifier;

	private final UploadManager uploadmanager;

	static {
		notfound = "HTTP/1.1 404 Not Found\r\nServer: AMusic upload\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
		updated = "HTTP/1.1 204 No Content\r\nServer: AMusic upload\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
		nolength = "HTTP/1.1 411 Length Required\r\nServer: AMusic upload\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
		novalidtoken = "HTTP/1.1 403 Forbidden\r\nServer: AMusic upload\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
		notoken = "HTTP/1.1 410 Gone\r\nServer: AMusic upload\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
		headertoolarge = "HTTP/1.1 431 Request Header Fields Too Large\r\nServer: AMusic upload\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
		novalidtokenget = "HTTP/1.1 403 Forbidden\r\nServer: AMusic upload\r\nCache-Control: no-store\r\nContent-Type: text/plain\r\nX-Content-Type-Options: nosniff\r\nReferrer-Policy: no-referrer\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
		notokenget = "HTTP/1.1 410 Gone\r\nServer: AMusic upload\r\nCache-Control: no-store\r\nContent-Type: text/plain\r\nX-Content-Type-Options: nosniff\r\nReferrer-Policy: no-referrer\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
		headertoolargeget = "HTTP/1.1 431 Request Header Fields Too Large\r\nServer: AMusic upload\r\nCache-Control: no-store\r\nContent-Type: text/plain\r\nX-Content-Type-Options: nosniff\r\nReferrer-Policy: no-referrer\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
		datatoolarge = "HTTP/1.1 413 Payload Too Large\r\nServer: AMusic upload\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
		queryheader = "HTTP/1.1 200 OK\r\nServer: AMusic upload\r\nCache-Control: no-store\r\nContent-Type: application/octet-stream\r\nX-Content-Type-Options: nosniff\r\nReferrer-Policy: no-referrer\r\nConnection: close\r\nContent-Length: ".getBytes(StandardCharsets.US_ASCII);
		clidentifier = "Content-Length: ".getBytes(StandardCharsets.US_ASCII);
		uidentifier = "AUTH: ".getBytes(StandardCharsets.US_ASCII);
		expectidentifier = "Fits: ".getBytes(StandardCharsets.US_ASCII);
		headerend = new byte[] {'\r','\n','\r','\n'};
		headersplit = new byte[] {'\r','\n'};
		final byte[] responseparthtml0 = "HTTP/1.1 200 OK\r\nServer: AMusic upload\r\nCache-Control: max-age=86400\r\nContent-Type: text/html; charset=us-ascii\r\nX-Content-Type-Options: nosniff\r\nReferrer-Policy: no-referrer\r\nCross-Origin-Embedder-Policy: require-corp\r\nCross-Origin-Opener-Policy: same-origin\r\nCross-Origin-Resource-Policy: same-origin\r\nContent-Length: ".getBytes(StandardCharsets.US_ASCII),
				responsepartjs1 = "HTTP/1.1 200 OK\r\nServer: AMusic upload\r\nCache-Control: max-age=86400\r\nContent-Type: text/javascript; charset=us-ascii\r\nX-Content-Type-Options: nosniff\r\nReferrer-Policy: no-referrer\r\nCross-Origin-Embedder-Policy: require-corp\r\nCross-Origin-Opener-Policy: same-origin\r\nCross-Origin-Resource-Policy: same-origin\r\nContent-Length: ".getBytes(StandardCharsets.US_ASCII),
				responsepartwasm1 = "HTTP/1.1 200 OK\r\nServer: AMusic upload\r\nCache-Control: max-age=86400\r\nContent-Type: application/wasm\r\nX-Content-Type-Options: nosniff\r\nReferrer-Policy: no-referrer\r\nCross-Origin-Embedder-Policy: require-corp\r\nCross-Origin-Opener-Policy: same-origin\r\nCross-Origin-Resource-Policy: same-origin\r\nContent-Length: ".getBytes(StandardCharsets.US_ASCII),
				responseclose = "\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
		
		final ClassLoader classloader = PageSender.class.getClassLoader();
		web = new byte[6][];
		identifier = new byte[9][];
		web[0] = "HTTP/1.1 200 OK\r\nServer: AMusic upload\r\nCache-Control: private, max-age=5\r\nContent-Type: text/html; charset=us-ascii\r\nX-Content-Type-Options: nosniff\r\nReferrer-Policy: no-referrer\r\nLink: </index.html>; rel=preload; as=fetch, </index.js>; rel=preload; as=script, </814.ffmpeg.js>; rel=preload; as=script, </ffmpeg-core.js>; rel=preload; as=script, </ffmpeg-core.wasm>; rel=preload; as=fetch\r\nCross-Origin-Embedder-Policy: require-corp\r\nCross-Origin-Opener-Policy: same-origin\r\nCross-Origin-Resource-Policy: same-origin\r\nContent-Length: 601\r\nConnection: close\r\n\r\n<!DOCTYPE html><html><head><meta charset=\"us-ascii\"><title>Upload sound</title><link rel=\"icon\" href=\"data:,\"><style>body,html{margin:0;padding:0;height:100%;overflow:hidden;background:#f5f5f7}@media(prefers-color-scheme:dark){body,html{background:#121212}}iframe{width:100%;height:100%;border:none}</style></head><body><iframe id=\"f\"></iframe><script>async function l(){try{const r=await fetch(\"./index.html\"),t=await r.text(),s='<meta charset=\"us-ascii\"><base href=\"'+location.origin+'/\">',h=t.replace(\"<head>\",\"<head>\"+s);document.getElementById(\"f\").srcdoc=h}catch(e){}}l();</script></body></html>".getBytes(StandardCharsets.US_ASCII);
		identifier[0] = "GET /".getBytes(StandardCharsets.US_ASCII);
		loadStaticContent(classloader, (byte)1, 15653 , "index.html", "index.html", responseparthtml0, responseclose);
		loadStaticContent(classloader, (byte)2, 7272, "index.js", "index.js", responsepartjs1, responseclose);
		loadStaticContent(classloader, (byte)3, 2954, "814.ffmpeg.js", "814.ffmpeg.js", responsepartjs1, responseclose);
		loadStaticContent(classloader, (byte)4, 87000, "ffmpeg-core.js", "ffmpeg-core.js", responsepartjs1, responseclose);
		loadStaticContent(classloader, (byte)5, 2284922, "ffmpeg-core.wasm", "ffmpeg-core.wasm", responsepartwasm1, responseclose);
		identifier[6] = "PUT".getBytes(StandardCharsets.US_ASCII);
		identifier[7] = "DELETE".getBytes(StandardCharsets.US_ASCII);
		identifier[8] = "GET / ".getBytes(StandardCharsets.US_ASCII);
	}
	
	private static void loadStaticContent(final ClassLoader classloader, final byte id, final int contentsize, final String contentid, final String contentpublicid, byte[] header, byte[] headerclose) {
		InputStream is = null;
		try {
			is = classloader.getResourceAsStream(contentid);
			byte[] contentsizebytes = Integer.toString(contentsize).getBytes(StandardCharsets.US_ASCII);
			int pos = header.length;
			byte[] buf = new byte[contentsize + contentsizebytes.length + pos + headerclose.length], bufb = new byte[8192];
			System.arraycopy(header, 0, buf, 0, pos);
			System.arraycopy(contentsizebytes, 0, buf, --pos, contentsizebytes.length);
			pos+=contentsizebytes.length;
			System.arraycopy(headerclose, 0, buf, pos, headerclose.length);
			pos+=headerclose.length;
			int i;
			while ((i = is.read(bufb)) != -1) {
				System.arraycopy(bufb, 0, buf, pos, i);
				pos += i;
			}
			web[id] = buf;
			identifier[id] = "GET /".concat(contentpublicid).getBytes(StandardCharsets.US_ASCII);
			is.close();
		} catch (IOException e1) {
			if(is != null) {
				try {
					is.close();
				} catch (IOException e2) {
				}
			}
		}
	}

	protected PageSender(UploadManager uploadmanager) {
		this.uploadmanager = uploadmanager;
	}

	public void processConnection(final Socket connected) throws IOException {
		byte[] buf = new byte[2048];
		
		try {
			final InputStream cis = connected.getInputStream();
			int readcount = cis.read(buf);
			byte e = 9;
			while (--e > -1) {
				int i = identifier[e].length;
				boolean b = false;
				if (buf.length <= i || (e != 8 && e != 0 && buf[i] != ' ')) {
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
			if(e == 8) {
				int i = 6;
				int split = indexOf(buf, headerend, i, buf.length);
				if(split != -1) {
					int pi = i;
					UUID token = null;
					while(token == null && (i = indexOf(buf, headersplit, i, split)) != -1) {

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
							byte[] query = session.query();
							connected.getOutputStream().write(queryheader);
							connected.getOutputStream().write(Integer.toString(query.length).getBytes(StandardCharsets.UTF_8));
							connected.getOutputStream().write(headerend);
							connected.getOutputStream().write(query);
						} else {
							connected.getOutputStream().write(notokenget);
						}
						
					} else {
						connected.getOutputStream().write(novalidtokenget);
					}
				} else {
					connected.getOutputStream().write(headertoolargeget);
				}
			} else if(e == 7) {
				//DELETE REQUEST
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
							connected.getOutputStream().write(session.remove(name) ? updated : datatoolarge);
						} else {
							connected.getOutputStream().write(notoken);
						}
					} else {
						connected.getOutputStream().write(novalidtoken);
					}
				} else {
					connected.getOutputStream().write(headertoolarge);
				}
			} else if(e == 6) {
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
					int pi = i, cl = -1, fits = -1;
					UUID token = null;
					boolean expect = false;
					while((i = indexOf(buf, headersplit, i, split)) != -1) {
						int length = i - pi;
						if(!expect && length > 6 && length < 26 && length != 16) {
							if(length < 16) {
								byte k = 6;
								int l = pi + k;
								while(--k > -1) {
									if(expectidentifier[k] != buf[--l]) {
										break;
									}
								}
								if(k == -1) {
									expect = true;
									length -= 6;
									try {
										fits = Integer.parseInt(new String(buf, pi + 6, length, StandardCharsets.US_ASCII));
									} catch (IndexOutOfBoundsException | NumberFormatException ex) {
									}
								}
							} else {
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
					if(expect) {
						cl = fits;
					}
					
					if(token != null) {
						if(cl != -1) {
							final UploadSession session = uploadmanager.getSession(token);
							if(session != null) {
								if(!expect) {
									bytes = session.tryAllocPutBuf(name, cl);
									if(bytes != null) {
										split += 4;
										connected.getOutputStream().write(updated);
										if(cl != 0) {
											readcount-=split;
											System.arraycopy(buf, split, bytes, 0, readcount);
											int pos = readcount;
											while((readcount = cis.read(buf)) != -1) {
												System.arraycopy(buf, 0, bytes, pos, readcount);
												pos+=readcount;
											}
										}
										session.putDone(name);
									} else {
										connected.getOutputStream().write(datatoolarge);
									}
								} else {
									connected.getOutputStream().write(session.canPut(name, cl) ? updated : datatoolarge);
								}
							} else {
								connected.getOutputStream().write(notoken);
							}
						} else {
							connected.getOutputStream().write(nolength);
						}
					} else {
						connected.getOutputStream().write(novalidtoken);
					}
				} else {
					connected.getOutputStream().write(headertoolarge);
				}
			} else {
				connected.getOutputStream().write(e == -1 ? notfound : web[e]);
			}
		} catch (IOException e) {
		} finally {
			try {
				connected.close();
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
