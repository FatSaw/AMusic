package me.bomb.amusic.resourceserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.UUID;

final class ResourceSender extends Thread {
	
	private static final byte[] responsepart0 = "HTTP/1.1 200 OK\r\nServer: AMusic server\r\nContent-Type: application/zip\r\nContent-Length: ".getBytes(), responsepart1 = "\r\nConnection: close\r\n\r\n".getBytes();
	
	private final Socket client;

	protected ResourceSender(Socket client) {
		this.client = client;
		start();
	}

	public void run() {
		UUID token = null;
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			StringTokenizer tokenizer = new StringTokenizer(in.readLine());
			String httpMethod = tokenizer.nextToken();
			String httpQueryString = tokenizer.nextToken();
			int uuidend;
			if (!httpMethod.equals("GET") || (uuidend = httpQueryString.lastIndexOf(".zip")) < 36) {
				client.close();
				return;
			}
			token = UUID.fromString(httpQueryString.substring(1, uuidend));
			byte i = 0;
			while (CachedResource.waitAcception(token) && 0 != ++i) {
				try {
					sleep(100);
				} catch (InterruptedException e) {
				}
			}
			byte[] ares;
			if (CachedResource.waitAcception(token) || (ares = CachedResource.get(token)).length == 0) {
				client.close();
				return;
			}
			OutputStream out = this.client.getOutputStream();
			out.write(responsepart0);
			out.write(Integer.toString(ares.length).getBytes());
			out.write(responsepart1);
			out.write(ares);
			client.close();
		} catch (NoSuchElementException | IndexOutOfBoundsException | IllegalArgumentException | IOException e) {
			if(client.isClosed()) return;
			try {
				client.close();
			} catch (IOException e1) {
			}
		}
	}
}