package me.bomb.amusic.resourceserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.Executor;

import me.bomb.amusic.http.ServerWorker;
import me.bomb.amusic.packedinfo.DataEntry;

final class ResourceSender implements ServerWorker {
	
	private static final byte[] responsepart0 = "HTTP/1.1 200 OK\r\nServer: AMusic server\r\nContent-Type: application/zip\r\nContent-Length: ".getBytes(), responsepart1 = "\r\nConnection: close\r\n\r\n".getBytes();
	
	private final ResourceManager resourcemanager;
	private final Executor checkerexecutor, senderexecutor;

	protected ResourceSender(ResourceManager resourcemanager, Executor checkerexecutor, Executor senderexecutor) {
		this.resourcemanager = resourcemanager;
		this.checkerexecutor = checkerexecutor;
		this.senderexecutor = senderexecutor;
	}

	@Override
	public void processConnection(Socket connected) throws IOException {
		Runnable r = new Runnable() {
			public void run() {
				UUID token = null;
				try {
					{
						byte[] buf = new byte[512];
						InputStream in = connected.getInputStream();
						if(in.read(buf, 0, buf.length) > 53 && buf[0] == 'G' && buf[1] == 'E' && buf[2] == 'T' && buf[3] == ' ' && buf[4] == '/' && buf[41] == '.' && buf[42] == 'z' && buf[43] == 'i' && buf[44] == 'p' && buf[45] == ' ' && buf[46] == 'H' && buf[47] == 'T' && buf[48] == 'T' && buf[49] == 'P' && buf[50] == '/' && buf[51] == '1' && buf[52] == '.' && buf[53] == '1') {
							token = UUID.fromString(new String(buf, 5, 36, StandardCharsets.US_ASCII));
							connected.shutdownInput();
						} else {
							connected.close();
							return;
						}
					}
					byte i = 0;
					while (resourcemanager.waitAcception(token) && 0 != ++i) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
						}
					}
					DataEntry entry;
					byte[] buf;
					if (resourcemanager.waitAcception(token) || (entry = resourcemanager.get(token)) == null || (buf = entry.getPack()) == null) {
						connected.close();
						return;
					}
					OutputStream out = connected.getOutputStream();
					senderexecutor.execute(new Runnable() {
						@Override
						public void run() {
							try {
								out.write(responsepart0);
								out.write(Integer.toString(buf.length).getBytes());
								out.write(responsepart1);
								out.write(buf);
								out.close();
							} catch (IOException e1) {
								try {
									out.close();
								} catch (IOException e2) {
								}
							}
						}
					});
				} catch (NoSuchElementException | IndexOutOfBoundsException | IllegalArgumentException | IOException e) {
					if(connected.isClosed()) return;
					try {
						connected.close();
					} catch (IOException e1) {
					}
				}
			}
		};
		this.checkerexecutor.execute(r);
	}
}
