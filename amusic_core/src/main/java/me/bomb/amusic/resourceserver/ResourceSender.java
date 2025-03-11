package me.bomb.amusic.resourceserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.UUID;

final class ResourceSender extends Thread {
	
	private static final byte[] responsepart0 = "HTTP/1.1 200 OK\r\nServer: AMusic server\r\nContent-Type: application/zip\r\nContent-Length: ".getBytes(), responsepart1 = "\r\nConnection: close\r\n\r\n".getBytes();
	
	private final Socket client;
	private final ResourceManager resourcemanager;

	protected ResourceSender(Socket client, ResourceManager resourcemanager) {
		this.client = client;
		this.resourcemanager = resourcemanager;
		start();
	}

	public void run() {
		UUID token = null;
		try {
			byte[] buf = new byte[512];
			InputStream in = client.getInputStream();
			if(in.read(buf) > 53 && buf[0] == 'G' && buf[1] == 'E' && buf[2] == 'T' && buf[3] == ' ' && buf[4] == '/' && buf[41] == '.' && buf[42] == 'z' && buf[43] == 'i' && buf[44] == 'p' && buf[45] == ' ' && buf[46] == 'H' && buf[47] == 'T' && buf[48] == 'T' && buf[49] == 'P' && buf[50] == '/' && buf[51] == '1' && buf[52] == '.' && buf[53] == '1') {
				buf = new byte[]{buf[5], buf[6], buf[7], buf[8], buf[9], buf[10], buf[11], buf[12], buf[13], buf[14], buf[15], buf[16], buf[17], buf[18], buf[19], buf[20], buf[21], buf[22], buf[23], buf[24], buf[25], buf[26], buf[27], buf[28], buf[29], buf[30], buf[31], buf[32], buf[33], buf[34], buf[35], buf[36], buf[37], buf[38], buf[39], buf[40]};
				token = UUID.fromString(new String(buf, StandardCharsets.US_ASCII));
				client.shutdownInput();
			} else {
				client.close();
				return;
			}
			byte i = 0;
			while (resourcemanager.waitAcception(token) && 0 != ++i) {
				try {
					sleep(100);
				} catch (InterruptedException e) {
				}
			}
			if (resourcemanager.waitAcception(token) || (buf = resourcemanager.get(token)) == null) {
				client.close();
				return;
			}
			OutputStream out = this.client.getOutputStream();
			out.write(responsepart0);
			out.write(Integer.toString(buf.length).getBytes());
			out.write(responsepart1);
			out.write(buf);
			out.close();
		} catch (NoSuchElementException | IndexOutOfBoundsException | IllegalArgumentException | IOException e) {
			if(client.isClosed()) return;
			try {
				client.close();
			} catch (IOException e1) {
			}
		}
	}
}
