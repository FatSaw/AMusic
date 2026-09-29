package me.bomb.amusic.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;

public final class SimpleSocketFactory extends SocketFactory {

	@Override
	public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
		return new Socket(host, port);
	}

	@Override
	public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
		return new Socket(host, port, localHost, localPort);
	}

	@Override
	public Socket createSocket(InetAddress host, int port) throws IOException {
		return new Socket(host, port);
	}

	@Override
	public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
		return new Socket(address, port, localAddress, localPort);
	}

}
